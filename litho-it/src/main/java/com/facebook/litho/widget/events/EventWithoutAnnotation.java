// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.litho.widget.events;

public class EventWithoutAnnotation {
  public int count;
  public boolean isDirty;
  public String message;

  public EventWithoutAnnotation() {}

  public EventWithoutAnnotation(int count, boolean isDirty, String message) {
    this.count = count;
    this.isDirty = isDirty;
    this.message = message;
  }
}
