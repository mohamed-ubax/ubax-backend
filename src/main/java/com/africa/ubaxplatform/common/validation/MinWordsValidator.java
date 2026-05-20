package com.africa.ubaxplatform.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinWordsValidator implements ConstraintValidator<MinWords, String> {

  private int min;

  @Override
  public void initialize(MinWords annotation) {
    this.min = annotation.value();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) return false;
    long wordCount =
        java.util.Arrays.stream(value.trim().split("\\s+")).filter(w -> !w.isEmpty()).count();
    return wordCount >= min;
  }
}
