-- =====================================================
-- V19 - Seed medicine, drug interaction rule and prescription data
-- Requires V3 users and V14 medical record seed data.
-- =====================================================

-- ===========================
-- Medicine catalog
-- ===========================

INSERT INTO medicines (
    id, medicine_code, medicine_name, active_ingredient, strength,
    dosage_form, unit, default_route, active, stock_quantity, min_stock_threshold, created_at, updated_at
) VALUES
(UUID_TO_BIN('16000000-0000-0000-0000-000000000001'), 'MED-PARA-500', 'Paracetamol 500 mg', 'Paracetamol', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 120, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), 'MED-IBU-400', 'Ibuprofen 400 mg', 'Ibuprofen', '400 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 80, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000003'), 'MED-AMOX-500', 'Amoxicillin 500 mg', 'Amoxicillin', '500 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, 0, 100, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000004'), 'MED-AMCL-625', 'Amoxicillin Clavulanate 625 mg', 'Amoxicillin + Clavulanic acid', '625 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 60, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000005'), 'MED-AZI-500', 'Azithromycin 500 mg', 'Azithromycin', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 50, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000006'), 'MED-CEFU-500', 'Cefuroxime 500 mg', 'Cefuroxime', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 50, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000007'), 'MED-OMEP-20', 'Omeprazole 20 mg', 'Omeprazole', '20 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, 0, 70, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000008'), 'MED-ESOM-40', 'Esomeprazole 40 mg', 'Esomeprazole', '40 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000009'), 'MED-CETI-10', 'Cetirizine 10 mg', 'Cetirizine', '10 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 60, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000010'), 'MED-LORA-10', 'Loratadine 10 mg', 'Loratadine', '10 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 60, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000011'), 'MED-DEXTRO-15', 'Dextromethorphan 15 mg/5 ml', 'Dextromethorphan', '15 mg/5 ml', 'SYRUP', 'chai', 'ORAL', TRUE, 0, 20, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000012'), 'MED-AMBRO-30', 'Ambroxol 30 mg', 'Ambroxol', '30 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 50, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000013'), 'MED-ACETY-200', 'Acetylcysteine 200 mg', 'Acetylcysteine', '200 mg', 'POWDER', 'goi', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000014'), 'MED-SALBU-2', 'Salbutamol 2 mg', 'Salbutamol', '2 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000015'), 'MED-VENTO', 'Salbutamol inhaler', 'Salbutamol', '100 mcg/lieu', 'INHALER', 'binh', 'INHALATION', TRUE, 0, 15, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000016'), 'MED-METFO-500', 'Metformin 500 mg', 'Metformin', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 120, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000017'), 'MED-GLIC-30', 'Gliclazide MR 30 mg', 'Gliclazide', '30 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 80, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000018'), 'MED-ATOR-20', 'Atorvastatin 20 mg', 'Atorvastatin', '20 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 60, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000019'), 'MED-AMLO-5', 'Amlodipine 5 mg', 'Amlodipine', '5 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 80, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000020'), 'MED-LOSAR-50', 'Losartan 50 mg', 'Losartan', '50 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 80, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000021'), 'MED-DICLO-50', 'Diclofenac 50 mg', 'Diclofenac', '50 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000022'), 'MED-MELOX-7', 'Meloxicam 7.5 mg', 'Meloxicam', '7.5 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000023'), 'MED-PRED-5', 'Prednisolone 5 mg', 'Prednisolone', '5 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000024'), 'MED-HYDRO-1', 'Hydrocortisone cream 1%', 'Hydrocortisone', '1%', 'CREAM', 'tuyp', 'TOPICAL', TRUE, 0, 15, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000025'), 'MED-ORS', 'Oresol', 'Oral rehydration salts', 'goi 27.9 g', 'POWDER', 'goi', 'ORAL', TRUE, 0, 50, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000026'), 'MED-LOPE-2', 'Loperamide 2 mg', 'Loperamide', '2 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000027'), 'MED-BISAC-5', 'Bisacodyl 5 mg', 'Bisacodyl', '5 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000028'), 'MED-LEVO-500', 'Levofloxacin 500 mg', 'Levofloxacin', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 35, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000029'), 'MED-WARF-2', 'Warfarin 2 mg', 'Warfarin', '2 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 25, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000030'), 'MED-ASP-81', 'Aspirin 81 mg', 'Aspirin', '81 mg', 'TABLET', 'vien', 'ORAL', FALSE, 0, 40, '2026-08-01 01:00:00', '2026-08-15 01:00:00'),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000031'), 'MED-CLARI-500', 'Clarithromycin 500 mg', 'Clarithromycin', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000032'), 'MED-SIMVA-20', 'Simvastatin 20 mg', 'Simvastatin', '20 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 60, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000033'), 'MED-GEMFI-600', 'Gemfibrozil 600 mg', 'Gemfibrozil', '600 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000034'), 'MED-AMIOD-200', 'Amiodarone 200 mg', 'Amiodarone', '200 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000035'), 'MED-ISMN-60', 'Isosorbide Mononitrate 60 mg', 'Isosorbide Mononitrate', '60 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000036'), 'MED-SILD-50', 'Sildenafil 50 mg', 'Sildenafil', '50 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000037'), 'MED-MTX-2_5', 'Methotrexate 2.5 mg', 'Methotrexate', '2.5 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 20, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000038'), 'MED-TRIM-160', 'Trimethoprim 160 mg', 'Trimethoprim', '160 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000039'), 'MED-DIGO-0_25', 'Digoxin 0.25 mg', 'Digoxin', '0.25 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 20, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000040'), 'MED-FURO-40', 'Furosemide 40 mg', 'Furosemide', '40 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000041'), 'MED-CIPRO-500', 'Ciprofloxacin 500 mg', 'Ciprofloxacin', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000042'), 'MED-TIZA-2', 'Tizanidine 2 mg', 'Tizanidine', '2 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 20, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000043'), 'MED-THEO-100', 'Theophylline 100 mg', 'Theophylline', '100 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 20, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000044'), 'MED-LITH-300', 'Lithium carbonate 300 mg', 'Lithium', '300 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, 0, 20, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000045'), 'MED-LISI-10', 'Lisinopril 10 mg', 'Lisinopril', '10 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000046'), 'MED-KCL-600', 'Potassium Chloride 600 mg', 'Potassium Chloride', '600 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 20, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000047'), 'MED-FLUOX-20', 'Fluoxetine 20 mg', 'Fluoxetine', '20 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000048'), 'MED-TRAMA-50', 'Tramadol 50 mg', 'Tramadol', '50 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, 0, 30, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000049'), 'MED-CLOPI-75', 'Clopidogrel 75 mg', 'Clopidogrel', '75 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000050'), 'MED-DIPHEN-25', 'Diphenhydramine 25 mg', 'Diphenhydramine', '25 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, 0, 20, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000051'), 'MED-PANTO-40', 'Pantoprazole 40 mg', 'Pantoprazole', '40 mg', 'TABLET', 'vien', 'ORAL', TRUE, 0, 40, '2026-08-01 01:00:00', NULL);

