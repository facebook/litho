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

import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.WorkingRangeDeclarationModel;
import com.facebook.litho.specmodels.model.WorkingRangeMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.util.Locale;
import javax.lang.model.element.Modifier;

public class WorkingRangeGenerator {

  private WorkingRangeGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final TypeSpecDataHolder.Builder builder =
        TypeSpecDataHolder.newBuilder()
            .addTypeSpecDataHolder(generateWorkingRangeMethodDelegates(specModel))
            .addTypeSpecDataHolder(generateStaticRegisterMethods(specModel));

    if (!specModel.getWorkingRangeMethods().isEmpty()) {
      builder.addMethod(generateDispatchOnEnteredRangeMethod(specModel));
      builder.addMethod(generateDispatchOnExitedRangeMethod(specModel));
    }

    return builder.build();
  }

  /** Generate dispatchOnEnteredRange() implementation for the component. */
  static MethodSpec generateDispatchOnEnteredRangeMethod(SpecModel specModel) {
    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("dispatchOnEnteredRange")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.VOID)
            .addParameter(specModel.getContextClass(), "c")
            .addParameter(ClassNames.STRING, "name");

    methodBuilder.beginControlFlow("switch (name)");

    for (WorkingRangeMethodModel model : specModel.getWorkingRangeMethods()) {
      if (model.enteredRangeModel != null && model.enteredRangeModel.typeModel != null) {
        final String nameInAnnotation = model.enteredRangeModel.typeModel.name;
        methodBuilder.beginControlFlow("case \"$L\":", nameInAnnotation);
        methodBuilder.addStatement("$L(c)", model.enteredRangeModel.name);
        methodBuilder.addStatement("return");
        methodBuilder.endControlFlow();
      }
    }

    return methodBuilder.endControlFlow().build();
  }

  /** Generate dispatchOnExitedRange() implementation for the component. */
  static MethodSpec generateDispatchOnExitedRangeMethod(SpecModel specModel) {
    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("dispatchOnExitedRange")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.VOID)
            .addParameter(specModel.getContextClass(), "c")
            .addParameter(ClassNames.STRING, "name");

    methodBuilder.beginControlFlow("switch (name)");

    for (WorkingRangeMethodModel model : specModel.getWorkingRangeMethods()) {
      if (model.exitedRangeModel != null && model.exitedRangeModel.typeModel != null) {
        final String nameInAnnotation = model.exitedRangeModel.typeModel.name;
        methodBuilder.beginControlFlow("case \"$L\":", nameInAnnotation);
        methodBuilder.addStatement("$L(c)", model.exitedRangeModel.name);
        methodBuilder.addStatement("return");
        methodBuilder.endControlFlow();
      }
    }

    return methodBuilder.endControlFlow().build();
  }

  static TypeSpecDataHolder generateWorkingRangeMethodDelegates(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (WorkingRangeMethodModel model : specModel.getWorkingRangeMethods()) {
      if (model.enteredRangeModel != null) {
        typeSpecDataHolder.addMethod(
            generateWorkingRangeMethodDelegate(specModel, model.enteredRangeModel));
      }
      if (model.exitedRangeModel != null) {
        typeSpecDataHolder.addMethod(
            generateWorkingRangeMethodDelegate(specModel, model.exitedRangeModel));
      }
    }

    return typeSpecDataHolder.build();
  }

  /** Generate a delegate to the Spec that defines this onTrigger method. */
  private static MethodSpec generateWorkingRangeMethodDelegate(
      SpecModel specModel, SpecMethodModel<EventMethod, WorkingRangeDeclarationModel> methodModel) {
    final MethodSpec.Builder methodSpec =
        MethodSpec.methodBuilder(methodModel.name.toString())
            .addModifiers(Modifier.PRIVATE)
            .returns(TypeName.VOID)
            .addParameter(ClassNames.COMPONENT_CONTEXT, "c");

    final CodeBlock.Builder delegation = CodeBlock.builder();

    final String sourceDelegateAccessor = SpecModelUtils.getSpecAccessor(specModel);
    delegation.add("$L.$L(\n", sourceDelegateAccessor, methodModel.name);
    delegation.indent();

    for (int i = 0, size = methodModel.methodParams.size(); i < size; i++) {
      final MethodParamModel methodParamModel = methodModel.methodParams.get(i);

      delegation.add(
          "($T) $L",
          methodParamModel.getTypeName(),
          getImplAccessor(specModel, methodParamModel, "c"));
      delegation.add((i < methodModel.methodParams.size() - 1) ? ",\n" : ");\n");
    }
    delegation.unindent();
    methodSpec.addCode(delegation.build());

    return methodSpec.build();
  }

  static TypeSpecDataHolder generateStaticRegisterMethods(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (WorkingRangeMethodModel model : specModel.getWorkingRangeMethods()) {
      if (model.enteredRangeModel != null && model.enteredRangeModel.typeModel != null) {
        typeSpecDataHolder.addMethod(
            generateStaticRegisterMethod(specModel.getContextClass(), model.enteredRangeModel));

      } else if (model.exitedRangeModel != null && model.exitedRangeModel.typeModel != null) {
        typeSpecDataHolder.addMethod(
            generateStaticRegisterMethod(specModel.getContextClass(), model.exitedRangeModel));
      }
    }

    return typeSpecDataHolder.build();
  }

  private static MethodSpec generateStaticRegisterMethod(
      ClassName contextClassName,
      SpecMethodModel<EventMethod, WorkingRangeDeclarationModel> methodModel) {
    String nameInAnnotation = (methodModel.typeModel != null) ? methodModel.typeModel.name : "";
    if (nameInAnnotation == null || nameInAnnotation.length() == 0) {
      throw new RuntimeException("Must provide a name for @OnEnteredRange and @OnExitedRange.");
    }

    final String methodName =
        "register"
            + nameInAnnotation.substring(0, 1).toUpperCase(Locale.US)
            + nameInAnnotation.substring(1)
            + "WorkingRange";

    MethodSpec.Builder registerMethod =
        MethodSpec.methodBuilder(methodName).addModifiers(Modifier.STATIC);

    registerMethod
        .addParameter(contextClassName, "c")
        .addParameter(ClassNames.WORKING_RANGE, "workingRange")
        .beginControlFlow("if (workingRange == null)")
        .addStatement("return")
        .endControlFlow()
        .addStatement("$T component = c.getComponentScope()", ClassNames.COMPONENT)
        .addStatement("registerWorkingRange(\"$L\", workingRange, component)", nameInAnnotation);

    return registerMethod.build();
  }
}
