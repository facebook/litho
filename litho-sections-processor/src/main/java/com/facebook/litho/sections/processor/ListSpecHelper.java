/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.annotations.OnDestroyService;
import com.facebook.litho.sections.annotations.OnUnbindService;
import com.facebook.litho.sections.processor.specmodels.model.HasService;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.ComponentsProcessingException;
import com.squareup.javapoet.TypeName;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Helper class to generate common code for {@link DiffSectionSpec} and {@link GroupSectionSpec}.
 */
public abstract class ListSpecHelper<S extends SpecModel & HasService> extends SpecHelper<S>
    implements Closeable {

  private static final String SERVICE_INSTANCE_NAME = "_service";
  private static final String ON_CREATE_SERVICE_METHOD_NAME = "onCreateService";

  public ListSpecHelper(
      ProcessingEnvironment processingEnv,
      TypeElement specElement,
      String name,
      boolean isPublic,
      Class<Annotation>[] stageAnnotations,
      Class<Annotation>[] interStageInputAnnotations,
      S specModel) {
    super(
        processingEnv,
        specElement,
        name,
        isPublic,
        stageAnnotations,
        interStageInputAnnotations,
        specModel);
  }

  @Override
  protected TypeName getSuperclassClass() {
    return SectionClassNames.SECTION_LIFECYCLE;
  }

  @Override
  protected boolean isStateSupported() {
    return true;
  }

  @Override
  protected Map<String, TypeMirror> populateExtraStateMembers() {
    Map<String, TypeMirror> extraMember = new HashMap<>();
    final TypeMirror serviceType = getServiceType();
    if (serviceType != null) {
      extraMember.put(SERVICE_INSTANCE_NAME, getServiceType());
    }

    return extraMember;
  }

  private TypeMirror getServiceType() {
    final ExecutableElement onCreateService = Utils.getAnnotatedMethod(
        mSpecElement,
        OnCreateService.class);

    if (onCreateService == null) {
      return null;
    }

    final TypeMirror serviceType = onCreateService.getReturnType();

    final ExecutableElement onBindService = Utils.getAnnotatedMethod(
        mSpecElement,
        OnBindService.class);

    if (onBindService == null) {
      throw new ComponentsProcessingException(
          onBindService,
          "OnBindService has to be defined if OnCreateService is defined");
    }

    validateServiceLifecycleMethod(onBindService, serviceType);

    final ExecutableElement onUnbindService = Utils.getAnnotatedMethod(
        mSpecElement,
        OnUnbindService.class);
    if (onUnbindService != null) {
      validateServiceLifecycleMethod(onUnbindService, serviceType);
    }

    final ExecutableElement onDestroyService = Utils.getAnnotatedMethod(
        mSpecElement,
        OnDestroyService.class);
    if (onDestroyService != null) {
      validateServiceLifecycleMethod(onDestroyService, serviceType);
    }

    return serviceType;
  }

  private void validateServiceLifecycleMethod(
      ExecutableElement element,
      TypeMirror serviceType) {

    if (element.getParameters().size() < 2) {
      throw new ComponentsProcessingException(
          element,
          element.getSimpleName() + " has to be defined with ListContext and "
              + serviceType.toString() + " as mandatory parameters");
    }

    if (!element.getParameters().get(1).asType().equals(serviceType)) {
      throw new ComponentsProcessingException(
          element,
          "The service defined in " + element.getSimpleName() +
              " and the one returned from OnCreateService are not of the same type");
    }
  }

  /**
   * Generate a ShouldUpdate method that delegates to the method in the Spec annotated with
   * {@link ShouldUpdate} annotation.
   */
  public void generateShouldUpdate() {
    final ExecutableElement onShouldUpdate = Utils.getAnnotatedMethod(
        mSpecElement,
        ShouldUpdate.class);
    if (onShouldUpdate != null) {
      mStages.generateShouldUpdateMethod(
          onShouldUpdate,
          SectionClassNames.SECTION);
    }
  }

  /**
   * Generate a createInitialState that delegates to the method in the Spec annotated with
   * {@link OnCreateInitialState} annotation.
   */
  public void generateCreateInitialState() {
    final ExecutableElement onCreateInitialState = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnCreateInitialState.class);
    if (onCreateInitialState == null) {
      return;
    }

    mStages.generateCreateInitialState(
        onCreateInitialState,
        SectionClassNames.SECTION_CONTEXT,
        SectionClassNames.SECTION);
  }
}
