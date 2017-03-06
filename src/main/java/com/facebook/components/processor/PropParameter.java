// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import java.util.List;

import com.facebook.components.annotations.ResType;

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
