-- An IN_PROGRESS backup has not produced a file yet. The file name is assigned
-- only after the snapshot is written successfully.
ALTER TABLE backup_records
    MODIFY file_name VARCHAR(255) NULL;
