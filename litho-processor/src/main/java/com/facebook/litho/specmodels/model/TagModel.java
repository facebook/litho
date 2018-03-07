/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
