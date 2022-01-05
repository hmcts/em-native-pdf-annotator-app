package uk.gov.hmcts.reform.em.npa.rest.errors;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;

class FieldErrorVMTest {

    FieldErrorVM fieldErrorVM = new FieldErrorVM("testObjectName", "testField", "testMessage");

    @Test
    void getObjectName() {
        assertSame("testObjectName", fieldErrorVM.getObjectName());
    }

    @Test
    void getField() {
        assertSame("testField", fieldErrorVM.getField());
    }

    @Test
    void getMessage() {
        assertSame("testMessage", fieldErrorVM.getMessage());
    }
}
