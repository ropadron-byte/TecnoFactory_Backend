package com.factory.tecnofactory.validation;

import com.factory.tecnofactory.dto.UsuarioRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RunValidoValidator implements ConstraintValidator<RunValido, UsuarioRequest> {

    @Override
    public boolean isValid(UsuarioRequest request, ConstraintValidatorContext context) {
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