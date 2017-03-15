// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.List;

import com.facebook.components.specmodels.model.ClassNames;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

/**
 * Generates populateTreeProps.
 */
public class PopulateTreePropsMethodBuilder {

  final List<Parameter> treeProps = new ArrayList<>();
  String lifecycleImplClass;
  ClassName componentClassName;

  MethodSpec build() {
    final MethodSpec.Builder builder = MethodSpec.methodBuilder("populateTreeProps")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(componentClassName, "_abstractImpl")
        .addParameter(ClassNames.TREE_PROPS, "treeProps")
        .addStatement("if (treeProps == null) return")
        .addStatement(
            "final $L $L = ($L) $L",
            lifecycleImplClass,
            "_impl",
            lifecycleImplClass,
            "_abstractImpl");
    for (Parameter treeProp : treeProps) {
      builder.addStatement("_impl.$L = treeProps.get($L.class)", treeProp.name, treeProp.type);
    }

    return builder.build();
  }
}
