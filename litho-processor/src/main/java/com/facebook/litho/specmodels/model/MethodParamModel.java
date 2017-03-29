/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.lang.annotation.Annotation;
import java.util.List;

import com.squareup.javapoet.AnnotationSpec;
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
   * @return all components library annotations that are on the param.
   */
  List<Annotation> getAnnotations();

  /**
   * @return all non-library annotations that are on the param.
   */
  List<AnnotationSpec> getExternalAnnotations();

  /**
   * @return the object that this model represents.
   */
  Object getRepresentedObject();
}
