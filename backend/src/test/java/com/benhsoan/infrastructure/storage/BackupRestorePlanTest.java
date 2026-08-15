package com.benhsoan.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.benhsoan.config.BackupStorageConfiguration;
import com.benhsoan.infrastructure.storage.BackupRestorePlan.DeferredForeignKey;
import com.benhsoan.infrastructure.storage.BackupRestorePlan.ForeignKeyDependency;

class BackupRestorePlanTest {

    @Test
    void fullPlanContainsEveryOperationalParentAndApprovedExternalBoundary() {
        BackupRestorePlan plan = new BackupStorageConfiguration().fullBackupRestorePlan();

        assertTrue(plan.allowedTables().containsAll(List.of(
                "diagnosis_catalog", "clinical_service_catalog", "drug_interaction_rules",
                "prescription_code_sequences", "invoice_code_sequences", "payments"
        )));
        assertEquals(Set.of("users"), plan.approvedExternalParentTables());
        assertTrue(plan.dependencies().stream().allMatch(dependency ->
                plan.allowedTables().contains(dependency.childTable())
                        && plan.allowedTables().contains(dependency.parentTable())));
    }

    @Test
    void restoreGraphRemovesOnlyExplicitlyDeferredCyclicEdges() {
        BackupRestorePlan plan = new BackupStorageConfiguration().fullBackupRestorePlan();

        assertTrue(plan.dependencyGraph().get("visits").contains("queue_items"));
        assertTrue(plan.dependencyGraph().get("invoices").contains("invoices"));
        assertFalse(plan.restoreDependencyGraph().get("visits").contains("queue_items"));
        assertFalse(plan.restoreDependencyGraph().get("invoices").contains("invoices"));
        assertTrue(plan.restoreDependencyGraph().get("invoices").contains("payments"));
    }

    @Test
    void rejectsDeferredForeignKeyOutsideDependencyGraph() {
        assertThrows(IllegalArgumentException.class, () -> new BackupRestorePlan(
                List.of("parents", "children"),
                Set.of(),
                List.of(new ForeignKeyDependency("children", "parent_id", "parents")),
                Set.of(new DeferredForeignKey("children", "other_parent_id"))
        ));
    }
}
