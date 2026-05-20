package com.africa.ubaxplatform.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = MinWordsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MinWords {

  int value() default 10;

  String message() default "La description doit contenir au moins {value} mots";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
