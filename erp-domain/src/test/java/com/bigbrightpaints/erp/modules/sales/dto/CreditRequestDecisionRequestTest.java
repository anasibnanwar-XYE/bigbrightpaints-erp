package com.bigbrightpaints.erp.modules.sales.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CreditRequestDecisionRequestTest {

    @Test
    void canonicalConstructor_exposesReasonAccessor() {
        CreditRequestDecisionRequest request = new CreditRequestDecisionRequest("Risk cleared by finance");

        assertThat(request.reason()).isEqualTo("Risk cleared by finance");
    }

    @Test
    void equalsAndHashCode_sameReason_areEqual() {
        CreditRequestDecisionRequest one = new CreditRequestDecisionRequest("Approved");
        CreditRequestDecisionRequest two = new CreditRequestDecisionRequest("Approved");

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
    }

    @Test
    void equalsAndHashCode_differentReason_areNotEqual() {
        CreditRequestDecisionRequest approved = new CreditRequestDecisionRequest("Approved");
        CreditRequestDecisionRequest rejected = new CreditRequestDecisionRequest("Rejected");

        assertThat(approved).isNotEqualTo(rejected);
        assertThat(approved.hashCode()).isNotEqualTo(rejected.hashCode());
    }

    @Test
    void toString_includesRecordComponentValue() {
        CreditRequestDecisionRequest request = new CreditRequestDecisionRequest("Manual override");

        assertThat(request.toString()).contains("reason=Manual override");
    }
}
