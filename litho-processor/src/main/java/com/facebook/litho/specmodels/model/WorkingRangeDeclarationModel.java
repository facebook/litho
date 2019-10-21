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
