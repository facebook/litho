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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;

/** Model that is an abstract representation of a method param. */
public interface MethodParamModel {

  /** @return the {@link TypeSpec} of the param. */
  TypeSpec getTypeSpec();

  /** @return the typeName of the param. */
  TypeName getTypeName();

  /** @return the name of the param. */
  String getName();

  /** @return all components library annotations that are on the param. */
  List<Annotation> getAnnotations();

  /** @return all non-library annotations that are on the param. */
  List<AnnotationSpec> getExternalAnnotations();

  /** @return the object that this model represents. */
  Object getRepresentedObject();
}
