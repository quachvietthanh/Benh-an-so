package com.benhsoan.infrastructure.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.benhsoan.domain.backup.exception.BackupExecutionException;

final class ForeignKeyRestorePlanner {

    private final JdbcTemplate jdbcTemplate;
    private final BackupRestorePlan backupRestorePlan;

    ForeignKeyRestorePlanner(JdbcTemplate jdbcTemplate, BackupRestorePlan backupRestorePlan) {
        this.jdbcTemplate = jdbcTemplate;
        this.backupRestorePlan = backupRestorePlan;
    }

    RestorePlan createPlan(List<String> snapshotTables) {
        List<String> tables = snapshotTables.stream()
                .map(ForeignKeyRestorePlanner::normalize)
                .toList();
        validateSnapshotTables(tables);

        try {
            return jdbcTemplate.execute((ConnectionCallback<RestorePlan>) connection -> createPlan(connection, tables));
        } catch (BackupExecutionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BackupExecutionException("Failed to inspect database foreign keys: " + ex.getMessage());
        }
    }

    private RestorePlan createPlan(Connection connection, List<String> tables) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        Set<String> snapshotTableSet = Set.copyOf(tables);
        Set<ForeignKeyEdge> foreignKeys = readForeignKeys(metadata, connection.getCatalog(), tables);

        validateForeignKeyScope(foreignKeys, snapshotTableSet);
        validateSchemaContract(foreignKeys, snapshotTableSet);

        List<String> parentFirst = topologicalOrder(tables, foreignKeys, snapshotTableSet);
        List<String> childFirst = new ArrayList<>(parentFirst);
        java.util.Collections.reverse(childFirst);

