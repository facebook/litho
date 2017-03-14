// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.facebook.components.specmodels.generator.BuilderGenerator;
import com.facebook.components.specmodels.generator.EventGenerator;
import com.facebook.components.specmodels.generator.JavadocGenerator;
import com.facebook.components.specmodels.generator.PreambleGenerator;
import com.facebook.components.specmodels.generator.StateGenerator;
import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.DependencyInjectionHelper;
import com.facebook.components.specmodels.model.SpecModel;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ComponentsProcessor extends AbstractComponentsProcessor {

  @Override
  protected void generate(ReferenceSpecHelper referenceSpecHelper) {
    generatePreamble(referenceSpecHelper.getStages());

    referenceSpecHelper.getStages()
        .generateReferenceImplClass(
            Stages.StaticFlag.STATIC,
            referenceSpecHelper.getReferenceType());
    referenceSpecHelper.generateOnAcquire();
    referenceSpecHelper.generateOnRelease();

    referenceSpecHelper.getStages().generateReferenceBuilder(
        Stages.StaticFlag.STATIC,
        TypeName.get(referenceSpecHelper.getReferenceType()));

    referenceSpecHelper.generateShouldUpdate();
  }

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

    mountSpecHelper.getStages().generateComponentImplClass(Stages.StaticFlag.STATIC);
    mountSpecHelper.generateTreePropsMethods();
    mountSpecHelper.generateOnPrepare();
    mountSpecHelper.generateOnMeasure();
    mountSpecHelper.generateOnMeasureBaseline();
    mountSpecHelper.generateOnBoundsDefined();
    mountSpecHelper.generateOnCreateMountContentAndGetMountType();
    mountSpecHelper.generateOnMount();
    mountSpecHelper.generateOnBind();
    mountSpecHelper.generateOnUnbind();
    mountSpecHelper.generateOnUnmount();
    mountSpecHelper.generateAccessibilityMethods();
    mountSpecHelper.generateCanMountIncrementally();
    mountSpecHelper.generateShouldUseDisplayList();
    mountSpecHelper.generateCreateInitialState();

    final Stages stages = mountSpecHelper.getStages();
    stages.generateOnLoadStyle();

    EventGenerator.generate(specModel).addToTypeSpec(typeSpec);
    StateGenerator.generate(specModel).addToTypeSpec(typeSpec);
    BuilderGenerator.generate(specModel).addToTypeSpec(typeSpec);
  }

  @Override
  protected DependencyInjectionHelper getDependencyInjectionGenerator(TypeElement typeElement) {
    return null;
  }

  private void generatePreamble(Stages stages) {
    stages.generateSourceDelegate(true);
    stages.generateConstructor();
    stages.generateGetter(/*isStatic*/ true);
  }
}
