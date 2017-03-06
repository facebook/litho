// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

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
  public TypeName[] parameterTypes;
  public TypeName[] exceptions;
}