        List<DeferredUpdate> deferredUpdates = deferredUpdates(metadata, connection.getCatalog(), tables);
        return new RestorePlan(parentFirst, childFirst, deferredUpdates);
    }

    private Set<ForeignKeyEdge> readForeignKeys(DatabaseMetaData metadata, String catalog, List<String> tables)
            throws SQLException {
        Set<ForeignKeyEdge> foreignKeys = new LinkedHashSet<>();
        for (String table : tables) {
            try (ResultSet keys = metadata.getImportedKeys(catalog, null, table)) {
                while (keys.next()) {
                    foreignKeys.add(new ForeignKeyEdge(
                            normalize(keys.getString("FKTABLE_NAME")),
                            normalize(keys.getString("FKCOLUMN_NAME")),
                            normalize(keys.getString("PKTABLE_NAME"))
                    ));
                }
            }
        }
        return foreignKeys;
    }

    private void validateSnapshotTables(List<String> tables) {
        if (tables.isEmpty()) {
            throw new BackupExecutionException("Backup snapshot does not contain any tables.");
        }
        if (tables.size() != Set.copyOf(tables).size()) {
            throw new BackupExecutionException("Backup snapshot contains duplicate tables.");
        }
        for (String table : tables) {
            if (!backupRestorePlan.allowedTables().contains(table)) {
                throw new BackupExecutionException("Backup snapshot contains a table outside the FULL scope: " + table);
            }
        }
    }

    private void validateForeignKeyScope(Set<ForeignKeyEdge> foreignKeys, Set<String> snapshotTables) {
        for (ForeignKeyEdge foreignKey : foreignKeys) {
            if (snapshotTables.contains(foreignKey.parentTable())) {
                continue;
            }
            if (backupRestorePlan.approvedExternalParentTables().contains(foreignKey.parentTable())) {
                continue;
            }
            throw new BackupExecutionException(
                    "Backup snapshot omits foreign-key parent table " + foreignKey.parentTable()
                            + " required by " + foreignKey.childTable() + ".");
        }
    }

    private void validateSchemaContract(Set<ForeignKeyEdge> foreignKeys, Set<String> snapshotTables) {
        Set<ForeignKeyEdge> expected = backupRestorePlan.dependencies().stream()
                .filter(dependency -> snapshotTables.contains(dependency.childTable())
                        && snapshotTables.contains(dependency.parentTable()))
                .map(dependency -> new ForeignKeyEdge(
                        dependency.childTable(), dependency.childColumn(), dependency.parentTable()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        Set<ForeignKeyEdge> actual = foreignKeys.stream()
                .filter(foreignKey -> snapshotTables.contains(foreignKey.parentTable()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        if (!actual.equals(expected)) {
            throw new BackupExecutionException(
                    "Database foreign keys do not match the configured FULL backup scope.");
        }
    }

    private List<String> topologicalOrder(
            List<String> tables,
            Set<ForeignKeyEdge> foreignKeys,
            Set<String> snapshotTables
    ) {
        Map<String, Set<String>> parents = new LinkedHashMap<>();
        for (String table : tables) {
            parents.put(table, new LinkedHashSet<>());
        }
        for (ForeignKeyEdge foreignKey : foreignKeys) {
            if (snapshotTables.contains(foreignKey.parentTable()) && !isDeferred(foreignKey)) {
                parents.get(foreignKey.childTable()).add(foreignKey.parentTable());
            }
        }

        List<String> parentFirst = new ArrayList<>();
        Set<String> restored = new LinkedHashSet<>();
        while (parentFirst.size() < tables.size()) {
            List<String> ready = tables.stream()
                    .filter(table -> !restored.contains(table))
                    .filter(table -> restored.containsAll(parents.get(table)))
                    .toList();
            if (ready.isEmpty()) {
                throw new BackupExecutionException("Backup restore graph still contains an unresolved foreign-key cycle.");
            }
            parentFirst.addAll(ready);
            restored.addAll(ready);
        }
        return List.copyOf(parentFirst);
    }

    private boolean isDeferred(ForeignKeyEdge foreignKey) {
        return backupRestorePlan.deferredForeignKeys().contains(
                new BackupRestorePlan.DeferredForeignKey(foreignKey.childTable(), foreignKey.childColumn()));
    }

    private List<DeferredUpdate> deferredUpdates(DatabaseMetaData metadata, String catalog, List<String> tables)
            throws SQLException {
        Set<String> snapshotTables = Set.copyOf(tables);
        List<DeferredUpdate> deferredUpdates = new ArrayList<>();
        for (BackupRestorePlan.DeferredForeignKey foreignKey : backupRestorePlan.deferredForeignKeys()) {
            if (!snapshotTables.contains(foreignKey.childTable())) {
                continue;
            }
            deferredUpdates.add(new DeferredUpdate(
                    foreignKey.childTable(),
                    foreignKey.childColumn(),
                    primaryKey(metadata, catalog, foreignKey.childTable())
            ));
        }
        return List.copyOf(deferredUpdates);
    }

    private String primaryKey(DatabaseMetaData metadata, String catalog, String table) throws SQLException {
        List<PrimaryKeyColumn> primaryKeys = new ArrayList<>();
        try (ResultSet keys = metadata.getPrimaryKeys(catalog, null, table)) {
            while (keys.next()) {
                primaryKeys.add(new PrimaryKeyColumn(
                        keys.getShort("KEY_SEQ"),
                        normalize(keys.getString("COLUMN_NAME"))
                ));
            }
        }
        primaryKeys.sort(Comparator.comparingInt(PrimaryKeyColumn::sequence));
        if (primaryKeys.size() != 1) {
            throw new BackupExecutionException(
                    "Deferred foreign-key restore requires a single-column primary key on " + table + ".");
        }
        return primaryKeys.getFirst().name();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    record RestorePlan(
            List<String> parentFirstTables,
            List<String> childFirstTables,
            List<DeferredUpdate> deferredUpdates
    ) {
    }

    record DeferredUpdate(String tableName, String columnName, String primaryKeyColumn) {
    }

    private record ForeignKeyEdge(String childTable, String childColumn, String parentTable) {
    }

    private record PrimaryKeyColumn(short sequence, String name) {
    }
}
