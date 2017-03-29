/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import com.squareup.javapoet.TypeName;

public class Parameter {
  public final TypeName type;
  public final String name;

  public Parameter(TypeName type, String name) {
    this.type = type;
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Parameter) {
      final Parameter p = (Parameter) o;
      return type.equals(p.type) && name.equals(p.name);
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }
}
