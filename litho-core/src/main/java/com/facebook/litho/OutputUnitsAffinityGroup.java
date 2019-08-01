/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import javax.annotation.Nullable;

/**
 * A family of output units ({@link LayoutOutput}s or {@link MountItem}s) generated for the same
 * {@link Component}. Used by {@link LayoutState}, {@link MountState} and {@link TransitionManager}
 * to group items subjected to same {@link Transition} set to their originative {@link Component}
 */
public class OutputUnitsAffinityGroup<T> {
  private final Object[] mContent = new Object[5];
  private short mSize = 0;

  public OutputUnitsAffinityGroup() {}

  public OutputUnitsAffinityGroup(OutputUnitsAffinityGroup<T> group) {
    for (int index = 0, size = mContent.length; index < size; index++) {
      mContent[index] = group.mContent[index];
    }
    mSize = group.mSize;
  }

  public void add(@OutputUnitType int type, T value) {
    if (value == null) {
      throw new IllegalArgumentException("value should not be null");
    }

    if (mContent[type] != null) {
      throw new RuntimeException("Already contains unit for type " + typeToString(type));
    }

    if ((mContent[OutputUnitType.HOST] != null) || (type == OutputUnitType.HOST && mSize > 0)) {
      throw new RuntimeException(
          "OutputUnitType.HOST unit should be the only member of an OutputUnitsAffinityGroup");
    }

    mContent[type] = value;
    mSize++;
  }

  public void replace(@OutputUnitType int type, T value) {
    if (value != null && mContent[type] != null) {
      mContent[type] = value;
    } else if (value != null && mContent[type] == null) {
      add(type, value);
    } else if (value == null && mContent[type] != null) {
      mContent[type] = null;
      mSize--;
    }
  }

  public T getContentType(@OutputUnitType int type) {
    return (T) mContent[type];
  }

  public int size() {
    return mSize;
  }

  public boolean isEmpty() {
    return mSize == 0;
  }

  public @OutputUnitType int typeAt(int index) {
    if (index < 0 || index >= mSize) {
      throw new IndexOutOfBoundsException("index=" + index + ", size=" + mSize);
    }
    int i = 0, j = 0;
    while (j <= index) {
      if (mContent[i] != null) {
        j++;
      }
      i++;
    }
    return i - 1;
  }

  public T getAt(int index) {
    return getContentType(typeAt(index));
  }

  public T getMostSignificantUnit() {
    if (mContent[OutputUnitType.HOST] != null) {
      return getContentType(OutputUnitType.HOST);
    } else if (mContent[OutputUnitType.CONTENT] != null) {
      return getContentType(OutputUnitType.CONTENT);
    } else if (mContent[OutputUnitType.BACKGROUND] != null) {
      return getContentType(OutputUnitType.BACKGROUND);
    } else if (mContent[OutputUnitType.FOREGROUND] != null) {
      return getContentType(OutputUnitType.FOREGROUND);
    } else {
      return getContentType(OutputUnitType.BORDER);
    }
  }

  public void clean() {
    for (int i = 0; i < mContent.length; i++) {
      mContent[i] = null;
    }
    mSize = 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OutputUnitsAffinityGroup<?> that = (OutputUnitsAffinityGroup<?>) o;
    if (mSize != that.mSize) {
      return false;
    }
    for (int i = 0; i < mContent.length; i++) {
      if (mContent[i] != that.mContent[i]) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    for (int index = 0; index < size(); ++index) {
      final @OutputUnitType int type = typeAt(index);
      final T unit = getAt(index);
      sb.append("\n\t").append(typeToString(type)).append(": ").append(unit.toString());
    }
    return sb.toString();
  }

  private static @Nullable String typeToString(@OutputUnitType int type) {
    switch (type) {
      case OutputUnitType.CONTENT:
        return "CONTENT";

      case OutputUnitType.BACKGROUND:
        return "BACKGROUND";

      case OutputUnitType.FOREGROUND:
        return "FOREGROUND";

      case OutputUnitType.HOST:
        return "HOST";

      case OutputUnitType.BORDER:
        return "BORDER";

      default:
        return null;
    }
  }
}
