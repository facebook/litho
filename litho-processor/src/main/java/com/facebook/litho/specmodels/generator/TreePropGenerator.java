/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static com.facebook.litho.specmodels.generator.ComponentBodyGenerator.LOCAL_STATE_CONTAINER_NAME;
import static com.facebook.litho.specmodels.generator.ComponentBodyGenerator.PREDICATE_NEEDS_STATE;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_CONTAINER_IMPL_GETTER;

import com.facebook.litho.annotations.InjectProp;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.TreePropModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.EnumSet;
import java.util.List;
import javax.lang.model.element.Modifier;

/** Class that generates the tree prop methods for a Component. */
public class TreePropGenerator {

  private static final ClassName LAZY_CLASS_NAME = ClassName.get("com.facebook.inject", "Lazy");

  private TreePropGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel, EnumSet<RunMode> runMode) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generatePopulateTreeProps(specModel))
        .addTypeSpecDataHolder(generateGetTreePropsForChildren(specModel, runMode))
        .build();
  }

  static TypeSpecDataHolder generatePopulateTreeProps(SpecModel specModel) {
    if (specModel.getTreeProps().isEmpty()) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    final MethodSpec.Builder method =
        MethodSpec.methodBuilder("populateTreeProps")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PROTECTED)
            .addParameter(ClassNames.TREE_PROPS, "treeProps")
            .beginControlFlow("if (treeProps == null)")
            .addStatement("return")
            .endControlFlow();

    for (TreePropModel treeProp : specModel.getTreeProps()) {
      method.addStatement(
          "$L = treeProps.get($L.class)",
          treeProp.getName(),
          findTypeByTypeName(treeProp.getTypeName()));
    }

    return TypeSpecDataHolder.newBuilder().addMethod(method.build()).build();
  }

  static TypeSpecDataHolder generateGetTreePropsForChildren(
      SpecModel specModel, EnumSet<RunMode> runMode) {
    List<SpecMethodModel<DelegateMethod, Void>> onCreateTreePropsMethods =
        SpecModelUtils.getMethodModelsWithAnnotation(specModel, OnCreateTreeProp.class);

    if (onCreateTreePropsMethods.isEmpty()) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    final String delegateName = SpecModelUtils.getSpecAccessor(specModel);
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder("getTreePropsForChildren")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PROTECTED)
            .returns(ClassNames.TREE_PROPS)
            .addParameter(specModel.getContextClass(), "c")
            .addParameter(ClassNames.TREE_PROPS, "parentTreeProps")
            .addStatement(
                "final $T childTreeProps = $T.acquire(parentTreeProps)",
                ClassNames.TREE_PROPS,
                ClassNames.TREE_PROPS);

    final boolean requiresState =
        onCreateTreePropsMethods.stream()
            .anyMatch(method -> method.methodParams.stream().anyMatch(PREDICATE_NEEDS_STATE));

    if (requiresState) {
      builder.addStatement(
          "$L $L = $L",
          StateContainerGenerator.getStateContainerClassName(specModel),
          LOCAL_STATE_CONTAINER_NAME,
          STATE_CONTAINER_IMPL_GETTER + "(c)");
    }

    for (SpecMethodModel<DelegateMethod, Void> onCreateTreePropsMethod : onCreateTreePropsMethods) {
      final CodeBlock.Builder block = CodeBlock.builder();
      block
          .add(
              "childTreeProps.put($L.class, $L.$L(\n",
              findTypeByTypeName(onCreateTreePropsMethod.returnType),
              delegateName,
              onCreateTreePropsMethod.name)
          .indent()
          .indent();

      for (int i = 0, size = onCreateTreePropsMethod.methodParams.size(); i < size; i++) {
        MethodParamModel methodParamModel = onCreateTreePropsMethod.methodParams.get(i);
        if (i == 0) {
          block.add("($T) $L", specModel.getContextClass(), "c");
        } else if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, State.class)) {
          block.add("$L.$L", LOCAL_STATE_CONTAINER_NAME, methodParamModel.getName());
        } else if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, TreeProp.class)) {
          if (specModel.isStateful()) {
            block.add("$L", methodParamModel.getName());
          } else {
            block.add(
                "(($T) $T.getTreePropFromParent(parentTreeProps,"
                    + TreePropGenerator.findTypeByTypeName(methodParamModel.getTypeName())
                    + ".class"
                    + "))",
                methodParamModel.getTypeName(),
                ClassNames.COMPONENT);
          }
        } else if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, InjectProp.class)) {
          boolean isLazy =
              runMode.contains(RunMode.ABI)
                  ? !methodParamModel.getTypeSpec().toString().contains("Lazy<")
                  : methodParamModel.getTypeSpec().isSubType(LAZY_CLASS_NAME);
          block.add(
              "($T) $L$L",
              methodParamModel.getTypeName(),
              methodParamModel.getName(),
              isLazy ? "" : ".get()");
        } else {
          block.add("$L", methodParamModel.getName());
        }

        if (i < size - 1) {
          block.add(",\n");
        }
      }

      builder.addCode(block.add("));\n").unindent().unindent().build());
    }

    builder.addStatement("return childTreeProps");

    return TypeSpecDataHolder.newBuilder().addMethod(builder.build()).build();
  }

  public static TypeName findTypeByTypeName(final TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType;
    }
    return typeName;
  }
}
