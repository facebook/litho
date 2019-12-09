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

import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/** Class that generates the cached value methods for a Component. */
public class CachedValueGenerator {

  private CachedValueGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel, EnumSet<RunMode> runMode) {
    final List<SpecMethodModel<DelegateMethod, Void>> onCalculateCachedValueMethods =
        SpecModelUtils.getMethodModelsWithAnnotation(specModel, OnCalculateCachedValue.class);

    if (onCalculateCachedValueMethods.isEmpty()) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    final TypeSpecDataHolder.Builder builder = TypeSpecDataHolder.newBuilder();
    for (SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod :
        onCalculateCachedValueMethods) {
      builder.addTypeSpecDataHolder(
          generateCachedValueMethodsAndClasses(specModel, onCalculateCachedValueMethod, runMode));
    }

    return builder.build();
  }

  private static TypeSpecDataHolder generateCachedValueMethodsAndClasses(
      SpecModel specModel,
      SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod,
      EnumSet<RunMode> runMode) {
    TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    String cachedValueName = getAnnotatedName(onCalculateCachedValueMethod);

    typeSpecDataHolder.addMethod(
        createGetterMethod(specModel, onCalculateCachedValueMethod, cachedValueName));
    typeSpecDataHolder.addType(
        createInputsClass(onCalculateCachedValueMethod, cachedValueName, runMode));

    return typeSpecDataHolder.build();
  }

  private static String getAnnotatedName(
      SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod) {
    for (Annotation annotation : onCalculateCachedValueMethod.annotations) {
      if (annotation instanceof OnCalculateCachedValue) {
        return ((OnCalculateCachedValue) annotation).name();
      }
    }

    throw new RuntimeException("Should be unreachable, please report to Litho team");
  }

  public static MethodSpec createGetterMethod(
      SpecModel specModel,
      SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod,
      String cachedValueName) {
    final TypeName cachedValueType = onCalculateCachedValueMethod.returnType;
    MethodSpec.Builder methodSpec =
        MethodSpec.methodBuilder(getCachedValueGetterName(cachedValueName))
            .addModifiers(Modifier.PRIVATE)
            .returns(cachedValueType)
            .addStatement("$T c = getScopedContext()", specModel.getContextClass());

    final CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock
        .add(
            "final $L inputs = new $L(",
            getInputsClassName(cachedValueName),
            getInputsClassName(cachedValueName))
        .indent();

    // Skip the ComponentContext param from the input class creation since the context can change
    // during the lifetime of the cache.
    List<MethodParamModel> onCalculateCachedValueMethodParams =
        onCalculateCachedValueMethod.methodParams.stream()
            .filter(
                methodParamModel ->
                    !MethodParamModelUtils.isComponentContextParam(methodParamModel))
            .collect(Collectors.toList());

    final int filteredParamSize = onCalculateCachedValueMethodParams.size();

    for (int i = 0; i < filteredParamSize; i++) {
      MethodParamModel methodParamModel = onCalculateCachedValueMethodParams.get(i);
      codeBlock.add("$L", ComponentBodyGenerator.getImplAccessor(specModel, methodParamModel));
      if (i < filteredParamSize - 1) {
        codeBlock.add(",");
      }
    }
    codeBlock.add(");\n");
    codeBlock.unindent();

    final int paramSize = onCalculateCachedValueMethod.methodParams.size();

    methodSpec
        .addCode(codeBlock.build())
        .addStatement(
            "$T $L = ($T) c.getCachedValue(inputs)",
            cachedValueType.box(),
            cachedValueName,
            cachedValueType.box())
        .beginControlFlow("if ($L == null)", cachedValueName);

    final CodeBlock.Builder delegation = CodeBlock.builder();
    delegation
        .add(
            "$L = $L.$L(",
            cachedValueName,
            specModel.getSpecName(),
            onCalculateCachedValueMethod.name)
        .indent();
    if (paramSize == 0) {
      delegation.add(");\n");
    } else {
      for (int i = 0; i < paramSize; i++) {
        MethodParamModel methodParamModel = onCalculateCachedValueMethod.methodParams.get(i);
        String argName;
        if (MethodParamModelUtils.isComponentContextParam(methodParamModel)) {
          argName = "c";
        } else {
          argName = ComponentBodyGenerator.getImplAccessor(specModel, methodParamModel);
        }
        if (i < paramSize - 1) {
          delegation.add("$L,", argName);
        } else {
          delegation.add("$L);\n", argName);
        }
      }
    }

    methodSpec
        .addCode(delegation.unindent().build())
        .addStatement("c.putCachedValue(inputs, $L)", cachedValueName)
        .endControlFlow()
        .addStatement("return $L", cachedValueName);

    return methodSpec.build();
  }

  public static TypeSpec createInputsClass(
      SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod,
      String cachedValueName,
      EnumSet<RunMode> runMode) {
    TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(getInputsClassName(cachedValueName))
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

    // Skip the ComponentContext param from the input class creation since the context can change
    // during the lifetime of the cache.
    List<MethodParamModel> onCalculateCachedValueMethodParams =
        onCalculateCachedValueMethod.methodParams.stream()
            .filter(
                methodParamModel ->
                    !MethodParamModelUtils.isComponentContextParam(methodParamModel))
            .collect(Collectors.toList());

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder();

    final Set<TypeVariableName> typeVariables = new HashSet<>();

    for (MethodParamModel param : onCalculateCachedValueMethodParams) {
      typeVariables.addAll(MethodParamModelUtils.getTypeVariables(param));
      typeSpec.addField(
          FieldSpec.builder(param.getTypeName(), param.getName(), Modifier.PRIVATE, Modifier.FINAL)
              .build());
      constructor
          .addParameter(ParameterSpec.builder(param.getTypeName(), param.getName()).build())
          .addStatement("this.$L = $L", param.getName(), param.getName());
    }

    typeSpec.addTypeVariables(typeVariables);
    typeSpec.addMethod(constructor.build());

    final int paramSize = onCalculateCachedValueMethodParams.size();
    MethodSpec.Builder hashCodeMethod =
        MethodSpec.methodBuilder("hashCode")
            .addAnnotation(Override.class)
            .returns(TypeName.INT)
            .addModifiers(Modifier.PUBLIC);

    CodeBlock.Builder codeBlock =
        CodeBlock.builder().add("return $T.hash(", ClassNames.COMMON_UTILS);
    if (paramSize > 0) {
      for (int i = 0; i < paramSize; i++) {
        if (i < paramSize - 1) {
          codeBlock.add("$L, ", onCalculateCachedValueMethodParams.get(i).getName());
        } else {
          codeBlock.add("$L);\n", onCalculateCachedValueMethodParams.get(i).getName());
        }
      }
    } else {
      codeBlock.add("getClass());\n");
    }
    hashCodeMethod.addCode(codeBlock.build());
    typeSpec.addMethod(hashCodeMethod.build());

    MethodSpec.Builder equalsMethod =
        MethodSpec.methodBuilder("equals")
            .addAnnotation(Override.class)
            .returns(TypeName.BOOLEAN)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ClassNames.OBJECT, "other")
            .beginControlFlow("if (this == other)")
            .addStatement("return true")
            .endControlFlow()
            .beginControlFlow(
                "if (other == null || !(other instanceof $L))", getInputsClassName(cachedValueName))
            .addStatement("return false")
            .endControlFlow()
            .addStatement(
                "$L cachedValueInputs = ($L) other",
                getInputsClassName(cachedValueName),
                getInputsClassName(cachedValueName));

    for (MethodParamModel methodParamModel : onCalculateCachedValueMethodParams) {
      equalsMethod.addCode(
          ComponentBodyGenerator.getCompareStatement(
              methodParamModel,
              methodParamModel.getName(),
              "cachedValueInputs." + methodParamModel.getName(),
              runMode));
    }

    equalsMethod.addStatement("return true");

    typeSpec.addMethod(equalsMethod.build());

    return typeSpec.build();
  }

  private static String getInputsClassName(CharSequence cachedValueName) {
    return toUpperCaseFirstLetter(cachedValueName) + "Inputs";
  }

  private static String getCachedValueGetterName(CharSequence cachedValueName) {
    return "get" + toUpperCaseFirstLetter(cachedValueName);
  }

  private static String toUpperCaseFirstLetter(CharSequence name) {
    return name.toString().substring(0, 1).toUpperCase() + name.toString().substring(1);
  }
}
