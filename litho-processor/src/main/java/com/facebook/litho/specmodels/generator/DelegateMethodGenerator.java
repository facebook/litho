/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DelegateMethodDescription;
import com.facebook.litho.specmodels.model.DiffPropModel;
import com.facebook.litho.specmodels.model.DiffStateParamModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.RenderDataDiffModel;
import com.facebook.litho.specmodels.model.SimpleMethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.lang.annotation.Annotation;
import java.util.Map;

import static com.facebook.litho.specmodels.generator.ComponentBodyGenerator.getImplAccessor;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.PREVIOUS_RENDER_DATA_FIELD_NAME;
import static com.facebook.litho.specmodels.model.ClassNames.OUTPUT;
import static com.facebook.litho.specmodels.model.ClassNames.STATE_VALUE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_STATE;

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
    for (SpecMethodModel<DelegateMethod, Void> delegateMethodModel : specModel.getDelegateMethods()) {
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
      SpecMethodModel<DelegateMethod, Void> delegateMethod) {
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
    final String componentName = specModel.getComponentName();

    for (TypeName exception : methodDescription.exceptions) {
      methodSpec.addException(exception);
    }

    if (methodUsesDiffs) {
      methodSpec.addParameter(specModel.getComponentClass(), "_prevAbstractImpl");
      methodSpec.addParameter(specModel.getComponentClass(), "_nextAbstractImpl");
      methodSpec.addStatement(
          "$L _prevImpl = ($L) _prevAbstractImpl",
          componentName,
          componentName);
      methodSpec.addStatement(
          "$L _nextImpl = ($L) _nextAbstractImpl",
          componentName,
          componentName);
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
        delegation.add("($T) $L", methodParamModel.getTypeName(), methodParamModel.getName());
      } else if (i < definedParameterTypesSize + methodDescription.optionalParameters.size()
          && shouldIncludeOptionalParameter(
              methodParamModel,
              methodDescription.optionalParameters.get(i - definedParameterTypesSize))) {
        final MethodParamModel extraDefinedParam =
            methodDescription.optionalParameters.get(i - definedParameterTypesSize);
        delegation.add("$L", extraDefinedParam.getName());
      } else if (methodParamModel instanceof DiffPropModel
          || methodParamModel instanceof DiffStateParamModel) {
        acquireStatements.addStatement(
            "$T $L = ($T) acquireDiff(_prevImpl == null ? null : _prevImpl.$L, "
                + "_nextImpl == null ? null : _nextImpl.$L)",
            methodParamModel.getTypeName(),
            methodParamModel.getName(),
            ClassNames.DIFF,
            ComponentBodyGenerator.getImplAccessor(specModel, methodParamModel),
            ComponentBodyGenerator.getImplAccessor(specModel, methodParamModel));
        delegation.add("$L", methodParamModel.getName());
        releaseStatements.addStatement("releaseDiff($L)", methodParamModel.getName());
      } else if (isOutputType(methodParamModel.getTypeName())) {
        String localOutputName = methodParamModel.getName() + "Tmp";
        acquireStatements.add(
            "$T $L = acquireOutput();\n", methodParamModel.getTypeName(), localOutputName);
        delegation.add("$L", localOutputName);

        final boolean isPropOutput = SpecModelUtils.isPropOutput(specModel, methodParamModel);
        if (isPropOutput) {
          releaseStatements.beginControlFlow("if ($L.get() != null)", localOutputName);
        }
        releaseStatements.addStatement(
            "$L = $L.get()",
            getImplAccessor(specModel, methodParamModel),
            localOutputName);
        if (isPropOutput) {
          releaseStatements.endControlFlow();
        }
        releaseStatements.addStatement("releaseOutput($L)", localOutputName);
      } else if (isStateValueType(methodParamModel.getTypeName())) {
        acquireStatements.add(
            "$T $L = new StateValue<>();\n",
            methodParamModel.getTypeName(),
            methodParamModel.getName());

        delegation.add("$L", methodParamModel.getName());

        if (delegateMethod.name.toString().equals("createInitialState")) {
          releaseStatements.beginControlFlow("if ($L.get() != null)", methodParamModel.getName());
        }

        releaseStatements.addStatement(
            "$L = $L.get()",
            getImplAccessor(specModel, methodParamModel),
            methodParamModel.getName());

        if (delegateMethod.name.toString().equals("createInitialState")) {
          releaseStatements.endControlFlow();
        }
        
      } else if (methodParamModel instanceof RenderDataDiffModel) {
        final String diffName = "_" + methodParamModel.getName() + "Diff";
        CodeBlock block =
            CodeBlock.builder()
                .add("$T $L = acquireDiff(\n", methodParamModel.getTypeName(), diffName)
                .indent()
                .add(
                    "$L == null ? null : $L.$L,\n",
                    PREVIOUS_RENDER_DATA_FIELD_NAME,
                    PREVIOUS_RENDER_DATA_FIELD_NAME,
                    methodParamModel.getName())
                .add("$L);\n", getImplAccessor(specModel, methodParamModel))
                .unindent()
                .build();
        methodSpec.addCode(block);
        releaseStatements.addStatement("releaseDiff($L)", diffName);
        delegation.add("$L", diffName);
      } else {
        delegation.add(
            "($T) $L",
            methodParamModel.getTypeName(),
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
        && methodParamModel.getTypeName().equals(extraOptionalParameter.getTypeName())
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
