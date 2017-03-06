// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.List;

import com.facebook.components.specmodels.model.ClassNames;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

/**
 * Generates getTreePropsForChildren methods.
 */
class GetTreePropsForChildrenMethodBuilder {

  static class CreateTreePropMethodData {
    String name;
    TypeName returnType;
    String treePropName;
    List<Parameter> parameters = new ArrayList<>();
  }

  List<CreateTreePropMethodData> createTreePropMethods = new ArrayList<>();
  String lifecycleImplClass;
  String delegateName;
  ClassName contextClassName;
  ClassName componentClassName;

  MethodSpec build() {
    final MethodSpec.Builder builder = MethodSpec.methodBuilder("getTreePropsForChildren")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .returns(ClassNames.TREE_PROPS)
        .addParameter(contextClassName, "c")
        .addParameter(componentClassName, "_abstractImpl")
        .addParameter(ClassNames.TREE_PROPS, "parentTreeProps")
        .addStatement(
            "final $L $L = ($L) $L",
            lifecycleImplClass,
            "_impl",
            lifecycleImplClass,
            "_abstractImpl")
        .addStatement(
            "final $T childTreeProps = $T.copy(parentTreeProps)",
            ClassNames.TREE_PROPS,
            ClassNames.TREE_PROPS);

    for (CreateTreePropMethodData method : createTreePropMethods) {
      CodeBlock.Builder block = CodeBlock.builder();
      // A unique key to identify the TreeProp based on its type and name.
      String key = method.returnType.toString() + "~" + method.treePropName;
      block.add("childTreeProps.put($S, $L.$L(\n", key, delegateName, method.name)
          .indent()
          .indent();
      List<Parameter> parameters = method.parameters;

      for (int i = 0; i < parameters.size(); i++) {
        if (i == 0) {
          block.add("($T) $L", contextClassName, "c");
        } else {
          block.add("($T) _impl.$L", parameters.get(i).type, parameters.get(i).name);
        }
        if (i < parameters.size() - 1) {
          block.add(",\n");
        }
      }

      builder.addCode(block.add("));\n")
          .unindent()
          .unindent()
          .build());
    }

    return builder
        .addStatement("return childTreeProps")
        .build();
  }
}
