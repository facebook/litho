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

import java.util.ArrayList;
import java.util.List;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

/**
 * Builder for the static state update methods of a Component
 */
class OnStateUpdateMethodSpecBuilder {
  private String mUpdateMethodName;
  private TypeName mContextClass;
  private ClassName mComponentClass;
  private String mLifecycleImplClass;

  private final List<Parameter> mUpdateParams = new ArrayList<>();
  private final List<String> mTypeParameters = new ArrayList<>();
  private String mStateUpdateClassName;
  private boolean mIsAsync;

  OnStateUpdateMethodSpecBuilder updateMethodName(String updateMethodName) {
    this.mUpdateMethodName = updateMethodName;
    return this;
  }

  OnStateUpdateMethodSpecBuilder contextClass(TypeName contextClass) {
    this.mContextClass = contextClass;
    return this;
  }

  OnStateUpdateMethodSpecBuilder updateMethodParams(List<Parameter> eventParams) {
    this.mUpdateParams.addAll(eventParams);
    return this;
  }

  OnStateUpdateMethodSpecBuilder updateMethodParam(Parameter eventParam) {
    this.mUpdateParams.add(eventParam);
    return this;
  }

  OnStateUpdateMethodSpecBuilder typeParameter(String typeParam) {
    this.mTypeParameters.add(typeParam);
    return this;
  }

  OnStateUpdateMethodSpecBuilder componentClass(ClassName componentClass) {
    mComponentClass = componentClass;
    return this;
  }

  OnStateUpdateMethodSpecBuilder lifecycleImplClass(String lifecycleImplClass) {
    mLifecycleImplClass = lifecycleImplClass;
    return this;
  }

  OnStateUpdateMethodSpecBuilder stateUpdateClassName(String stateUpdateClassName) {
    mStateUpdateClassName = stateUpdateClassName;
    return this;
  }

  OnStateUpdateMethodSpecBuilder async(boolean isAsync) {
    mIsAsync = isAsync;
    return this;
  }

  MethodSpec build() {
    final MethodSpec.Builder builder = MethodSpec.methodBuilder(mUpdateMethodName)
        .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
        .addParameter(mContextClass, "c");
    for (String typeParam : mTypeParameters) {
      builder.addTypeVariable(TypeVariableName.get(typeParam));
    }
    for (Parameter eventParam : mUpdateParams) {
      builder.addParameter(eventParam.type, eventParam.name);
    }

    builder.addStatement(
        "$T _component = c.get$LScope()",
        mComponentClass,
        mComponentClass.simpleName())
        .addCode(
            CodeBlock.builder()
                .beginControlFlow("if (_component == null)")
                .addStatement("return")
                .endControlFlow()
                .build());
    final CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
    codeBlockBuilder.add(mLifecycleImplClass +
        "." +
        mStateUpdateClassName +
        " _stateUpdate = ((" +
        mLifecycleImplClass+"."+
        mLifecycleImplClass +
        "Impl) _component).create" +
        mStateUpdateClassName +
        "(");

    for (int i = 0, size = mUpdateParams.size(); i < size; i++) {
      codeBlockBuilder.add(mUpdateParams.get(i).name);
      if (i < size -1) {
        codeBlockBuilder.add(", ");
      }
    }
    codeBlockBuilder.add(");\n");

    builder.addCode(codeBlockBuilder.build());
    if (mIsAsync) {
      builder.addStatement("c.updateStateAsync(_stateUpdate)");
    } else {
      builder.addStatement("c.updateState(_stateUpdate)");
    }

    return builder.build();
  }
}
