CREATE TABLE prescription_code_sequences (
    code_prefix VARCHAR(10) NOT NULL,
    last_value BIGINT NOT NULL,

    CONSTRAINT pk_prescription_code_sequences PRIMARY KEY (code_prefix),
    CONSTRAINT chk_prescription_code_sequences_last_value CHECK (last_value > 0)
);
