package com.benhsoan.port.outbound.repository.medicine;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;

public record MedicineSearchCriteria(
        String keyword,
        DosageForm dosageForm,
        AdministrationRoute defaultRoute,
        Boolean active
) {

    public MedicineSearchCriteria {
        keyword = normalizeKeyword(keyword);
    }

    public static MedicineSearchCriteria all() {
        return new MedicineSearchCriteria(null, null, null, null);
    }

    public static MedicineSearchCriteria activeOnly() {
        return new MedicineSearchCriteria(null, null, null, true);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }
}
