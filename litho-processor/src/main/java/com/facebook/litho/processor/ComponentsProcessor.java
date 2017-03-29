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
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TreePropGenerator;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;

import com.squareup.javapoet.TypeSpec;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ComponentsProcessor extends AbstractComponentsProcessor {

  /**
   * Generate the entire source file for the component.
   */
  @Override
  protected void generate(MountSpecHelper mountSpecHelper) {
    final boolean isPureRender = mountSpecHelper.isPureRender();
    final SpecModel specModel = mountSpecHelper.getSpecModel();
    final TypeSpec.Builder typeSpec = mountSpecHelper.getTypeSpec();

    mountSpecHelper.getTypeSpec().addModifiers(Modifier.FINAL);
    JavadocGenerator.generate(mountSpecHelper.getSpecModel())
        .addToTypeSpec(mountSpecHelper.getTypeSpec());
    PreambleGenerator.generate(mountSpecHelper.getSpecModel())
        .addToTypeSpec(mountSpecHelper.getTypeSpec());

    if (isPureRender) {
      mountSpecHelper.getStages().generateIsPureRender();
      mountSpecHelper.generateShouldUpdate();
      if (mountSpecHelper.callsShouldUpdateOnMount()) {
        mountSpecHelper.getStages().generateCallsShouldUpdateOnMount();
      }
    }

    TreePropGenerator.generate(specModel).addToTypeSpec(typeSpec);
    mountSpecHelper.generateOnPrepare();
    mountSpecHelper.generateOnMeasure();
    mountSpecHelper.generateOnMeasureBaseline();
