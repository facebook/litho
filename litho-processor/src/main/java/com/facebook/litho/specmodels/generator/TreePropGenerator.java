/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.specmodels.generator;

import javax.lang.model.element.Modifier;

import java.util.List;

import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.DelegateMethodModel;
import com.facebook.components.specmodels.model.SpecModel;
import com.facebook.components.specmodels.model.SpecModelUtils;
import com.facebook.components.specmodels.model.TreePropModel;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import static com.facebook.components.specmodels.generator.GeneratorConstants.DELEGATE_FIELD_NAME;

/**
 * Class that generates the tree prop methods for a Component.
 */
public class TreePropGenerator {

  private TreePropGenerator() {
  }

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generatePopulateTreeProps(specModel))
        .addTypeSpecDataHolder(generateGetTreePropsForChildren(specModel))
        .build();
  }

  static TypeSpecDataHolder generatePopulateTreeProps(SpecModel specModel) {
    if (specModel.getTreeProps().isEmpty()) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    final MethodSpec.Builder method = MethodSpec.methodBuilder("populateTreeProps")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(specModel.getComponentClass(), "_abstractImpl")
        .addParameter(ClassNames.TREE_PROPS, "treeProps")
        .beginControlFlow("if (treeProps == null)")
        .addStatement("return")
        .endControlFlow()
        .addStatement(
            "final $L _impl = ($L) _abstractImpl",
            ComponentImplGenerator.getImplClassName(specModel),
            ComponentImplGenerator.getImplClassName(specModel));

    for (TreePropModel treeProp : specModel.getTreeProps()) {
