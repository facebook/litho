/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.TypeName;

/**
 * Describes a method signature.
 *
 * We use method descriptions to refer to abstract methods defined in {@link ComponentLifecycle},
 * so that we can define implementations that delegate to client-declared methods with annotated
 * props.
 */
public class MethodDescription {
  public Class[] annotations;
  public Modifier accessType;
  public TypeName returnType;
  public String name;
