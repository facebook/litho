// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.annotation.processing.Messager;

public abstract class PrintableException extends RuntimeException {

  PrintableException() {
    super();
  }

  PrintableException(String message) {
    super(message);
  }

  public abstract void print(Messager messager);
}
