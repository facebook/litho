// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.lang.model.element.Modifier;

import java.util.List;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

class CreateStateUpdateInstanceMethodSpecBuilder {
  private String mStateUpdateClassName;
  private List<Parameter> mParameters;

  CreateStateUpdateInstanceMethodSpecBuilder stateUpdateClass(String stateUpdateClass) {
    mStateUpdateClassName = stateUpdateClass;
    return this;
  }

  CreateStateUpdateInstanceMethodSpecBuilder parameters(List<Parameter> parameters) {
    mParameters = parameters;
    return this;
  }

  MethodSpec build() {
    final MethodSpec.Builder methodSpecBuilder = MethodSpec
        .methodBuilder("create" + mStateUpdateClassName)
        .addModifiers(Modifier.PRIVATE)
        .returns(ClassName.bestGuess(mStateUpdateClassName));

    if (mParameters != null) {
      for (Parameter parameter : mParameters) {
        methodSpecBuilder
            .addParameter(ParameterSpec.builder(parameter.type, parameter.name).build());
      }
    }

    final CodeBlock.Builder constructor = CodeBlock.builder();
    constructor.add("return new " + mStateUpdateClassName + "(");

    if (mParameters != null) {
      for (int i = 0, size = mParameters.size(); i < size; i++) {
        constructor.add(mParameters.get(i).name);
        if (i < mParameters.size() - 1) {
          constructor.add(", ");
        }
      }
    }
    constructor.add(");\n");

    methodSpecBuilder.addCode(constructor.build());
    return methodSpecBuilder.build();
  }
}