-- ===========================
-- Drug interaction rules
-- ===========================

INSERT INTO drug_interaction_rules (
    id, active_ingredient_a, active_ingredient_b, severity_level, description,
    clinical_recommendation, is_active, created_at, updated_at
) VALUES
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000001'), 'Aspirin', 'Warfarin', 'SEVERE', 'Aspirin increases bleeding risk when used with Warfarin.', 'Avoid the combination when possible; if both are required, monitor INR and bleeding closely.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000002'), 'Ibuprofen', 'Warfarin', 'SEVERE', 'Ibuprofen increases bleeding risk when used with Warfarin.', 'Avoid NSAIDs when possible; consider Paracetamol and monitor INR closely.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000003'), 'Clarithromycin', 'Simvastatin', 'CONTRAINDICATED', 'Clarithromycin can sharply increase Simvastatin exposure and rhabdomyolysis risk.', 'Do not combine; choose another antibiotic or temporarily stop Simvastatin.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000004'), 'Gemfibrozil', 'Simvastatin', 'CONTRAINDICATED', 'Gemfibrozil markedly increases Simvastatin toxicity risk.', 'Do not combine; consider another lipid-lowering strategy.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000005'), 'Amiodarone', 'Simvastatin', 'SEVERE', 'Amiodarone can increase Simvastatin exposure and muscle toxicity.', 'Limit Simvastatin dose or switch to a lower-interaction statin.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000006'), 'Isosorbide Mononitrate', 'Sildenafil', 'CONTRAINDICATED', 'Combining Sildenafil with nitrates can cause profound hypotension.', 'Contraindicated; avoid Sildenafil within 24 to 48 hours of nitrate use.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000007'), 'Methotrexate', 'Trimethoprim', 'CONTRAINDICATED', 'Trimethoprim increases bone marrow suppression risk with Methotrexate.', 'Do not combine; use another antibiotic and monitor blood counts.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000008'), 'Digoxin', 'Furosemide', 'MODERATE', 'Furosemide-induced hypokalemia can increase Digoxin toxicity.', 'Monitor potassium and Digoxin levels and replace potassium if needed.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000009'), 'Ciprofloxacin', 'Tizanidine', 'CONTRAINDICATED', 'Ciprofloxacin can greatly increase Tizanidine exposure.', 'Do not combine; select another antibiotic.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000a'), 'Ciprofloxacin', 'Theophylline', 'SEVERE', 'Ciprofloxacin can raise Theophylline concentration and toxicity risk.', 'Reduce Theophylline dose and monitor levels if coadministration is unavoidable.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000b'), 'Ibuprofen', 'Lithium', 'SEVERE', 'NSAIDs can reduce Lithium clearance and increase toxicity.', 'Avoid the combination or monitor Lithium closely.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000c'), 'Lisinopril', 'Potassium Chloride', 'MODERATE', 'The combination may increase hyperkalemia risk.', 'Monitor serum potassium and limit supplementation unless necessary.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000d'), 'Paracetamol', 'Warfarin', 'MODERATE', 'Prolonged high-dose Paracetamol may increase INR in patients taking Warfarin.', 'Use the lowest effective Paracetamol dose and monitor INR.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000e'), 'Fluoxetine', 'Tramadol', 'SEVERE', 'The combination may increase serotonin syndrome and seizure risk.', 'Avoid the combination; consider another analgesic.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000f'), 'Clopidogrel', 'Omeprazole', 'MODERATE', 'Omeprazole may reduce Clopidogrel activation through CYP2C19 inhibition.', 'Prefer Pantoprazole or another lower-interaction acid suppressor.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000010'), 'Cetirizine', 'Diphenhydramine', 'MILD', 'Dual antihistamine therapy may increase sedation.', 'Advise patients to monitor drowsiness and avoid driving if sedated.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('17100000-0000-0000-0000-000000000001'), 'Amoxicillin', 'Amoxicillin + Clavulanic acid', 'MODERATE', 'This duplicates penicillin therapy.', 'Do not prescribe both together unless there is a clear justification.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('17100000-0000-0000-0000-000000000002'), 'Azithromycin', 'Levofloxacin', 'SEVERE', 'The combination may increase QT prolongation risk.', 'Avoid the combination and consider an alternative antibiotic.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('17100000-0000-0000-0000-000000000003'), 'Omeprazole', 'Esomeprazole', 'MODERATE', 'This duplicates proton pump inhibitor therapy.', 'Use only one proton pump inhibitor at a time.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('17100000-0000-0000-0000-000000000004'), 'Cetirizine', 'Loratadine', 'MODERATE', 'This duplicates H1-antihistamine therapy.', 'Use only one antihistamine to reduce excess sedation.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('17100000-0000-0000-0000-000000000005'), 'Metformin', 'Gliclazide', 'MILD', 'The combination can increase hypoglycemia risk.', 'Monitor blood glucose and counsel the patient on hypoglycemia symptoms.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('17100000-0000-0000-0000-000000000006'), 'Diclofenac', 'Warfarin', 'CONTRAINDICATED', 'Diclofenac can substantially increase bleeding risk when used with Warfarin.', 'Avoid the combination; consider Paracetamol if analgesia is needed.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('17100000-0000-0000-0000-000000000007'), 'Atorvastatin', 'Azithromycin', 'MODERATE', 'Azithromycin may increase Atorvastatin exposure in susceptible patients.', 'Monitor for muscle pain and consider holding the statin during short antibiotic courses.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('17100000-0000-0000-0000-000000000008'), 'Metformin', 'Prednisolone', 'MODERATE', 'Prednisolone can worsen glycemic control in patients taking Metformin.', 'Monitor blood glucose during corticosteroid treatment.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('17100000-0000-0000-0000-000000000009'), 'Loperamide', 'Oral rehydration salts', 'MILD', 'Loperamide may mask symptoms in infectious diarrhea while rehydration is ongoing.', 'Assess the cause of diarrhea and prioritize rehydration.', TRUE, '2026-08-01 01:10:00', NULL);

