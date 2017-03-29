/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromMeasureBaseline;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MountSpecModel;

/**
 * Factory for creating {@link MountSpecModel}s.
 */
public class MountSpecModelFactory {
  private static final List<Class<? extends Annotation>> INTER_STAGE_INPUT_ANNOTATIONS =
      new ArrayList<>();
  private static final List<Class<? extends Annotation>> DELEGATE_METHOD_ANNOTATIONS =
      new ArrayList<>();
  static {
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromPrepare.class);
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromMeasureBaseline.class);
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromMeasure.class);
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromBoundsDefined.class);
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromBind.class);
    DELEGATE_METHOD_ANNOTATIONS.addAll(
        DelegateMethodDescriptions.MOUNT_SPEC_DELEGATE_METHODS_MAP.keySet());
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateTreeProp.class);
    DELEGATE_METHOD_ANNOTATIONS.add(ShouldUpdate.class);
  }

  /**
   * Create a {@link MountSpecModel} from the given {@link TypeElement} and an optional
   * {@link DependencyInjectionHelper}.
   */
  public static MountSpecModel create(
      Elements elements,
      TypeElement element,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper) {
    return new MountSpecModel(
        element.getQualifiedName().toString(),
        DelegateMethodExtractor.getDelegateMethods(
            element,
            DELEGATE_METHOD_ANNOTATIONS,
            INTER_STAGE_INPUT_ANNOTATIONS),
        EventMethodExtractor.getOnEventMethods(
            elements, element, INTER_STAGE_INPUT_ANNOTATIONS),
        UpdateStateMethodExtractor.getOnUpdateStateMethods(
            element,
            INTER_STAGE_INPUT_ANNOTATIONS),
        ImmutableList.copyOf(TypeVariablesExtractor.getTypeVariables(element)),
        ImmutableList.copyOf(PropDefaultsExtractor.getPropDefaults(element)),
        EventDeclarationsExtractor.getEventDeclarations(elements, element, MountSpec.class),
        JavadocExtractor.getClassJavadoc(elements, element),
        JavadocExtractor.getPropJavadocs(elements, element),
        element.getAnnotation(MountSpec.class).isPublic(),
        dependencyInjectionHelper,
        element.getAnnotation(MountSpec.class).isPureRender(),
        element);
  }
}
