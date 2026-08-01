package org.learn.currencyexchanger.user.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.learn.currencyexchanger.user.domain.UsernamePolicy;
import org.learn.currencyexchanger.user.domain.exception.InvalidUsernameException;


public final class UsernameValidator implements
        ConstraintValidator<ValidUsername, String> {

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context
    ) {
        if (value == null || value.isBlank()) {
            return true;
        }

        try {
            UsernamePolicy.normalize(value);
            return true;
        } catch (InvalidUsernameException exception) {
            return false;
        }
    }
}
