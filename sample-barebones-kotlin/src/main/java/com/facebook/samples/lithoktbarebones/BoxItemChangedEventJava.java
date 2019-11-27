package com.facebook.samples.lithoktbarebones;

import com.facebook.litho.annotations.Event;

@Event
public class BoxItemChangedEventJava {
  public static final int A_RANDOM_CONST = 1;

  private final int newColor;
  private final String newStatus;
  private final int newIndex;
  private final boolean newBoolean;

  public BoxItemChangedEventJava(int newColor, String newStatus, int newIndex, boolean newBoolean) {
    this.newColor = newColor;
    this.newStatus = newStatus;
    this.newIndex = newIndex;
    this.newBoolean = newBoolean;
  }

  public int getNewColor() {
    return newColor;
  }

  public String getNewStatus() {
    return newStatus;
  }

  public int getNewIndex() {
    return newIndex;
  }

  public boolean isNewBoolean() {
    return newBoolean;
  }
}
