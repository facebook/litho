// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.DependencyInjectionHelper;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

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

    mountSpecHelper.getTypeSpec().addModifiers(Modifier.FINAL);
    generatePreamble(mountSpecHelper.getStages());

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
    mountSpecHelper.generateEvents();
    mountSpecHelper.generateShouldUseDisplayList();
    mountSpecHelper.generateCreateInitialState();

    generatePostamble(mountSpecHelper);
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

  private static void generatePostamble(SpecHelper specHelper) {
    Stages stages = specHelper.getStages();

    stages.generateOnLoadStyle();
    stages.generateOnEventHandlers(ClassNames.COMPONENT, ClassNames.COMPONENT_CONTEXT);
    stages.generateEventHandlerFactories(
        ClassNames.COMPONENT_CONTEXT,
        ClassNames.COMPONENT);
    stages.generateDispatchOnEvent(ClassNames.COMPONENT_CONTEXT);

    stages.generateTransferState(
        ClassNames.COMPONENT_CONTEXT,
        ClassNames.COMPONENT,
        ClassNames.STATE_CONTAINER_COMPONENT);
    stages.generateHasState();
    stages.generateOnStateUpdateMethods(
        ClassNames.COMPONENT_CONTEXT,
        ClassNames.COMPONENT,
        ClassNames.STATE_CONTAINER_COMPONENT,
        ClassNames.COMPONENT_STATE_UPDATE,
        Stages.StaticFlag.STATIC);
    stages.generateLazyStateUpdateMethods(
        ClassNames.COMPONENT_CONTEXT,
        ClassNames.COMPONENT,
        ClassNames.COMPONENT_STATE_UPDATE,
        ClassNames.STATE_CONTAINER_COMPONENT);
    stages.generateComponentBuilder(
        Stages.StaticFlag.STATIC,
        ClassName.bestGuess(stages.getSimpleClassName()));
  }
}
