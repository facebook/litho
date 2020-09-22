/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/** Generator for the cases within the event handler switch clause. */
public class EventCaseGenerator {
  private final ClassName mContextClass;
  private final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>>
      mEventMethodModels;
  private final boolean mWithErrorPropagation;

  public static final String INTERNAL_ON_ERROR_HANDLER_NAME = "__internalOnErrorHandler";

  EventCaseGenerator(
      ClassName contextClass,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> eventMethodModels,
      boolean withErrorPropagation) {
    mContextClass = contextClass;
    mEventMethodModels = eventMethodModels;
    mWithErrorPropagation = withErrorPropagation;
  }

  public void writeTo(MethodSpec.Builder methodBuilder) {
    mEventMethodModels.forEach(e -> writeCase(methodBuilder, e));

    if (mWithErrorPropagation) {
      boolean hasErrorHandler =
          mEventMethodModels.stream()
              .anyMatch(e -> e.name.toString().equals(INTERNAL_ON_ERROR_HANDLER_NAME));

      if (!hasErrorHandler) {
        writePropagatingErrorCase(methodBuilder);
      }
    }
  }

  private void writePropagatingErrorCase(MethodSpec.Builder methodBuilder) {
    methodBuilder
        .beginControlFlow("case $L:", INTERNAL_ON_ERROR_HANDLER_NAME.toString().hashCode())
        .addStatement(
            "dispatchErrorEvent(($L) eventHandler.params[0], ($L) eventState)",
            mContextClass,
            ClassNames.ERROR_EVENT)
        .addStatement("return null")
        .endControlFlow();
  }

  private void writeCase(
      MethodSpec.Builder methodBuilder,
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel) {
    methodBuilder.beginControlFlow("case $L:", eventMethodModel.name.toString().hashCode());

    final String eventVariableName = "_event";

    methodBuilder.addStatement(
        "$T $L = ($T) $L",
        eventMethodModel.typeModel.name,
        eventVariableName,
        eventMethodModel.typeModel.name,
        "eventState");

    final CodeBlock.Builder eventHandlerParams =
        CodeBlock.builder().indent().add("\n$L", "eventHandler.mHasEventDispatcher");

    int paramIndex = 0;
    for (MethodParamModel methodParamModel : eventMethodModel.methodParams) {
      final TypeName methodParamModelTypeName = methodParamModel.getTypeName();

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromEvent.class)) {
        final String methodParamModelName = methodParamModel.getName();

        final String modifiedMethodParamModelName;

        final boolean isPrivateFieldInEvent = TypeElementUtils
            .extractFieldModifiers(
                (TypeElement) eventMethodModel.typeModel.representedObject,
                methodParamModelName
            ).contains(Modifier.PRIVATE);

        if (isPrivateFieldInEvent) {
          final String accessorMethodName = methodParamModelName.substring(0,1).toUpperCase()
              + methodParamModelName.substring(1);

          if (methodParamModelTypeName == TypeName.BOOLEAN) {
            modifiedMethodParamModelName = "is" + accessorMethodName + "()";
          } else {
            modifiedMethodParamModelName = "get" + accessorMethodName + "()";
          }
        } else {
          modifiedMethodParamModelName = methodParamModel.getName();
        }

        eventHandlerParams.add(
            ",\n($T) $L.$L",
            methodParamModelTypeName,
            eventVariableName,
            modifiedMethodParamModelName);
      } else if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)
          || methodParamModelTypeName.equals(mContextClass)) {
        eventHandlerParams.add(
            ",\n($T) eventHandler.params[$L]", methodParamModelTypeName, paramIndex++);
      }
    }

    eventHandlerParams.unindent();

    if (!eventMethodModel.returnType.equals(TypeName.VOID)) {
      methodBuilder.addStatement(
          "return $L($L)", eventMethodModel.name, eventHandlerParams.build());
    } else {
      methodBuilder.addStatement("$L($L)", eventMethodModel.name, eventHandlerParams.build());
      methodBuilder.addStatement("return null");
    }

    methodBuilder.endControlFlow();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ClassName mContextClass;
    private ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> mEventMethodModels;
    private boolean mWithErrorPropagation;

    private Builder() {}

    public Builder contextClass(ClassName contextClass) {
      mContextClass = contextClass;
      return this;
    }

    public Builder eventMethodModels(
        ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> eventMethodModels) {
      mEventMethodModels = eventMethodModels;
      return this;
    }

    public Builder withErrorPropagation(boolean withErrorPropagation) {
      mWithErrorPropagation = withErrorPropagation;
      return this;
    }

    public void writeTo(MethodSpec.Builder methodBuilder) {
      new EventCaseGenerator(mContextClass, mEventMethodModels, mWithErrorPropagation)
          .writeTo(methodBuilder);
    }
  }
}
