package com.benhsoan.infrastructure.storage;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.benhsoan.domain.backup.exception.BackupExecutionException;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON snapshot implementation of {@link DatabaseBackupStoragePort}.
 *
 * Exports the configured operational tables into a self-describing JSON dump
 * (column names + JDBC types + string-encoded cell values) and restores them by
 * deleting and re-inserting rows in reverse FK-safe order.
 */
public class JsonDatabaseBackupStorageAdapter implements DatabaseBackupStoragePort {

    private static final String EXTENSION = ".json";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final List<String> backupTables;
    private final Path backupDirectory;

    public JsonDatabaseBackupStorageAdapter(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            List<String> backupTables,
            Path backupDirectory
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.backupTables = List.copyOf(backupTables);
        this.backupDirectory = backupDirectory;
    }

    @Override
    public BackupSnapshot exportSnapshot(String backupCode) {
        List<TableSnapshot> tables = backupTables.stream()
                .map(this::dumpTable)
                .toList();

        byte[] content = writeJson(tables);
        String fileName = backupCode + EXTENSION;
        persist(fileName, content);

        return new BackupSnapshot(fileName, content);
    }

    @Override
    public BackupSnapshot loadSnapshot(String fileName) {
        return new BackupSnapshot(fileName, readFile(fileName));
    }

    @Override
    public void restoreSnapshot(String fileName) {
        List<TableSnapshot> tables = readJson(readFile(fileName));
        List<TableSnapshot> reverseOrder = new ArrayList<>(tables);
        Collections.reverse(reverseOrder);
        for (TableSnapshot table : reverseOrder) {
            restoreTable(table);
        }
    }

    private TableSnapshot dumpTable(String tableName) {
        List<ColumnMeta> columns = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        jdbcTemplate.query("SELECT * FROM " + tableName, (RowCallbackHandler) rs -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            if (columns.isEmpty()) {
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(new ColumnMeta(metaData.getColumnName(i), metaData.getColumnType(i)));
                }
            }

            List<String> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(toJsonSafe(rs.getObject(i)));
            }
            rows.add(row);
        });

        return new TableSnapshot(tableName, columns, rows);
    }
    private void restoreTable(TableSnapshot table) {
        jdbcTemplate.update("DELETE FROM " + table.name());
        if (table.rows().isEmpty()) {
            return;
        }

        String columnList = table.columns().stream()
                .map(ColumnMeta::name)
                .collect(Collectors.joining(", "));
        String placeholders = table.columns().stream()
                .map(column -> "?")
                .collect(Collectors.joining(", "));
        String insertSql = "INSERT INTO " + table.name() + " (" + columnList + ") VALUES (" + placeholders + ")";

        for (List<String> row : table.rows()) {
            jdbcTemplate.update(insertSql, ps -> bindRow(ps, table.columns(), row));
        }
    }

    private void bindRow(PreparedStatement ps, List<ColumnMeta> columns, List<String> row) throws SQLException {
        for (int i = 0; i < columns.size(); i++) {
            setValue(ps, i + 1, columns.get(i).type(), row.get(i));
        }
    }

    private void setValue(PreparedStatement ps, int index, int jdbcType, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, jdbcType);
            return;
        }
        switch (jdbcType) {
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB ->
                    ps.setBytes(index, HexFormat.of().parseHex(value));
            case Types.DATE -> ps.setDate(index, java.sql.Date.valueOf(value));
            case Types.TIME -> ps.setTime(index, java.sql.Time.valueOf(value));
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> ps.setTimestamp(index, toTimestamp(value));
            case Types.BOOLEAN, Types.BIT -> ps.setBoolean(index, isTrue(value));
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> ps.setInt(index, Integer.parseInt(value));
            case Types.BIGINT -> ps.setLong(index, Long.parseLong(value));
            case Types.DECIMAL, Types.NUMERIC -> ps.setBigDecimal(index, new BigDecimal(value));
            case Types.FLOAT, Types.REAL -> ps.setFloat(index, Float.parseFloat(value));
            case Types.DOUBLE -> ps.setDouble(index, Double.parseDouble(value));
            default -> ps.setString(index, value);
        }
    }

    private String toJsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return HexFormat.of().formatHex(bytes);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof java.time.OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant().toString();
        }
        if (value instanceof java.time.Instant instant) {
            return instant.toString();
        }
        return value.toString();
    }

    private Timestamp toTimestamp(String value) {
        if (value.endsWith("Z")) {
            return Timestamp.from(java.time.Instant.parse(value));
        }
        return Timestamp.valueOf(value.replace('T', ' '));
    }

    private boolean isTrue(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private byte[] writeJson(List<TableSnapshot> tables) {
        try {
            return objectMapper.writeValueAsBytes(tables);
        } catch (JsonProcessingException ex) {
            throw new BackupExecutionException("Failed to serialize backup snapshot: " + ex.getMessage());
        }
    }

    private List<TableSnapshot> readJson(byte[] content) {
        try {
            return objectMapper.readValue(content, new TypeReference<List<TableSnapshot>>() {
            });
        } catch (IOException ex) {
            throw new BackupExecutionException("Failed to deserialize backup snapshot: " + ex.getMessage());
        }
    }

    private void persist(String fileName, byte[] content) {
        try {
            Files.createDirectories(backupDirectory);
            Files.write(
                    backupDirectory.resolve(fileName),
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException ex) {
            throw new BackupExecutionException("Failed to store backup file: " + ex.getMessage());
        }
    }

    private byte[] readFile(String fileName) {
        try {
            Path path = backupDirectory.resolve(fileName);
            if (!Files.exists(path)) {
                throw new BackupExecutionException("Backup file not found: " + fileName);
            }
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new BackupExecutionException("Failed to read backup file: " + ex.getMessage());
        }
    }

    public record TableSnapshot(String name, List<ColumnMeta> columns, List<List<String>> rows) {
    }

    public record ColumnMeta(String name, int type) {
    }
}
