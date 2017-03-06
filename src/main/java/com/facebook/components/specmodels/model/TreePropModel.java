// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.annotation.concurrent.Immutable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a
 * {@link com.facebook.components.annotations.TreeProp}.
 */
@Immutable
public class TreePropModel implements MethodParamModel {
  private final MethodParamModel mParamModel;

  TreePropModel(MethodParamModel paramModel) {
    mParamModel = paramModel;
  }

  @Override
  public TypeName getType() {
    return mParamModel.getType();
  }

  @Override
  public String getName() {
    return mParamModel.getName();
  }

  @Override
  public List<Annotation> getAnnotations() {
    return mParamModel.getAnnotations();
  }

  @Override
  public Object getRepresentedObject() {
    return mParamModel.getRepresentedObject();
  }

  public boolean equals(Object o) {
    if (o instanceof TreePropModel) {
      final TreePropModel p = (TreePropModel) o;
      return mParamModel.equals(p.mParamModel);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mParamModel.hashCode();
  }
}
