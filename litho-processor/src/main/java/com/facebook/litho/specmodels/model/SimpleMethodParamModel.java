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
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/** Model that is a simple base representation of a method param. */
@Immutable
public class SimpleMethodParamModel implements MethodParamModel {
  private final TypeSpec mTypeSpec;
  private final String mName;
  private final List<Annotation> mAnnotations;
  private final List<AnnotationSpec> mExternalAnnotations;
  private final Object mRepresentedObject;

  SimpleMethodParamModel(
      TypeSpec typeSpec,
      String name,
      List<Annotation> annotations,
      List<AnnotationSpec> externalAnnotations,
      Object representedObject) {
    mTypeSpec = typeSpec;
    mName = name;
    mAnnotations = annotations;
    mExternalAnnotations = externalAnnotations;
    mRepresentedObject = representedObject;
  }

  @Override
  public TypeSpec getTypeSpec() {
    return mTypeSpec;
  }

  @Override
  public TypeName getTypeName() {
    return mTypeSpec.getTypeName();
  }

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public List<Annotation> getAnnotations() {
    return mAnnotations;
  }

  @Override
  public List<AnnotationSpec> getExternalAnnotations() {
    return mExternalAnnotations;
  }

  @Override
  public Object getRepresentedObject() {
    return mRepresentedObject;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final SimpleMethodParamModel that = (SimpleMethodParamModel) o;
    return Objects.equals(mTypeSpec, that.mTypeSpec)
        && Objects.equals(mName, that.mName)
        && Objects.equals(mAnnotations, that.mAnnotations)
        && Objects.equals(mExternalAnnotations, that.mExternalAnnotations)
        && Objects.equals(mRepresentedObject, that.mRepresentedObject);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mTypeSpec, mName, mAnnotations, mExternalAnnotations, mRepresentedObject);
  }
}
