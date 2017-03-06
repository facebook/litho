// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.annotation.processing.Messager;

import java.util.List;

class MultiPrintableException extends PrintableException {
  private final List<PrintableException> exceptions;

  MultiPrintableException(List<PrintableException> exceptions) {
    this.exceptions = exceptions;
  }

  public void print(Messager messager) {
    for (PrintableException e : exceptions) {
      e.print(messager);
    }
  }
}
