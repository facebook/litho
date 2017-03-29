/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import java.util.List;

import com.facebook.litho.annotations.ResType;

import com.squareup.javapoet.ClassName;

class PropParameter {
  final Parameter parameter;
  final boolean optional;
  final ResType resType;
  final List<ClassName> annotations;

  PropParameter(
      Parameter parameter,
      boolean optional,
      ResType resType,
      List<ClassName> annotations) {
    this.parameter = parameter;
    this.optional = optional;
    this.resType = resType;
    this.annotations = annotations;
  }
}
