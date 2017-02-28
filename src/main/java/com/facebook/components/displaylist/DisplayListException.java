// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.displaylist;

public class DisplayListException extends Exception {

  public DisplayListException(Exception originatingException) {
    super(originatingException);
  }
}
