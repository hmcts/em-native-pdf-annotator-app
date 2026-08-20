package uk.gov.hmcts.reform.em.npa.rest.errors;

import org.springframework.context.annotation.Profile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("exception-test-controller-enabled")
public class ExceptionTranslatorTestController {

    @GetMapping("/test/type-mismatch")
    public void triggerTypeMismatch(@RequestParam("id") Long id) {

    }

    @GetMapping("/test/missing-param")
    public void triggerMissingParam(@RequestParam("requiredParam") String param) {

    }

    @PostMapping("/test/validation-failure")
    public void triggerValidation(@RequestBody Object body) throws MethodArgumentNotValidException,
            NoSuchMethodException {
        // Programmatically construct Spring validation binding errors
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(body, "testDTO");
        bindingResult.addError(new FieldError("testDTO", "fieldName", null,
                false, new String[]{"NotNull"}, null, "Default validation error"));


        java.lang.reflect.Method method = ExceptionTranslatorTestController.class
                .getDeclaredMethod("triggerValidation", Object.class);
        org.springframework.core.MethodParameter methodParameter =
                new org.springframework.core.MethodParameter(method, 0);

        throw new MethodArgumentNotValidException(methodParameter, bindingResult);
    }
}
