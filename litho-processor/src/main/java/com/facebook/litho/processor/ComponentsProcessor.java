/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.ComponentImplGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.MountSpecGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.PureRenderGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TreePropGenerator;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MountSpecModel;

import com.squareup.javapoet.TypeSpec;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ComponentsProcessor extends AbstractComponentsProcessor {

  /**
   * Generate the entire source file for the component.
   */
  @Override
  protected void generate(MountSpecHelper mountSpecHelper) {
    final MountSpecModel specModel = (MountSpecModel) mountSpecHelper.getSpecModel();
    final TypeSpec.Builder typeSpec = mountSpecHelper.getTypeSpec();

    mountSpecHelper.getTypeSpec().addModifiers(Modifier.FINAL);
    JavadocGenerator.generate(mountSpecHelper.getSpecModel())
        .addToTypeSpec(mountSpecHelper.getTypeSpec());
    PreambleGenerator.generate(mountSpecHelper.getSpecModel())
        .addToTypeSpec(mountSpecHelper.getTypeSpec());
    PureRenderGenerator.generate(specModel).addToTypeSpec(typeSpec);
    TreePropGenerator.generate(specModel).addToTypeSpec(typeSpec);
    DelegateMethodGenerator.generateDelegates(
        specModel,
        DelegateMethodDescriptions.MOUNT_SPEC_DELEGATE_METHODS_MAP).addToTypeSpec(typeSpec);
    MountSpecGenerator.generateGetMountType(specModel).addToTypeSpec(typeSpec);
    MountSpecGenerator.generatePoolSize(specModel).addToTypeSpec(typeSpec);
    MountSpecGenerator.generateCanMountIncrementally(specModel).addToTypeSpec(typeSpec);
    MountSpecGenerator.generateShouldUseDisplayList(specModel).addToTypeSpec(typeSpec);


    ComponentImplGenerator.generate(specModel).addToTypeSpec(typeSpec);
    EventGenerator.generate(specModel).addToTypeSpec(typeSpec);
    StateGenerator.generate(specModel).addToTypeSpec(typeSpec);
    BuilderGenerator.generate(specModel).addToTypeSpec(typeSpec);
  }

  @Override
  protected DependencyInjectionHelper getDependencyInjectionGenerator(TypeElement typeElement) {
    return null;
  }
}
