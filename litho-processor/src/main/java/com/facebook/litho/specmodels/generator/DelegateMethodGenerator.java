/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.ComponentImplGenerator.getImplAccessor;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.ABSTRACT_IMPL_PARAM_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.IMPL_VARIABLE_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.PREVIOUS_RENDER_DATA_FIELD_NAME;
import static com.facebook.litho.specmodels.model.ClassNames.COMPONENT;
import static com.facebook.litho.specmodels.model.ClassNames.OUTPUT;
import static com.facebook.litho.specmodels.model.ClassNames.STATE_VALUE;

import com.facebook.litho.specmodels.model.DelegateMethodDescription;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.DiffModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Class that generates delegate methods for a component.
 */
public class DelegateMethodGenerator {

  private DelegateMethodGenerator() {
  }

  /**
   * Generate all delegates defined on this {@link SpecModel}.
   */
  public static TypeSpecDataHolder generateDelegates(
      SpecModel specModel,
      Map<Class<? extends Annotation>, DelegateMethodDescription> delegateMethodsMap) {
    TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (DelegateMethodModel delegateMethodModel : specModel.getDelegateMethods()) {
      for (Annotation annotation : delegateMethodModel.annotations) {
        if (delegateMethodsMap.containsKey(annotation.annotationType())) {
          final DelegateMethodDescription delegateMethodDescription =
              delegateMethodsMap.get(annotation.annotationType());
          typeSpecDataHolder.addMethod(
              generateDelegate(
                  specModel,
                  delegateMethodDescription,
                  delegateMethodModel));
          for (MethodSpec methodSpec : delegateMethodDescription.extraMethods) {
            typeSpecDataHolder.addMethod(methodSpec);
          }
          break;
        }
      }
    }

    return typeSpecDataHolder.build();
  }

  /**
   * Generate a delegate to the Spec that defines this component.
   */
  private static MethodSpec generateDelegate(
      SpecModel specModel,
      DelegateMethodDescription methodDescription,
      DelegateMethodModel delegateMethod) {
    final MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(methodDescription.name)
        .addModifiers(methodDescription.accessType)
        .returns(methodDescription.returnType);

    for (AnnotationSpec annotation : methodDescription.annotations) {
      methodSpec.addAnnotation(annotation);
    }

    for (int i = 0, size = methodDescription.definedParameterTypes.size(); i < size; i++) {
      methodSpec.addParameter(
          methodDescription.definedParameterTypes.get(i),
          delegateMethod.methodParams.get(i).getName());
    }

    if (!methodDescription.optionalParameterTypes.isEmpty()) {
      methodSpec.addParameter(COMPONENT, ABSTRACT_IMPL_PARAM_NAME);
    }

    for (TypeName exception : methodDescription.exceptions) {
      methodSpec.addException(exception);
    }

    if (!methodDescription.optionalParameterTypes.isEmpty()) {
      final String implName = specModel.getComponentName() + "Impl";
      methodSpec.addStatement(
          implName + " " + IMPL_VARIABLE_NAME + " = (" + implName + ") " +
              ABSTRACT_IMPL_PARAM_NAME);
    }

    final CodeBlock.Builder acquireOutputs = CodeBlock.builder();
    final CodeBlock.Builder delegation = CodeBlock.builder();
    final CodeBlock.Builder releaseOutputs = CodeBlock.builder();

    final String sourceDelegateAccessor = SpecModelUtils.getSpecAccessor(specModel);
    if (methodDescription.returnType.equals(TypeName.VOID)) {
      delegation.add("$L.$L(\n", sourceDelegateAccessor, delegateMethod.name);
    } else {
      delegation.add(
          "$T _result = ($T) $L.$L(\n",
          methodDescription.returnType,
          methodDescription.returnType,
          sourceDelegateAccessor,
          delegateMethod.name);
    }

    delegation.indent();
    for (int i = 0, size = delegateMethod.methodParams.size(); i < size; i++) {
      final MethodParamModel methodParamModel = delegateMethod.methodParams.get(i);

      if (i < methodDescription.definedParameterTypes.size()) {
        delegation.add("($T) $L", methodParamModel.getType(), methodParamModel.getName());
      } else if (isOutputType(methodParamModel.getType())) {
        acquireOutputs.add(
            "$T $L = acquireOutput();\n", methodParamModel.getType(), methodParamModel.getName());
        delegation.add("$L", methodParamModel.getName());

        final boolean isPropOutput = SpecModelUtils.isPropOutput(specModel, methodParamModel);
        if (isPropOutput) {
          releaseOutputs.beginControlFlow("if ($L.get() != null)", methodParamModel.getName());
        }
        releaseOutputs.addStatement(
            "$L.$L = $L.get()",
            IMPL_VARIABLE_NAME,
            getImplAccessor(specModel, methodParamModel),
            methodParamModel.getName());
        if (isPropOutput) {
          releaseOutputs.endControlFlow();
        }
        releaseOutputs.addStatement("releaseOutput($L)", methodParamModel.getName());
      } else if (isStateValueType(methodParamModel.getType())) {
        acquireOutputs.add(
            "$T $L = new StateValue<>();\n",
            methodParamModel.getType(),
            methodParamModel.getName());
        delegation.add("$L", methodParamModel.getName());

        releaseOutputs.addStatement(
            "$L.$L = $L.get()",
            IMPL_VARIABLE_NAME,
            getImplAccessor(specModel, methodParamModel),
            methodParamModel.getName());
      } else if (methodParamModel instanceof DiffModel
          && ((DiffModel) methodParamModel).needsRenderDataInfra()) {
        final String diffName = "_" + methodParamModel.getName() + "Diff";
        CodeBlock block =
            CodeBlock.builder()
                .add("$T $L = acquireDiff(\n", methodParamModel.getType(), diffName)
                .indent()
                .add(
                    "$L.$L == null ? null : $L.$L.$L,\n",
                    IMPL_VARIABLE_NAME,
                    PREVIOUS_RENDER_DATA_FIELD_NAME,
                    IMPL_VARIABLE_NAME,
                    PREVIOUS_RENDER_DATA_FIELD_NAME,
                    methodParamModel.getName())
                .add("$L.$L);\n", IMPL_VARIABLE_NAME, getImplAccessor(specModel, methodParamModel))
                .unindent()
                .build();
        methodSpec.addCode(block);
        releaseOutputs.addStatement("releaseDiff($L)", diffName);
        delegation.add("$L", diffName);
      } else {
        delegation.add(
            "($T) $L.$L",
            methodParamModel.getType(),
            IMPL_VARIABLE_NAME,
            getImplAccessor(specModel, methodParamModel));
      }

      if (i < delegateMethod.methodParams.size() - 1) {
        delegation.add(",\n");
      } else {
        delegation.add(");\n");
      }
    }

    delegation.unindent();

    methodSpec.addCode(acquireOutputs.build());
    methodSpec.addCode(delegation.build());
    methodSpec.addCode(releaseOutputs.build());

    if (!methodDescription.returnType.equals(TypeName.VOID)) {
      methodSpec.addStatement("return _result");
    }

    return methodSpec.build();
  }

  private static boolean isOutputType(TypeName type) {
    return type.equals(OUTPUT) ||
        (type instanceof ParameterizedTypeName &&
         ((ParameterizedTypeName) type).rawType.equals(OUTPUT));
  }

  private static boolean isStateValueType(TypeName type) {
    return type.equals(STATE_VALUE) ||
        (type instanceof ParameterizedTypeName &&
            ((ParameterizedTypeName) type).rawType.equals(STATE_VALUE));
  }
}
