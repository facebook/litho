package com.facebook.components.dataflow;

/**
 * Exception thrown when a node tries to calculate and propagate an illegal value (e.g. NaN).
 */
public class IllegalValueException extends RuntimeException {

  public IllegalValueException(String detailMessage) {
    super(detailMessage);
  }
}
