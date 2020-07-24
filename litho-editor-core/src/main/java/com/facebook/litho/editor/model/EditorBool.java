/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.editor.model;

import javax.annotation.Nullable;

/** Wraps over a boolean to make it an EditorValue */
public final class EditorBool extends EditorValue {

  public final boolean value;

  public EditorBool(boolean value) {
    this.value = value;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof EditorBool)) {
      return false;
    }
    final EditorBool other = (EditorBool) o;
    final Object thisValue = this.value;
    final Object otherValue = other.value;
    return thisValue == null ? otherValue == null : thisValue.equals(otherValue);
  }

  @Override
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final Object objValue = this.value;
    result = result * PRIME + objValue.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @Override
  public <R> R when(EditorVisitor<R> visitor) {
    return visitor.isBool(this);
  }
}
