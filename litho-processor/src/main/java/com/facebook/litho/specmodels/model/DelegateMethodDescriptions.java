/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.model;

import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.CACHED_VALUE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_STATE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.INJECT_PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.INTER_STAGE_OUTPUT;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP_OUTPUT;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE_VALUE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.TREE_PROP;

import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromMeasureBaseline;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.FromPreviousCreateLayout;
import com.facebook.litho.annotations.GetExtraAccessibilityNodeAt;
import com.facebook.litho.annotations.GetExtraAccessibilityNodesCount;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnCreateMountContentPool;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnError;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMeasureBaseline;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPopulateExtraAccessibilityNode;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnShouldCreateLayoutWithNewSizeSpec;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.ShouldAlwaysRemeasure;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.element.Modifier;

/** Descriptions of delegate methods. */
public final class DelegateMethodDescriptions {
  public static final DelegateMethodDescription ON_LOAD_STYLE =
      DelegateMethodDescription.newBuilder()
          .annotations(
              ImmutableList.of(
                  AnnotationSpec.builder(SuppressWarnings.class)
                      .addMember("value", "$S", "unchecked")
                      .build(),
                  AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onLoadStyle")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP_OUTPUT))
          .build();

  public static final DelegateMethodDescription ON_CREATE_LAYOUT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(ClassNames.COMPONENT)
          .name("onCreateLayout")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .build();

  public static final DelegateMethodDescription ON_ERROR =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onError")
          .definedParameterTypes(
              ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT, ClassNames.EXCEPTION))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .extraMethods(
              ImmutableList.of(
                  MethodSpec.methodBuilder("hasOwnErrorHandler")
                      .addAnnotation(Override.class)
                      .addModifiers(Modifier.PROTECTED)
                      .returns(TypeName.BOOLEAN)
                      .addStatement("return true")
                      .build()))
          .build();

  public static final DelegateMethodDescription ON_CREATE_LAYOUT_WITH_SIZE_SPEC =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(ClassNames.COMPONENT)
          .name("onCreateLayoutWithSizeSpec")
          .definedParameterTypes(
              ImmutableList.of(ClassNames.COMPONENT_CONTEXT, TypeName.INT, TypeName.INT))
          .optionalParameterTypes(
              ImmutableList.of(
                  PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT, INJECT_PROP, CACHED_VALUE))
          .extraMethods(
              ImmutableList.of(
                  MethodSpec.methodBuilder("canMeasure")
                      .addAnnotation(Override.class)
                      .addModifiers(Modifier.PROTECTED)
                      .returns(TypeName.BOOLEAN)
                      .addStatement("return true")
                      .build()))
          .build();

  public static final DelegateMethodDescription ON_SHOULD_CREATE_LAYOUT_WITH_NEW_SIZE_SPEC =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.BOOLEAN)
          .name("onShouldCreateLayoutWithNewSizeSpec")
          .definedParameterTypes(
              ImmutableList.of(ClassNames.COMPONENT_CONTEXT, TypeName.INT, TypeName.INT))
          .optionalParameterTypes(
              ImmutableList.of(
                  PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.<Class<? extends Annotation>>of(FromPreviousCreateLayout.class))
          .extraMethods(
              ImmutableList.of(
                  MethodSpec.methodBuilder("isLayoutSpecWithSizeSpecCheck")
                      .addAnnotation(Override.class)
                      .addModifiers(Modifier.PROTECTED)
                      .returns(TypeName.BOOLEAN)
                      .addStatement("return true")
                      .build()))
          .build();

  public static final DelegateMethodDescription ON_CREATE_INITIAL_STATE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("createInitialState")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE_VALUE, INJECT_PROP))
          .build();

  public static final DelegateMethodDescription ON_CREATE_TRANSITION =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(ClassNames.TRANSITION)
          .name("onCreateTransition")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, DIFF, INJECT_PROP, CACHED_VALUE))
          .build();

  public static final DelegateMethodDescription ON_PREPARE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onPrepare")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(
              ImmutableList.of(
                  PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT, INJECT_PROP, CACHED_VALUE))
          .build();

  public static final DelegateMethodDescription ON_MEASURE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onMeasure")
          .definedParameterTypes(
              ImmutableList.of(
                  ClassNames.COMPONENT_CONTEXT,
                  ClassNames.COMPONENT_LAYOUT,
                  TypeName.INT,
                  TypeName.INT,
                  ClassNames.SIZE))
          .optionalParameterTypes(
              ImmutableList.of(
                  PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.<Class<? extends Annotation>>of(FromPrepare.class))
          .extraMethods(
              ImmutableList.of(
                  MethodSpec.methodBuilder("canMeasure")
                      .addAnnotation(Override.class)
                      .addModifiers(Modifier.PROTECTED)
                      .returns(TypeName.BOOLEAN)
                      .addStatement("return true")
                      .build()))
          .build();

  public static final DelegateMethodDescription ON_MEASURE_BASELINE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.INT)
          .name("onMeasureBaseline")
          .definedParameterTypes(
              ImmutableList.of(ClassNames.COMPONENT_CONTEXT, TypeName.INT, TypeName.INT))
          .optionalParameterTypes(
              ImmutableList.of(
                  PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.<Class<? extends Annotation>>of(FromPrepare.class))
          .build();

  public static final DelegateMethodDescription ON_BOUNDS_DEFINED =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onBoundsDefined")
          .definedParameterTypes(
              ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT, ClassNames.COMPONENT_LAYOUT))
          .optionalParameterTypes(
              ImmutableList.of(
                  PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.of(FromPrepare.class, FromMeasure.class, FromMeasureBaseline.class))
          .build();

  public static final DelegateMethodDescription ON_CREATE_MOUNT_CONTENT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.OBJECT)
          .name("onCreateMountContent")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.ANDROID_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(INJECT_PROP))
          .build();

  public static final DelegateMethodDescription ON_CREATE_MOUNT_CONTENT_POOL =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(ClassNames.MOUNT_CONTENT_POOL)
          .name("onCreateMountContentPool")
          .definedParameterTypes(ImmutableList.<TypeName>of())
          .optionalParameterTypes(ImmutableList.of(INJECT_PROP))
          .build();

  public static final DelegateMethodDescription ON_MOUNT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onMount")
          .definedParameterTypes(
              ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT, ClassNames.OBJECT))
          .optionalParameterTypes(
              ImmutableList.of(
                  PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.of(
                  FromPrepare.class,
                  FromMeasure.class,
                  FromMeasureBaseline.class,
                  FromBoundsDefined.class))
          .build();

  public static final DelegateMethodDescription ON_BIND =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onBind")
          .definedParameterTypes(
              ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT, ClassNames.OBJECT))
          .optionalParameterTypes(
              ImmutableList.of(
                  PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.of(
                  FromPrepare.class,
                  FromMeasure.class,
                  FromMeasureBaseline.class,
                  FromBoundsDefined.class))
          .build();

  public static final DelegateMethodDescription ON_UNBIND =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onUnbind")
          .definedParameterTypes(
              ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT, ClassNames.OBJECT))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.of(
                  FromPrepare.class,
                  FromMeasure.class,
                  FromMeasureBaseline.class,
                  FromBoundsDefined.class,
                  FromBind.class))
          .build();

  public static final DelegateMethodDescription ON_UNMOUNT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onUnmount")
          .definedParameterTypes(
              ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT, ClassNames.OBJECT))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.of(
                  FromPrepare.class,
                  FromMeasure.class,
                  FromMeasureBaseline.class,
                  FromBoundsDefined.class))
          .build();

  public static final DelegateMethodDescription SHOULD_UPDATE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.BOOLEAN)
          .name("shouldUpdate")
          .definedParameterTypes(ImmutableList.of())
          .optionalParameterTypes(ImmutableList.of(DIFF_PROP, DIFF_STATE, INJECT_PROP))
          .build();

  public static final DelegateMethodDescription ON_POPULATE_ACCESSIBILITY_NODE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onPopulateAccessibilityNode")
          .definedParameterTypes(
              ImmutableList.<TypeName>of(ClassNames.VIEW, ClassNames.ACCESSIBILITY_NODE))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.of(
                  FromPrepare.class,
                  FromMeasure.class,
                  FromMeasureBaseline.class,
                  FromBoundsDefined.class,
                  FromBind.class))
          .extraMethods(
              ImmutableList.of(
                  MethodSpec.methodBuilder("implementsAccessibility")
                      .addAnnotation(Override.class)
                      .addModifiers(Modifier.PUBLIC)
                      .returns(TypeName.BOOLEAN)
                      .addStatement("return true")
                      .build()))
          .build();

  public static final DelegateMethodDescription ON_POPULATE_EXTRA_ACCESSIBILITY_NODE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onPopulateExtraAccessibilityNode")
          .definedParameterTypes(
              ImmutableList.of(
                  ClassNames.ACCESSIBILITY_NODE, TypeName.INT, TypeName.INT, TypeName.INT))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.of(
                  FromPrepare.class,
                  FromMeasure.class,
                  FromMeasureBaseline.class,
                  FromBoundsDefined.class,
                  FromBind.class))
          .extraMethods(
              ImmutableList.of(
                  MethodSpec.methodBuilder("implementsExtraAccessibilityNodes")
                      .addAnnotation(Override.class)
                      .addModifiers(Modifier.PUBLIC)
                      .returns(TypeName.BOOLEAN)
                      .addStatement("return true")
                      .build()))
          .build();

  public static final DelegateMethodDescription GET_EXTRA_ACCESSIBILITY_NODE_AT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.INT)
          .name("getExtraAccessibilityNodeAt")
          .definedParameterTypes(ImmutableList.of(TypeName.INT, TypeName.INT))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.of(
                  FromPrepare.class,
                  FromMeasure.class,
                  FromMeasureBaseline.class,
                  FromBoundsDefined.class,
                  FromBind.class))
          .build();

  public static final DelegateMethodDescription GET_EXTRA_ACCESSIBILITY_NODES_COUNT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.INT)
          .name("getExtraAccessibilityNodesCount")
          .definedParameterTypes(ImmutableList.<TypeName>of())
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .interStageInputAnnotations(
              ImmutableList.of(
                  FromPrepare.class,
                  FromMeasure.class,
                  FromMeasureBaseline.class,
                  FromBoundsDefined.class,
                  FromBind.class))
          .build();

  public static final DelegateMethodDescription SHOULD_ALWAYS_REMEASURE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.BOOLEAN)
          .name("shouldAlwaysRemeasure")
          .definedParameterTypes(ImmutableList.of())
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .build();

  public static final DelegateMethodDescription ON_ATTACHED =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onAttached")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .build();

  public static final DelegateMethodDescription ON_DETACHED =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onDetached")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(
              ImmutableList.of(PROP, TREE_PROP, STATE, INJECT_PROP, CACHED_VALUE))
          .build();

  public static final Map<Class<? extends Annotation>, DelegateMethodDescription>
      LAYOUT_SPEC_DELEGATE_METHODS_MAP;

  public static final Map<Class<? extends Annotation>, DelegateMethodDescription>
      MOUNT_SPEC_DELEGATE_METHODS_MAP;

  public static final Map<Class<? extends Annotation>, Class<? extends Annotation>>
      INTER_STAGE_INPUTS_MAP;

  static {
    Map<Class<? extends Annotation>, DelegateMethodDescription> layoutSpecDelegateMethodsMap =
        new TreeMap<>(
            new Comparator<Class<? extends Annotation>>() {
              @Override
              public int compare(Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
                return lhs.toString().compareTo(rhs.toString());
              }
            });
    layoutSpecDelegateMethodsMap.put(OnLoadStyle.class, ON_LOAD_STYLE);
    layoutSpecDelegateMethodsMap.put(OnError.class, ON_ERROR);
    layoutSpecDelegateMethodsMap.put(OnCreateLayout.class, ON_CREATE_LAYOUT);
    layoutSpecDelegateMethodsMap.put(
        OnCreateLayoutWithSizeSpec.class, ON_CREATE_LAYOUT_WITH_SIZE_SPEC);
    layoutSpecDelegateMethodsMap.put(
        OnShouldCreateLayoutWithNewSizeSpec.class, ON_SHOULD_CREATE_LAYOUT_WITH_NEW_SIZE_SPEC);
    layoutSpecDelegateMethodsMap.put(OnCreateInitialState.class, ON_CREATE_INITIAL_STATE);
    layoutSpecDelegateMethodsMap.put(OnCreateTransition.class, ON_CREATE_TRANSITION);
    layoutSpecDelegateMethodsMap.put(ShouldUpdate.class, SHOULD_UPDATE);
    layoutSpecDelegateMethodsMap.put(OnAttached.class, ON_ATTACHED);
    layoutSpecDelegateMethodsMap.put(OnDetached.class, ON_DETACHED);
    LAYOUT_SPEC_DELEGATE_METHODS_MAP = Collections.unmodifiableMap(layoutSpecDelegateMethodsMap);

    Map<Class<? extends Annotation>, DelegateMethodDescription> mountSpecDelegateMethodsMap =
        new TreeMap<>(
            new Comparator<Class<? extends Annotation>>() {
              @Override
              public int compare(Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
                return lhs.toString().compareTo(rhs.toString());
              }
            });
    mountSpecDelegateMethodsMap.put(OnCreateInitialState.class, ON_CREATE_INITIAL_STATE);
    mountSpecDelegateMethodsMap.put(OnLoadStyle.class, ON_LOAD_STYLE);
    mountSpecDelegateMethodsMap.put(OnError.class, ON_ERROR);
    mountSpecDelegateMethodsMap.put(OnPrepare.class, ON_PREPARE);
    mountSpecDelegateMethodsMap.put(OnMeasure.class, ON_MEASURE);
    mountSpecDelegateMethodsMap.put(OnMeasureBaseline.class, ON_MEASURE_BASELINE);
    mountSpecDelegateMethodsMap.put(OnBoundsDefined.class, ON_BOUNDS_DEFINED);
    mountSpecDelegateMethodsMap.put(OnCreateMountContent.class, ON_CREATE_MOUNT_CONTENT);
    mountSpecDelegateMethodsMap.put(OnCreateMountContentPool.class, ON_CREATE_MOUNT_CONTENT_POOL);
    mountSpecDelegateMethodsMap.put(OnMount.class, ON_MOUNT);
    mountSpecDelegateMethodsMap.put(OnBind.class, ON_BIND);
    mountSpecDelegateMethodsMap.put(OnUnbind.class, ON_UNBIND);
    mountSpecDelegateMethodsMap.put(OnUnmount.class, ON_UNMOUNT);
    mountSpecDelegateMethodsMap.put(ShouldUpdate.class, SHOULD_UPDATE);
    mountSpecDelegateMethodsMap.put(
        OnPopulateAccessibilityNode.class, ON_POPULATE_ACCESSIBILITY_NODE);
    mountSpecDelegateMethodsMap.put(
        OnPopulateExtraAccessibilityNode.class, ON_POPULATE_EXTRA_ACCESSIBILITY_NODE);
    mountSpecDelegateMethodsMap.put(
        GetExtraAccessibilityNodeAt.class, GET_EXTRA_ACCESSIBILITY_NODE_AT);
    mountSpecDelegateMethodsMap.put(
        GetExtraAccessibilityNodesCount.class, GET_EXTRA_ACCESSIBILITY_NODES_COUNT);
    mountSpecDelegateMethodsMap.put(ShouldAlwaysRemeasure.class, SHOULD_ALWAYS_REMEASURE);
    mountSpecDelegateMethodsMap.put(OnAttached.class, ON_ATTACHED);
    mountSpecDelegateMethodsMap.put(OnDetached.class, ON_DETACHED);
    MOUNT_SPEC_DELEGATE_METHODS_MAP = Collections.unmodifiableMap(mountSpecDelegateMethodsMap);

    Map<Class<? extends Annotation>, Class<? extends Annotation>> interStageInputsMap =
        new TreeMap<>(
            new Comparator<Class<? extends Annotation>>() {
              @Override
              public int compare(Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
                return lhs.toString().compareTo(rhs.toString());
              }
            });
    interStageInputsMap.put(FromPreviousCreateLayout.class, OnCreateLayoutWithSizeSpec.class);
    interStageInputsMap.put(FromPrepare.class, OnPrepare.class);
    interStageInputsMap.put(FromMeasure.class, OnMeasure.class);
    interStageInputsMap.put(FromMeasureBaseline.class, OnMeasureBaseline.class);
    interStageInputsMap.put(FromBoundsDefined.class, OnBoundsDefined.class);
    interStageInputsMap.put(FromBind.class, OnBind.class);
    INTER_STAGE_INPUTS_MAP = Collections.unmodifiableMap(interStageInputsMap);
  }
}