-- ===========================
-- Prescription headers
-- ===========================

INSERT INTO prescriptions (
    id, prescription_code, medical_record_id, status, note,
    prescribed_by, prescribed_at, updated_by, updated_at
) VALUES
(UUID_TO_BIN('16200000-0000-0000-0000-000000000001'), 'RX000001', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'DISPENSED', 'Dieu tri dau dau trieu chung.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:30:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'), '2026-08-20 02:35:00'),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000002'), 'RX000002', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'DISPENSED', 'Don dieu tri viem duong ho hap cap.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:35:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'), '2026-08-20 02:40:00'),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000003'), 'RX000003', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'PENDING_DISPENSE', 'Don mau cho cap phat va dieu chinh.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:40:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:45:00'),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000004'), 'RX000004', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'CANCELLED', 'Don huy do thay doi phuong an dieu tri.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:42:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:46:00'),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000005'), 'RX000005', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'PENDING_DISPENSE', 'Don mau co canh bao tuong tac da duoc xac nhan.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:47:00', NULL, NULL),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000006'), 'RX000006', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'PENDING_DISPENSE', 'Don theo doi benh man tinh.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:50:00', NULL, NULL);

-- ===========================
-- Prescription items
-- ===========================

INSERT INTO prescription_items (
    id, prescription_id, medicine_id, medicine_name, active_ingredient,
    strength, unit, dosage, frequency, route, duration_days, quantity,
    instructions, created_at, updated_at
) VALUES
(UUID_TO_BIN('16300000-0000-0000-0000-000000000001'), UUID_TO_BIN('16200000-0000-0000-0000-000000000001'), UUID_TO_BIN('16000000-0000-0000-0000-000000000001'), 'Paracetamol 500 mg', 'Paracetamol', '500 mg', 'vien', '1 vien', 'Moi 8 gio khi dau', 'ORAL', 3, 9, 'Uong sau an, toi da 3 g moi ngay.', '2026-08-20 02:30:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000002'), UUID_TO_BIN('16200000-0000-0000-0000-000000000001'), UUID_TO_BIN('16000000-0000-0000-0000-000000000007'), 'Omeprazole 20 mg', 'Omeprazole', '20 mg', 'vien', '1 vien', 'Moi sang', 'ORAL', 7, 7, 'Uong truoc an sang 30 phut.', '2026-08-20 02:30:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000003'), UUID_TO_BIN('16200000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000003'), 'Amoxicillin 500 mg', 'Amoxicillin', '500 mg', 'vien', '1 vien', 'Moi 8 gio', 'ORAL', 5, 15, 'Dung du lieu trinh dieu tri.', '2026-08-20 02:35:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000004'), UUID_TO_BIN('16200000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000012'), 'Ambroxol 30 mg', 'Ambroxol', '30 mg', 'vien', '1 vien', 'Ngay 3 lan', 'ORAL', 5, 15, 'Uong nhieu nuoc.', '2026-08-20 02:35:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000005'), UUID_TO_BIN('16200000-0000-0000-0000-000000000003'), UUID_TO_BIN('16000000-0000-0000-0000-000000000016'), 'Metformin 500 mg', 'Metformin', '500 mg', 'vien', '1 vien', 'Ngay 2 lan', 'ORAL', 30, 60, 'Uong trong hoac sau an.', '2026-08-20 02:40:00', '2026-08-20 02:45:00'),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000006'), UUID_TO_BIN('16200000-0000-0000-0000-000000000003'), UUID_TO_BIN('16000000-0000-0000-0000-000000000017'), 'Gliclazide MR 30 mg', 'Gliclazide', '30 mg', 'vien', '1 vien', 'Moi sang', 'ORAL', 30, 30, 'Uong cung bua an sang.', '2026-08-20 02:45:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000007'), UUID_TO_BIN('16200000-0000-0000-0000-000000000004'), UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), 'Ibuprofen 400 mg', 'Ibuprofen', '400 mg', 'vien', '1 vien', 'Ngay 2 lan', 'ORAL', 3, 6, 'Don nay da huy, khong cap phat.', '2026-08-20 02:42:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000008'), UUID_TO_BIN('16200000-0000-0000-0000-000000000005'), UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), 'Ibuprofen 400 mg', 'Ibuprofen', '400 mg', 'vien', '1 vien', 'Ngay 2 lan', 'ORAL', 3, 6, 'Can nhac nguy co xuat huyet va theo doi sat.', '2026-08-20 02:47:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000009'), UUID_TO_BIN('16200000-0000-0000-0000-000000000005'), UUID_TO_BIN('16000000-0000-0000-0000-000000000029'), 'Warfarin 2 mg', 'Warfarin', '2 mg', 'vien', '1 vien', 'Moi toi', 'ORAL', 7, 7, 'Dung theo chi dinh chong dong hien tai.', '2026-08-20 02:47:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000010'), UUID_TO_BIN('16200000-0000-0000-0000-000000000006'), UUID_TO_BIN('16000000-0000-0000-0000-000000000019'), 'Amlodipine 5 mg', 'Amlodipine', '5 mg', 'vien', '1 vien', 'Moi sang', 'ORAL', 30, 30, 'Theo doi huyet ap tai nha.', '2026-08-20 02:50:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000011'), UUID_TO_BIN('16200000-0000-0000-0000-000000000006'), UUID_TO_BIN('16000000-0000-0000-0000-000000000020'), 'Losartan 50 mg', 'Losartan', '50 mg', 'vien', '1 vien', 'Moi sang', 'ORAL', 30, 30, 'Theo doi huyet ap va chuc nang than.', '2026-08-20 02:50:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000012'), UUID_TO_BIN('16200000-0000-0000-0000-000000000006'), UUID_TO_BIN('16000000-0000-0000-0000-000000000018'), 'Atorvastatin 20 mg', 'Atorvastatin', '20 mg', 'vien', '1 vien', 'Moi toi', 'ORAL', 30, 30, 'Uong buoi toi.', '2026-08-20 02:50:00', NULL);

