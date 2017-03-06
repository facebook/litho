// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class ComponentsProcessingException extends PrintableException {
  private final Element element;

  public ComponentsProcessingException(String message) {
    this(null, message);
  }

  public ComponentsProcessingException(Element element, String message) {
    super(message);
    this.element = element;
  }

  public void print(Messager messager) {
    messager.printMessage(Diagnostic.Kind.ERROR, getMessage(), element);
  }
}
