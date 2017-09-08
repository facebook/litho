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
import static com.facebook.litho.specmodels.model.ClassNames.OUTPUT;
import static com.facebook.litho.specmodels.model.ClassNames.STATE_VALUE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_STATE;

import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethodDescription;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.DiffPropModel;
import com.facebook.litho.specmodels.model.DiffStateParamModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.RenderDataDiffModel;
import com.facebook.litho.specmodels.model.SimpleMethodParamModel;
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

    final boolean methodUsesDiffs =
        methodDescription.optionalParameterTypes.contains(DIFF_PROP)
            || methodDescription.optionalParameterTypes.contains(DIFF_STATE);
    final boolean shouldIncludeImpl =
        (!methodDescription.optionalParameterTypes.isEmpty() && !methodUsesDiffs) ||
            !methodDescription.optionalParameters.isEmpty();
    if (shouldIncludeImpl) {
      methodSpec.addParameter(specModel.getComponentClass(), ABSTRACT_IMPL_PARAM_NAME);
      final String implName = ComponentImplGenerator.getImplClassName(specModel);
      methodSpec.addStatement(
          implName
              + " "
              + IMPL_VARIABLE_NAME
              + " = ("
              + implName
              + ") "
              + ABSTRACT_IMPL_PARAM_NAME);
    }

    for (TypeName exception : methodDescription.exceptions) {
      methodSpec.addException(exception);
    }

    if (methodUsesDiffs) {
      methodSpec.addParameter(specModel.getComponentClass(), "_prevAbstractImpl");
      methodSpec.addParameter(specModel.getComponentClass(), "_nextAbstractImpl");
      methodSpec.addStatement(
          "$L _prevImpl = ($L) _prevAbstractImpl",
          ComponentImplGenerator.getImplClassName(specModel),
          ComponentImplGenerator.getImplClassName(specModel));
      methodSpec.addStatement(
          "$L _nextImpl = ($L) _nextAbstractImpl",
          ComponentImplGenerator.getImplClassName(specModel),
          ComponentImplGenerator.getImplClassName(specModel));
    }

    final CodeBlock.Builder acquireStatements = CodeBlock.builder();
    final CodeBlock.Builder delegation = CodeBlock.builder();
    final CodeBlock.Builder releaseStatements = CodeBlock.builder();

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
      final int definedParameterTypesSize = methodDescription.definedParameterTypes.size();
      if (i < definedParameterTypesSize) {
        delegation.add("($T) $L", methodParamModel.getType(), methodParamModel.getName());
      } else if (i < definedParameterTypesSize + methodDescription.optionalParameters.size()
          && shouldIncludeOptionalParameter(
              methodParamModel,
              methodDescription.optionalParameters.get(i - definedParameterTypesSize))) {
        final MethodParamModel extraDefinedParam =
            methodDescription.optionalParameters.get(i - definedParameterTypesSize);
        delegation.add(
            "($T) $L.$L",
            extraDefinedParam.getType(),
            IMPL_VARIABLE_NAME,
            extraDefinedParam.getName());
      } else if (methodParamModel instanceof DiffPropModel
          || methodParamModel instanceof DiffStateParamModel) {
        acquireStatements.addStatement(
            "$T $L = ($T) acquireDiff(_prevImpl == null ? null : _prevImpl.$L, "
                + "_nextImpl == null ? null : _nextImpl.$L)",
            methodParamModel.getType(),
            methodParamModel.getName(),
            ClassNames.DIFF,
            ComponentImplGenerator.getImplAccessor(specModel, methodParamModel),
            ComponentImplGenerator.getImplAccessor(specModel, methodParamModel));
        delegation.add("$L", methodParamModel.getName());
        releaseStatements.addStatement("releaseDiff($L)", methodParamModel.getName());
      } else if (isOutputType(methodParamModel.getType())) {
        acquireStatements.add(
            "$T $L = acquireOutput();\n", methodParamModel.getType(), methodParamModel.getName());
        delegation.add("$L", methodParamModel.getName());

        final boolean isPropOutput = SpecModelUtils.isPropOutput(specModel, methodParamModel);
        if (isPropOutput) {
          releaseStatements.beginControlFlow("if ($L.get() != null)", methodParamModel.getName());
        }
        releaseStatements.addStatement(
            "$L.$L = $L.get()",
            IMPL_VARIABLE_NAME,
            getImplAccessor(specModel, methodParamModel),
            methodParamModel.getName());
        if (isPropOutput) {
          releaseStatements.endControlFlow();
        }
        releaseStatements.addStatement("releaseOutput($L)", methodParamModel.getName());
      } else if (isStateValueType(methodParamModel.getType())) {
        acquireStatements.add(
            "$T $L = new StateValue<>();\n",
            methodParamModel.getType(),
            methodParamModel.getName());
        delegation.add("$L", methodParamModel.getName());

        releaseStatements.addStatement(
            "$L.$L = $L.get()",
            IMPL_VARIABLE_NAME,
            getImplAccessor(specModel, methodParamModel),
            methodParamModel.getName());
      } else if (methodParamModel instanceof RenderDataDiffModel) {
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
        releaseStatements.addStatement("releaseDiff($L)", diffName);
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
      }
    }

    delegation.add(");\n");
    delegation.unindent();

    methodSpec.addCode(acquireStatements.build());
    methodSpec.addCode(delegation.build());
    methodSpec.addCode(releaseStatements.build());

    if (!methodDescription.returnType.equals(TypeName.VOID)) {
      methodSpec.addStatement("return _result");
    }

    return methodSpec.build();
  }

  /**
   * We consider an optional parameter as something that comes immediately after defined parameters
   * and is not a special litho parameter (like a prop, state, etc...). This method verifies that
   * optional parameters are the right type and have no additional annotations.
   */
  private static boolean shouldIncludeOptionalParameter(
      MethodParamModel methodParamModel, MethodParamModel extraOptionalParameter) {
    return methodParamModel instanceof SimpleMethodParamModel
        && methodParamModel.getType().equals(extraOptionalParameter.getType())
        && methodParamModel.getAnnotations().isEmpty();
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
