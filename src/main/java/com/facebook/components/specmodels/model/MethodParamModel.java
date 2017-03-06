// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import java.lang.annotation.Annotation;
import java.util.List;

import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a method param.
 */
public interface MethodParamModel {

  /**
   * @return the type of the param.
   */
  TypeName getType();

  /**
   * @return the name of the param.
   */
  String getName();

  /**
   * @return all annotations that are on the param.
   */
  List<Annotation> getAnnotations();

  /**
   * @return the object that this model represents.
   */
  Object getRepresentedObject();
}
