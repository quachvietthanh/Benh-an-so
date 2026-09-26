export const ALLERGY_SEVERITIES = {
  MILD: {
    key: 'MILD',
    label: 'Nhẹ',
    color: 'gold',
    tagColor: '#eab308',
    bg: '#fef9c3',
    border: '#fde047',
    text: '#854d0e',
    rank: 1,
    description: 'Mẩn ngứa, phát ban nhẹ, không có triệu chứng toàn thân',
  },
  MODERATE: {
    key: 'MODERATE',
    label: 'Trung bình',
    color: 'orange',
    tagColor: '#f97316',
    bg: '#ffedd5',
    border: '#fed7aa',
    text: '#9a3412',
    rank: 2,
    description: 'Mề đay toàn thân, sưng môi/mí mắt, buồn nôn, sốt nhẹ',
  },
  SEVERE: {
    key: 'SEVERE',
    label: 'Nặng',
    color: 'red',
    tagColor: '#ef4444',
    bg: '#fee2e2',
    border: '#fca5a5',
    text: '#991b1b',
    rank: 3,
    description: 'Khó thở, co thắt phế quản, phù thanh quản, đau bụng dữ dội',
  },
  ANAPHYLAXIS: {
    key: 'ANAPHYLAXIS',
    label: 'Sốc phản vệ (Nguy cơ tử vong)',
    color: '#b91c1c',
    tagColor: '#b91c1c',
    bg: '#fef2f2',
    border: '#ef4444',
    text: '#7f1d1d',
    rank: 4,
    description: 'Tụt huyết áp, trụy tim mạch, suy hô hấp cấp, đe dọa tính mạng khẩn cấp',
  },
}

export const ALLERGY_SEVERITY_OPTIONS = [
  { value: 'MILD', label: 'Nhẹ (Mẩn ngứa, phát ban nhẹ)' },
  { value: 'MODERATE', label: 'Trung bình (Mề đay toàn thân, sưng mặt/môi)' },
  { value: 'SEVERE', label: 'Nặng (Khó thở, phù thanh quản, co thắt)' },
  { value: 'ANAPHYLAXIS', label: 'Sốc phản vệ (Trụy mạch, tụy huyết áp - Cực kỳ nguy hiểm)' },
]

export const COMMON_MEDICATION_ALLERGENS = [
  'Penicillin / Amoxicillin',
  'Cephalosporin (Cefalexin, Cefuroxime, Ceftriaxone)',
  'Aspirin / Nhóm NSAIDs (Ibuprofen, Diclofenac)',
  'Sulfonamide (Bactrim / Cotrimoxazol)',
  'Paracetamol (Acetaminophen)',
  'Macrolide (Erythromycin, Azithromycin, Clarithromycin)',
  'Quinolone (Ciprofloxacin, Levofloxacin)',
  'Tetracycline / Doxycycline',
  'Thuốc cản quang có chứa Iod',
  'Thuốc tê cục bộ (Lidocaine, Procaine)',
]

export const normalizeAllergenName = (name) => {
  return String(name || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ')
}

export const getAllergySeverityMeta = (severity) => {
  return ALLERGY_SEVERITIES[severity] || ALLERGY_SEVERITIES.MILD
}

/**
 * Kiểm tra xem một loại thuốc có xung đột với tiền sử dị ứng đã biết của bệnh nhân không
 * @param {Object} medicine Đối tượng thuốc (name, activeIngredient, ingredient, etc.)
 * @param {Array} patientAllergies Danh sách dị ứng thuốc của bệnh nhân
 * @returns {Object} { hasConflict: boolean, matchedAllergy: Object|null, warningMessage: string }
 */
export const checkPrescriptionAllergyConflict = (medicine, patientAllergies = []) => {
  if (!medicine || !Array.isArray(patientAllergies) || patientAllergies.length === 0) {
    return { hasConflict: false, matchedAllergy: null, warningMessage: '' }
  }

  const medName = normalizeAllergenName(medicine.name || medicine.medicineName || '')
  const medIngredient = normalizeAllergenName(
    medicine.activeIngredient || medicine.ingredient || medicine.active_ingredient || ''
  )

  for (const allergy of patientAllergies) {
    if (allergy.active === false) continue

    const allergen = normalizeAllergenName(allergy.allergenName)
    if (!allergen) continue

    // Tách các từ khóa hoặc tên nhóm thuốc nếu có dấu gạch chéo hoặc dấu phẩy
    const subTerms = allergen
      .split(/[/,()]/)
      .map((t) => t.trim())
      .filter((t) => t.length >= 3)

    const isDirectMatch =
      (medName && medName.includes(allergen)) ||
      (allergen && allergen.includes(medName)) ||
      (medIngredient && medIngredient.includes(allergen)) ||
      (allergen && allergen.includes(medIngredient))

    const isSubTermMatch = subTerms.some((term) => {
      return (
        (medName && medName.includes(term)) ||
        (medIngredient && medIngredient.includes(term))
      )
    })

    if (isDirectMatch || isSubTermMatch) {
      const severityMeta = getAllergySeverityMeta(allergy.severity)
      const isLifeThreatening = allergy.severity === 'ANAPHYLAXIS' || allergy.severity === 'SEVERE'
      return {
        hasConflict: true,
        matchedAllergy: allergy,
        severity: allergy.severity,
        severityMeta,
        isLifeThreatening,
        warningMessage: `Thuốc "${medicine.name || medicine.medicineName}" có nguy cơ ${
          isLifeThreatening ? 'SỐC PHẢN VỆ / NGUY HIỂM TÍNH MẠNG' : 'DỊ ỨNG'
        } do bệnh nhân có tiền sử dị ứng với "${allergy.allergenName}" (Mức độ: ${severityMeta.label}${
          allergy.reaction ? ` - Biểu hiện: ${allergy.reaction}` : ''
        })!`,
      }
    }
  }

  return { hasConflict: false, matchedAllergy: null, warningMessage: '' }
}
