package com.factory.tecnofactory.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RunValidoValidator implements ConstraintValidator<RunValido, ConRun> {

    @Override
    public boolean isValid(ConRun request, ConstraintValidatorContext context) {
        if (request == null || request.run() == null || request.runDv() == null) {
            return true;
        }

        boolean correcto = RunUtil.calcularDv(request.run()).equalsIgnoreCase(request.runDv());

        if (!correcto) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("runDv")
                    .addConstraintViolation();
        }
        return correcto;
    }
}
