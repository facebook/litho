/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.util.Objects;
import javax.annotation.Nullable;

public class WorkingRangeDeclarationModel {

  @Nullable public final String name;
  @Nullable public final Object representedObject;

  public WorkingRangeDeclarationModel(@Nullable String name, @Nullable Object representedObject) {
    this.name = name;
    this.representedObject = representedObject;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WorkingRangeDeclarationModel that = (WorkingRangeDeclarationModel) o;
    return Objects.equals(name, that.name)
        && Objects.equals(representedObject, that.representedObject);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, representedObject);
  }
}
