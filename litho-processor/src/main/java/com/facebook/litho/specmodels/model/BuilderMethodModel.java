/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.squareup.javapoet.TypeName;

import javax.annotation.concurrent.Immutable;

/**
 * Model that is an abstract representation of implicit methods on Component builders.
 */
@Immutable
public class BuilderMethodModel {
  public static BuilderMethodModel KEY_BUILDER_METHOD = new BuilderMethodModel(
      ClassNames.STRING,
      "key");

  public final String paramName;
  public final TypeName paramType;

  public BuilderMethodModel(
      TypeName paramType, String paramName) {
    this.paramName = paramName;
    this.paramType = paramType;
  }
}
