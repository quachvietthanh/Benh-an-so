CREATE TABLE rooms (
    id BINARY(16) NOT NULL,
    room_code VARCHAR(30) NOT NULL,
    room_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT uk_rooms_code UNIQUE (room_code)
);

CREATE TABLE doctor_room_assignments (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    room_id BINARY(16) NOT NULL,
    assigned_by BINARY(16) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_doctor_room_assignments PRIMARY KEY (id),
    CONSTRAINT uk_doctor_room_assignments_doctor UNIQUE (doctor_id),
    CONSTRAINT uk_doctor_room_assignments_room UNIQUE (room_id),
    CONSTRAINT fk_doctor_room_assignments_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_doctor_room_assignments_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_doctor_room_assignments_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id)
);

CREATE TABLE medical_queues (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    room_id BINARY(16) NOT NULL,
    queue_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_medical_queues PRIMARY KEY (id),
    CONSTRAINT uk_medical_queues_doctor_date UNIQUE (doctor_id, queue_date),
    CONSTRAINT fk_medical_queues_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_medical_queues_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT ck_medical_queues_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE TABLE queue_items (
    id BINARY(16) NOT NULL,
    medical_queue_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    appointment_id BINARY(16) NULL,
    visit_id BINARY(16) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    queue_number INT NOT NULL,
    queue_date DATE NOT NULL,
    checked_in_at TIMESTAMP NOT NULL,
    called_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    cancel_reason VARCHAR(500) NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_queue_items PRIMARY KEY (id),
    CONSTRAINT uk_queue_items_appointment UNIQUE (appointment_id),
    CONSTRAINT uk_queue_items_visit UNIQUE (visit_id),
    CONSTRAINT uk_queue_items_patient_date UNIQUE (patient_id, queue_date),
    CONSTRAINT uk_queue_items_queue_number UNIQUE (medical_queue_id, queue_number),
    CONSTRAINT fk_queue_items_queue FOREIGN KEY (medical_queue_id) REFERENCES medical_queues(id),
    CONSTRAINT fk_queue_items_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_queue_items_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_queue_items_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_queue_items_source_type CHECK (source_type IN ('APPOINTMENT', 'WALK_IN')),
    CONSTRAINT ck_queue_items_status CHECK (status IN ('WAITING', 'IN_PROGRESS', 'WAITING_FOR_RESULT', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_rooms_active ON rooms(active);
CREATE INDEX idx_medical_queues_room_date ON medical_queues(room_id, queue_date);
CREATE INDEX idx_queue_items_queue_status_number ON queue_items(medical_queue_id, status, queue_number);
CREATE INDEX idx_queue_items_patient ON queue_items(patient_id);
CREATE INDEX idx_queue_items_visit ON queue_items(visit_id);

INSERT INTO rooms (id, room_code, room_name, active, created_at, updated_at) VALUES
(UUID_TO_BIN('90000000-0000-0000-0000-000000000001'), 'P101', 'Phong kham 101', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('90000000-0000-0000-0000-000000000002'), 'P102', 'Phong kham 102', TRUE, CURRENT_TIMESTAMP, NULL);

INSERT INTO doctor_room_assignments (id, doctor_id, room_id, assigned_by, assigned_at) VALUES
(UUID_TO_BIN('91000000-0000-0000-0000-000000000001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), UUID_TO_BIN('90000000-0000-0000-0000-000000000001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'), CURRENT_TIMESTAMP),
(UUID_TO_BIN('91000000-0000-0000-0000-000000000002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), UUID_TO_BIN('90000000-0000-0000-0000-000000000002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'), CURRENT_TIMESTAMP);
