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

import static com.facebook.litho.specmodels.generator.ComponentBodyGenerator.getImplAccessor;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.PREVIOUS_RENDER_DATA_FIELD_NAME;
import static com.facebook.litho.specmodels.model.ClassNames.OUTPUT;
import static com.facebook.litho.specmodels.model.ClassNames.STATE_VALUE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_STATE;

import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DelegateMethodDescription;
import com.facebook.litho.specmodels.model.DiffPropModel;
import com.facebook.litho.specmodels.model.DiffStateParamModel;
import com.facebook.litho.specmodels.model.EventMethod;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;

/** Class that generates delegate methods for a component. */
public class DelegateMethodGenerator {
  private DelegateMethodGenerator() {}

  /** Generate all delegates defined on this {@link SpecModel}. */
  public static TypeSpecDataHolder generateDelegates(
      SpecModel specModel,
      Map<Class<? extends Annotation>, DelegateMethodDescription> delegateMethodsMap,
      EnumSet<RunMode> runMode) {
    TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    boolean hasAttachDetachCallback = false;
    for (SpecMethodModel<DelegateMethod, Void> delegateMethodModel :
        specModel.getDelegateMethods()) {
      for (Annotation annotation : delegateMethodModel.annotations) {
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        if (annotationType.equals(OnAttached.class) || annotationType.equals(OnDetached.class)) {
          hasAttachDetachCallback = true;
        }
        if (delegateMethodsMap.containsKey(annotation.annotationType())) {
          final DelegateMethodDescription delegateMethodDescription =
              delegateMethodsMap.get(annotation.annotationType());
          typeSpecDataHolder.addMethod(
              generateDelegate(specModel, delegateMethodDescription, delegateMethodModel, runMode));
          for (MethodSpec methodSpec : delegateMethodDescription.extraMethods) {
            typeSpecDataHolder.addMethod(methodSpec);
          }
          break;
        }
      }
    }

    if (hasAttachDetachCallback) {
      typeSpecDataHolder.addMethod(generateHasAttachDetachCallback());
    }

    return typeSpecDataHolder.build();
  }

