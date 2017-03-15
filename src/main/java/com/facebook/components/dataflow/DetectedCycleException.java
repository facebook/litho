package com.facebook.components.dataflow;

/**
 * Exception thrown when a runtime cycle is detected.
 */
public class DetectedCycleException extends BadGraphSetupException {

  public DetectedCycleException(String detailMessage) {
    super(detailMessage);
  }
}
