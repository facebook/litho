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

public class SpecModelValidationError {
  public final Object element;
  public final Object annotation;
  public final String message;

  public SpecModelValidationError(Object element, String message) {
    this(element, null, message);
  }

  public SpecModelValidationError(Object element, Object annotation, String message) {
    this.element = element;
    this.annotation = annotation;
    this.message = message;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " { " + message + " }";
  }
}