  /** Generate a delegate to the Spec that defines this component. */
  private static MethodSpec generateDelegate(
      SpecModel specModel,
      DelegateMethodDescription methodDescription,
      SpecMethodModel<DelegateMethod, Void> delegateMethod,
      EnumSet<RunMode> runMode) {
    final MethodSpec.Builder methodSpec =
        MethodSpec.methodBuilder(methodDescription.name)
            .addModifiers(methodDescription.accessType)
            .returns(methodDescription.returnType);

    for (AnnotationSpec annotation : methodDescription.annotations) {
      methodSpec.addAnnotation(annotation);
    }

    String contextParamName = null;

    for (int i = 0, size = methodDescription.lifecycleMethodArgumentTypes.size(); i < size; i++) {
      if (methodDescription.lifecycleMethodArgumentTypes.get(i).type
          == specModel.getContextClass()) {
        contextParamName = delegateMethod.methodParams.get(i).getName();
      }

      methodSpec.addParameter(
          methodDescription.lifecycleMethodArgumentTypes.get(i).type,
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
      methodSpec.addParameter(specModel.getContextClass(), "_prevScopedContext");
      methodSpec.addParameter(specModel.getComponentClass(), "_prevAbstractImpl");
      methodSpec.addParameter(specModel.getContextClass(), "_nextScopedContext");
      methodSpec.addParameter(specModel.getComponentClass(), "_nextAbstractImpl");
      methodSpec.addStatement(
          "$L _prevImpl = ($L) _prevAbstractImpl", componentName, componentName);
      methodSpec.addStatement(
          "$L _nextImpl = ($L) _nextAbstractImpl", componentName, componentName);
    }

    if (!methodDescription.returnType.equals(TypeName.VOID)) {
      methodSpec.addStatement("$T _result", methodDescription.returnType);
    }

    methodSpec.addCode(getDelegationCode(specModel, delegateMethod, methodDescription, runMode));

    if (delegateMethod.name.toString().equals("onCreateLayout")
        || delegateMethod.name.toString().equals("onPrepare")) {
      SpecMethodModel<EventMethod, Void> registerRangesModel =
          specModel.getWorkingRangeRegisterMethod();

      if (registerRangesModel != null) {
        CodeBlock.Builder registerDelegation =
            CodeBlock.builder()
                .add(
                    "$L.$L(\n",
                    SpecModelUtils.getSpecAccessor(specModel),
                    registerRangesModel.name);

        registerDelegation.indent();
        for (int i = 0, size = registerRangesModel.methodParams.size(); i < size; i++) {
          final MethodParamModel methodParamModel = registerRangesModel.methodParams.get(i);
          registerDelegation.add(
              "($T) $L",
              methodParamModel.getTypeName(),
              getImplAccessor(
                  methodDescription.name, specModel, methodParamModel, contextParamName));
          registerDelegation.add(
              (i < registerRangesModel.methodParams.size() - 1) ? ",\n" : ");\n");
        }
        registerDelegation.unindent();
        methodSpec.addCode(registerDelegation.build());
      }
    }

    if (!methodDescription.returnType.equals(TypeName.VOID)) {
      methodSpec.addStatement("return _result");
    }

    return methodSpec.build();
  }

  private static String getContextParamName(
      SpecModel specModel,
      SpecMethodModel<DelegateMethod, Void> delegateMethod,
      DelegateMethodDescription methodDescription) {
    for (int i = 0, size = methodDescription.lifecycleMethodArgumentTypes.size(); i < size; i++) {
      if (methodDescription.lifecycleMethodArgumentTypes.get(i).type
          == specModel.getContextClass()) {
        return delegateMethod.methodParams.get(i).getName();
      }
    }

    return null;
  }

  public static CodeBlock getDelegationCode(
      SpecModel specModel,
      SpecMethodModel<DelegateMethod, Void> delegateMethod,
      DelegateMethodDescription methodDescription,
      EnumSet<RunMode> runMode) {
    final CodeBlock.Builder acquireStatements = CodeBlock.builder();
    final CodeBlock.Builder releaseStatements = CodeBlock.builder();

    final List<ParamTypeAndName> delegationParams =
        new ArrayList<>(delegateMethod.methodParams.size());

    final String contextParamName =
        getContextParamName(specModel, delegateMethod, methodDescription);

    for (int i = 0, size = delegateMethod.methodParams.size(); i < size; i++) {
      final MethodParamModel methodParamModel = delegateMethod.methodParams.get(i);
      final int definedParameterTypesSize = methodDescription.lifecycleMethodArgumentTypes.size();
      if (i < definedParameterTypesSize) {
        delegationParams.add(
            ParamTypeAndName.create(methodParamModel.getTypeName(), methodParamModel.getName()));
      } else if (i < definedParameterTypesSize + methodDescription.optionalParameters.size()
          && shouldIncludeOptionalParameter(
              methodParamModel,
              methodDescription.optionalParameters.get(i - definedParameterTypesSize))) {
        final MethodParamModel extraDefinedParam =
            methodDescription.optionalParameters.get(i - definedParameterTypesSize);
        delegationParams.add(
            ParamTypeAndName.create(extraDefinedParam.getTypeName(), extraDefinedParam.getName()));
      } else if (methodParamModel instanceof DiffPropModel
          || methodParamModel instanceof DiffStateParamModel) {
        acquireStatements.addStatement(
            // Diff<type> name = new Diff<type>(...)
            "$T $L = new $T(_prevImpl == null ? null : _prevImpl.$L, "
                + "_nextImpl == null ? null : _nextImpl.$L)",
            methodParamModel.getTypeName(),
            methodParamModel.getName(),
            methodParamModel.getTypeName(),
            ComponentBodyGenerator.getImplAccessor(
                methodDescription.name, specModel, methodParamModel, "_prevScopedContext"),
            ComponentBodyGenerator.getImplAccessor(
                methodDescription.name, specModel, methodParamModel, "_nextScopedContext"));
        delegationParams.add(
            ParamTypeAndName.create(methodParamModel.getTypeName(), methodParamModel.getName()));
      } else if (isOutputType(methodParamModel.getTypeName())) {
        String localOutputName = methodParamModel.getName() + "Tmp";
        acquireStatements.add(
            "$T $L = new Output<>();\n", methodParamModel.getTypeName(), localOutputName);
        delegationParams.add(
            ParamTypeAndName.create(methodParamModel.getTypeName(), localOutputName));

        final boolean isPropOutput = SpecModelUtils.isPropOutput(specModel, methodParamModel);
        if (isPropOutput) {
          releaseStatements.beginControlFlow("if ($L.get() != null)", localOutputName);
        }
        releaseStatements.addStatement(
            "$L = $L.get()",
            getImplAccessor(methodDescription.name, specModel, methodParamModel, contextParamName),
            localOutputName);
        if (isPropOutput) {
          releaseStatements.endControlFlow();
        }
      } else if (isStateValueType(methodParamModel.getTypeName())) {
        acquireStatements.add(
            "$T $L = new StateValue<>();\n",
            methodParamModel.getTypeName(),
            methodParamModel.getName());
        delegationParams.add(
            ParamTypeAndName.create(methodParamModel.getTypeName(), methodParamModel.getName()));

        if (delegateMethod.name.toString().equals("createInitialState")) {
          releaseStatements.beginControlFlow("if ($L.get() != null)", methodParamModel.getName());
        }

        releaseStatements.addStatement(
            "$L = $L.get()",
            getImplAccessor(methodDescription.name, specModel, methodParamModel, contextParamName),
            methodParamModel.getName());

        if (delegateMethod.name.toString().equals("createInitialState")) {
          releaseStatements.endControlFlow();
        }

      } else if (methodParamModel instanceof RenderDataDiffModel) {
        final String diffName = "_" + methodParamModel.getName() + "Diff";
        CodeBlock block =
            CodeBlock.builder()
                // Diff<type> name = new Diff<type>(...)
                .add(
                    "$T $L = new $T(\n",
                    methodParamModel.getTypeName(),
                    diffName,
                    methodParamModel.getTypeName())
                .indent()
                .add(
                    "$L == null ? null : $L.$L,\n",
                    PREVIOUS_RENDER_DATA_FIELD_NAME,
                    PREVIOUS_RENDER_DATA_FIELD_NAME,
                    methodParamModel.getName())
                .add(
                    "$L);\n",
                    getImplAccessor(
                        methodDescription.name, specModel, methodParamModel, contextParamName))
                .unindent()
                .build();
        acquireStatements.add(block);
        delegationParams.add(ParamTypeAndName.create(methodParamModel.getTypeName(), diffName));
      } else {
        delegationParams.add(
            ParamTypeAndName.create(
                methodParamModel.getTypeName(),
                getImplAccessor(
                    methodDescription.name, specModel, methodParamModel, contextParamName)));
      }
    }

    return CodeBlock.builder()
        .add(acquireStatements.build())
        .add(
            getDelegationMethod(
                specModel,
                delegateMethod.name,
                methodDescription.returnType,
                ImmutableList.copyOf(delegationParams)))
        .add(releaseStatements.build())
        .build();
  }

  private static CodeBlock getDelegationMethod(
      SpecModel specModel,
      CharSequence methodName,
      TypeName returnType,
      ImmutableList<ParamTypeAndName> methodParams) {
    final CodeBlock.Builder delegation = CodeBlock.builder();
    final String sourceDelegateAccessor = SpecModelUtils.getSpecAccessor(specModel);
    if (returnType.equals(TypeName.VOID)) {
      delegation.add("$L.$L(\n", sourceDelegateAccessor, methodName);
    } else {
      delegation.add("_result = ($T) $L.$L(\n", returnType, sourceDelegateAccessor, methodName);
    }

    delegation.indent();
    for (int i = 0; i < methodParams.size(); i++) {
      delegation.add("($T) $L", methodParams.get(i).type, methodParams.get(i).name);

      if (i < methodParams.size() - 1) {
        delegation.add(",\n");
      }
    }

    delegation.add(");\n");
    delegation.unindent();

    return delegation.build();
  }

  /** Override hasAttachDetachCallback() method and return true. */
  private static MethodSpec generateHasAttachDetachCallback() {
    final MethodSpec.Builder methodSpec =
        MethodSpec.methodBuilder("hasAttachDetachCallback")
            .addModifiers(Modifier.PROTECTED)
            .addAnnotation(Override.class)
            .returns(TypeName.BOOLEAN);
    methodSpec.addStatement("return true");
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
    return type.equals(OUTPUT)
        || (type instanceof ParameterizedTypeName
            && ((ParameterizedTypeName) type).rawType.equals(OUTPUT));
  }

  private static boolean isStateValueType(TypeName type) {
    return type.equals(STATE_VALUE)
        || (type instanceof ParameterizedTypeName
            && ((ParameterizedTypeName) type).rawType.equals(STATE_VALUE));
  }

  static final class ParamTypeAndName {
    final TypeName type;
    final String name;

    private ParamTypeAndName(TypeName type, String name) {
      this.type = type;
      this.name = name;
    }

    static ParamTypeAndName create(TypeName type, String name) {
      return new ParamTypeAndName(type, name);
    }
  }
}
