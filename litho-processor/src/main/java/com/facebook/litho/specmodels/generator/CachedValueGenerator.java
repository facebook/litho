/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.Modifier;

/** Class that generates the cached value methods for a Component. */
public class CachedValueGenerator {

  private CachedValueGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final List<SpecMethodModel<DelegateMethod, Void>> onCalculateCachedValueMethods =
        SpecModelUtils.getMethodModelsWithAnnotation(specModel, OnCalculateCachedValue.class);

    if (onCalculateCachedValueMethods.isEmpty()) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    final TypeSpecDataHolder.Builder builder = TypeSpecDataHolder.newBuilder();
    for (SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod :
        onCalculateCachedValueMethods) {
      builder.addTypeSpecDataHolder(
          generateCachedValueMethodsAndClasses(specModel, onCalculateCachedValueMethod));
    }

    return builder.build();
  }

  private static TypeSpecDataHolder generateCachedValueMethodsAndClasses(
      SpecModel specModel, SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod) {
    TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    String cachedValueName = getAnnotatedName(onCalculateCachedValueMethod);

    typeSpecDataHolder.addMethod(
        createGetterMethod(specModel, onCalculateCachedValueMethod, cachedValueName));
    typeSpecDataHolder.addType(
        createInputsClass(specModel, onCalculateCachedValueMethod, cachedValueName));

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

  private static MethodSpec createGetterMethod(
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

    for (int i = 0, size = onCalculateCachedValueMethod.methodParams.size(); i < size; i++) {
      if (i < size - 1) {
        codeBlock.add(
            "$L,",
            ComponentBodyGenerator.getImplAccessor(
                specModel, onCalculateCachedValueMethod.methodParams.get(i)));
      } else {
        codeBlock.add(
            "$L);\n",
            ComponentBodyGenerator.getImplAccessor(
                specModel, onCalculateCachedValueMethod.methodParams.get(i)));
      }
    }

    codeBlock.unindent();

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
    for (int i = 0, size = onCalculateCachedValueMethod.methodParams.size(); i < size; i++) {
      if (i < size - 1) {
        delegation.add(
            "$L,",
            ComponentBodyGenerator.getImplAccessor(
                specModel, onCalculateCachedValueMethod.methodParams.get(i)));
      } else {
        delegation.add(
            "$L);\n",
            ComponentBodyGenerator.getImplAccessor(
                specModel, onCalculateCachedValueMethod.methodParams.get(i)));
      }
    }

    methodSpec
        .addCode(delegation.unindent().build())
        .addStatement("c.putCachedValue(inputs, $L)", cachedValueName)
        .endControlFlow()
        .addStatement("return $L", cachedValueName);

    return methodSpec.build();
  }

  private static TypeSpec createInputsClass(
      SpecModel specModel,
      SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod,
      String cachedValueName) {
    TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(getInputsClassName(cachedValueName))
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder();

    for (MethodParamModel param : onCalculateCachedValueMethod.methodParams) {
      typeSpec.addField(
          FieldSpec.builder(param.getTypeName(), param.getName(), Modifier.PRIVATE, Modifier.FINAL)
              .build());
      constructor
          .addParameter(ParameterSpec.builder(param.getTypeName(), param.getName()).build())
          .addStatement("this.$L = $L", param.getName(), param.getName());
    }

    typeSpec.addMethod(constructor.build());

    MethodSpec.Builder hashCodeMethod =
        MethodSpec.methodBuilder("hashCode")
            .addAnnotation(Override.class)
            .returns(TypeName.INT)
            .addModifiers(Modifier.PUBLIC);

    CodeBlock.Builder codeBlock =
        CodeBlock.builder().add("return $T.hash(", ClassNames.COMMON_UTILS);
    for (int i = 0, size = onCalculateCachedValueMethod.methodParams.size(); i < size; i++) {
      if (i < size - 1) {
        codeBlock.add("$L, ", onCalculateCachedValueMethod.methodParams.get(i).getName());
      } else {
        codeBlock.add("$L);\n", onCalculateCachedValueMethod.methodParams.get(i).getName());
      }
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

    for (MethodParamModel methodParamModel : onCalculateCachedValueMethod.methodParams) {
      equalsMethod.addCode(
          ComponentBodyGenerator.getCompareStatement(
              specModel,
              methodParamModel,
              methodParamModel.getName(),
              "cachedValueInputs." + methodParamModel.getName()));
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
