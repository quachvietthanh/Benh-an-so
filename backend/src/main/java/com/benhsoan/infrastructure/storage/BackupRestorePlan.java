package com.benhsoan.infrastructure.storage;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Infrastructure-level contract for one logical backup scope. It deliberately
 * models database table relationships without leaking them into application ports.
 */
public final class BackupRestorePlan {

    private final List<String> snapshotTables;
    private final Set<String> allowedTables;
    private final Set<String> approvedExternalParentTables;
    private final List<ForeignKeyDependency> dependencies;
    private final Set<DeferredForeignKey> deferredForeignKeys;
    private final Map<String, Set<String>> dependencyGraph;
    private final Map<String, Set<String>> restoreDependencyGraph;

    public BackupRestorePlan(
            List<String> snapshotTables,
            Set<String> approvedExternalParentTables,
            List<ForeignKeyDependency> dependencies,
            Set<DeferredForeignKey> deferredForeignKeys
    ) {
        this.snapshotTables = List.copyOf(snapshotTables);
        this.allowedTables = Set.copyOf(snapshotTables);
        this.approvedExternalParentTables = Set.copyOf(approvedExternalParentTables);
        this.dependencies = List.copyOf(dependencies);
        this.deferredForeignKeys = Set.copyOf(deferredForeignKeys);

        validateTablesAreUnique();
        validateDependencies();
        validateDeferredForeignKeys();

        this.dependencyGraph = buildDependencyGraph(false);
        this.restoreDependencyGraph = buildDependencyGraph(true);
    }

    public List<String> snapshotTables() {
        return snapshotTables;
    }

    public Set<String> allowedTables() {
        return allowedTables;
    }

    public Set<String> approvedExternalParentTables() {
        return approvedExternalParentTables;
    }

    public List<ForeignKeyDependency> dependencies() {
        return dependencies;
    }

    public Set<DeferredForeignKey> deferredForeignKeys() {
        return deferredForeignKeys;
    }

    /**
     * Contains every in-scope foreign key, including cyclic edges.
     */
    public Map<String, Set<String>> dependencyGraph() {
        return dependencyGraph;
    }

    /**
     * Excludes deferred edges so a restore planner can topologically order inserts.
     */
    public Map<String, Set<String>> restoreDependencyGraph() {
        return restoreDependencyGraph;
    }

    private void validateTablesAreUnique() {
        if (snapshotTables.isEmpty()) {
            throw new IllegalArgumentException("Backup snapshot tables must not be empty.");
        }
        if (snapshotTables.size() != allowedTables.size()) {
            throw new IllegalArgumentException("Backup snapshot tables must be unique.");
        }
    }

    private void validateDependencies() {
        Set<ForeignKeyDependency> uniqueDependencies = new LinkedHashSet<>(dependencies);
        if (uniqueDependencies.size() != dependencies.size()) {
            throw new IllegalArgumentException("Backup foreign-key dependencies must be unique.");
        }

        for (ForeignKeyDependency dependency : dependencies) {
            requireAllowedTable(dependency.childTable());
            requireAllowedTable(dependency.parentTable());
        }
    }

    private void validateDeferredForeignKeys() {
        for (DeferredForeignKey deferredForeignKey : deferredForeignKeys) {
            requireAllowedTable(deferredForeignKey.childTable());
            boolean dependencyExists = dependencies.stream().anyMatch(dependency ->
                    dependency.childTable().equals(deferredForeignKey.childTable())
                            && dependency.childColumn().equals(deferredForeignKey.childColumn()));
            if (!dependencyExists) {
                throw new IllegalArgumentException(
                        "Deferred foreign key must exist in the dependency graph: " + deferredForeignKey);
            }
        }
    }

    private void requireAllowedTable(String tableName) {
        if (!allowedTables.contains(tableName)) {
            throw new IllegalArgumentException("Table is outside the backup scope: " + tableName);
        }
    }

    private Map<String, Set<String>> buildDependencyGraph(boolean excludeDeferredForeignKeys) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        for (String table : snapshotTables) {
            graph.put(table, new LinkedHashSet<>());
        }
        for (ForeignKeyDependency dependency : dependencies) {
            DeferredForeignKey foreignKey = new DeferredForeignKey(
                    dependency.childTable(), dependency.childColumn());
            if (!excludeDeferredForeignKeys || !deferredForeignKeys.contains(foreignKey)) {
                graph.get(dependency.childTable()).add(dependency.parentTable());
            }
        }

        Map<String, Set<String>> immutableGraph = new LinkedHashMap<>();
        graph.forEach((table, parents) -> immutableGraph.put(table, Set.copyOf(parents)));
        return Map.copyOf(immutableGraph);
    }

    public record ForeignKeyDependency(String childTable, String childColumn, String parentTable) {

        public ForeignKeyDependency {
            requireIdentifier(childTable, "Child table");
            requireIdentifier(childColumn, "Child column");
            requireIdentifier(parentTable, "Parent table");
        }
    }

    public record DeferredForeignKey(String childTable, String childColumn) {

        public DeferredForeignKey {
            requireIdentifier(childTable, "Child table");
            requireIdentifier(childColumn, "Child column");
        }
    }

    private static void requireIdentifier(String value, String label) {
        if (value == null || !value.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException(label + " must be a lowercase SQL identifier.");
        }
    }
}
