ALTER TABLE patients ADD COLUMN emergency_relationship VARCHAR(50) NULL;

UPDATE patients
SET emergency_relationship = 'Người thân'
WHERE emergency_contact IS NOT NULL AND emergency_relationship IS NULL;
