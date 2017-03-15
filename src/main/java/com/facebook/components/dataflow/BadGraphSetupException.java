package com.facebook.components.dataflow;

/**
 * Exception thrown when the graph is not legal (e.g. doesn't represent a DAG).
 */
public class BadGraphSetupException extends RuntimeException {

  public BadGraphSetupException(String detailMessage) {
    super(detailMessage);
  }
}
