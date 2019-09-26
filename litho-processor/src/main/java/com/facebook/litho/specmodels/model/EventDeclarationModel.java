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

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/** Model that is an abstract representation of a {@link com.facebook.litho.annotations.Event}. */
@Immutable
public class EventDeclarationModel {
  public final ClassName name;
  public final TypeName returnType;
  public final ImmutableList<FieldModel> fields;
  public final Object representedObject;

  public EventDeclarationModel(
      ClassName name,
      TypeName returnType,
      ImmutableList<FieldModel> fields,
      Object representedObject) {
    this.name = name;
    this.returnType = returnType;
    this.fields = fields;
    this.representedObject = representedObject;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EventDeclarationModel that = (EventDeclarationModel) o;
    return Objects.equals(name, that.name)
        && Objects.equals(returnType, that.returnType)
        && Objects.equals(fields, that.fields)
        && Objects.equals(representedObject, that.representedObject);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, returnType, fields, representedObject);
  }
}
