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

import com.squareup.javapoet.ClassName;
import javax.annotation.concurrent.Immutable;

/** Model that is an abstract representation of empty interfaces tagging component specs. */
@Immutable
public class TagModel {
  public final ClassName name;
  public final boolean hasSupertype;
  public final boolean hasMethods;
  public final Object representedObject;

  public TagModel(
      ClassName name, boolean hasSupertype, boolean hasMethods, Object representedObject) {
    this.name = name;
    this.hasSupertype = hasSupertype;
    this.hasMethods = hasMethods;
    this.representedObject = representedObject;
  }
}
