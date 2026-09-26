package com.benhsoan.persistence.jpaRepository.medicine;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.persistence.entity.medicine.MedicineEntity;

public final class MedicineSpecifications {

    private static final char LIKE_ESCAPE_CHARACTER = '\\';

    private MedicineSpecifications() {
    }

    public static Specification<MedicineEntity> all() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
    }

    public static Specification<MedicineEntity> hasId(UUID id) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("id"), id);
    }

    public static Specification<MedicineEntity> hasIdIn(
            Collection<UUID> ids
    ) {
        return (root, query, criteriaBuilder) -> root.get("id").in(ids);
    }

    public static Specification<MedicineEntity> hasMedicineCode(
            String medicineCode
    ) {
        String normalizedMedicineCode = normalizeExactText(medicineCode);

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(
                                criteriaBuilder.trim(root.get("medicineCode"))
                        ),
                        normalizedMedicineCode
                );
    }

    public static Specification<MedicineEntity> hasMedicineNameIgnoreCase(
            String medicineName
    ) {
        String normalizedName = normalizeExactText(medicineName);

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(
                                criteriaBuilder.trim(root.get("medicineName"))
                        ),
                        normalizedName
                );
    }

    public static Specification<MedicineEntity>
            hasActiveIngredientIgnoreCase(String activeIngredient) {
        String normalizedActiveIngredient = normalizeExactText(
                activeIngredient
        );

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(
                                criteriaBuilder.trim(
                                        root.get("activeIngredient")
                                )
                        ),
                        normalizedActiveIngredient
                );
    }

    public static Specification<MedicineEntity> hasIdNotEqual(UUID id) {
        return (root, query, criteriaBuilder) ->
                id == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.notEqual(root.get("id"), id);
    }

    public static Specification<MedicineEntity> containsKeyword(
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%"
                    + escapeLikePattern(
                            keyword.trim().toLowerCase(Locale.ROOT)
                    )
                    + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("medicineCode")),
                            pattern,
                            LIKE_ESCAPE_CHARACTER
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("medicineName")),
                            pattern,
                            LIKE_ESCAPE_CHARACTER
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("activeIngredient")),
                            pattern,
                            LIKE_ESCAPE_CHARACTER
                    )
            );
        };
    }

    public static Specification<MedicineEntity> hasDosageForm(
            DosageForm dosageForm
    ) {
        return (root, query, criteriaBuilder) ->
                dosageForm == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(
                                root.get("dosageForm"),
                                dosageForm
                        );
    }

    public static Specification<MedicineEntity> hasDefaultRoute(
            AdministrationRoute defaultRoute
    ) {
        return (root, query, criteriaBuilder) ->
                defaultRoute == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(
                                root.get("defaultRoute"),
                                defaultRoute
                        );
    }

    public static Specification<MedicineEntity> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) ->
                active == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("active"), active);
    }

    private static String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static String normalizeExactText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Specification text value must not be blank."
            );
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
