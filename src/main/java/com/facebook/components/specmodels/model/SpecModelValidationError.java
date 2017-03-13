// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

public class SpecModelValidationError {
  public final Object element;
  public final String message;

  public SpecModelValidationError(Object element, String message) {
    this.element = element;
    this.message = message;
  }
}