-- ===========================
-- Warning logs and amendments
-- ===========================

INSERT INTO prescription_warning_logs (
    id, prescription_id, rule_id, first_medicine_id,
    second_medicine_id, severity, warning_message, action, override_reason,
    handled_by, handled_at, created_at
) VALUES
(UUID_TO_BIN('16400000-0000-0000-0000-000000000001'), UUID_TO_BIN('16200000-0000-0000-0000-000000000005'), UUID_TO_BIN('d1a00000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000029'), 'SEVERE', 'Ibuprofen increases bleeding risk when used with Warfarin. Recommendation: Avoid NSAIDs when possible; consider Paracetamol and monitor INR closely.', 'OVERRIDDEN', 'Bac si da danh gia loi ich vuot nguy co trong thoi gian dieu tri rat ngan.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:47:00', '2026-08-20 02:47:00'),
(UUID_TO_BIN('16400000-0000-0000-0000-000000000002'), UUID_TO_BIN('16200000-0000-0000-0000-000000000003'), UUID_TO_BIN('17100000-0000-0000-0000-000000000005'), UUID_TO_BIN('16000000-0000-0000-0000-000000000016'), UUID_TO_BIN('16000000-0000-0000-0000-000000000017'), 'MILD', 'The combination can increase hypoglycemia risk. Recommendation: Monitor blood glucose and counsel the patient on hypoglycemia symptoms.', 'OVERRIDDEN', 'Phac do phoi hop duoc bac si chi dinh va da huong dan benh nhan theo doi ha duong huyet.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:45:00', '2026-08-20 02:45:00');

