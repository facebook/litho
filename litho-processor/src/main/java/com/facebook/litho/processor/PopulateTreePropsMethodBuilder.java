/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.List;

import com.facebook.litho.specmodels.model.ClassNames;

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