INSERT INTO prescription_amendments (
    id, prescription_id, change_reason, before_data, after_data,
    amended_by, amended_at
) VALUES
(
    UUID_TO_BIN('16500000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16200000-0000-0000-0000-000000000003'),
    'Bo sung gliclazide theo duong huyet luc doi cao.',
    JSON_OBJECT('schemaVersion', 1, 'prescriptionCode', 'RX000003', 'status', 'PENDING_DISPENSE', 'note', 'Don mau cho cap phat va dieu chinh.', 'items', JSON_ARRAY(JSON_OBJECT('medicineName', 'Metformin 500 mg', 'dosage', '1 vien', 'quantity', 60))),
    JSON_OBJECT('schemaVersion', 1, 'prescriptionCode', 'RX000003', 'status', 'PENDING_DISPENSE', 'note', 'Don mau cho cap phat va dieu chinh.', 'items', JSON_ARRAY(JSON_OBJECT('medicineName', 'Metformin 500 mg', 'dosage', '1 vien', 'quantity', 60), JSON_OBJECT('medicineName', 'Gliclazide MR 30 mg', 'dosage', '1 vien', 'quantity', 30))),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-20 02:45:00'
);

-- The next automatically generated code is RX000007.
INSERT INTO prescription_code_sequences (code_prefix, `last_value`)
VALUES ('RX', 6);
