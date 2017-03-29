/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.GetExtraAccessibilityNodeAt;
import com.facebook.litho.annotations.GetExtraAccessibilityNodesCount;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLayoutTransition;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMeasureBaseline;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPopulateExtraAccessibilityNode;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.INTER_STAGE_OUTPUT;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP_OUTPUT;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE_OUTPUT;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.TREE_PROP;

/**
 * Descriptions of delegate methods.
 */
public final class DelegateMethodDescriptions {
  public static final DelegateMethodDescription ON_LOAD_STYLE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(
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
          .returnType(ClassNames.COMPONENT_LAYOUT)
          .name("onCreateLayout")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT))
          .build();

  public static final DelegateMethodDescription ON_CREATE_LAYOUT_WITH_SIZE_SPEC =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(ClassNames.COMPONENT_LAYOUT)
          .name("onCreateLayoutWithSizeSpec")
          .definedParameterTypes(
              ImmutableList.of(ClassNames.COMPONENT_CONTEXT, TypeName.INT, TypeName.INT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT))
          .extraMethods(
              ImmutableList.of(MethodSpec.methodBuilder("canMeasure")
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
          .optionalParameterTypes(ImmutableList.of(PROP, STATE_OUTPUT))
          .build();

  public static final DelegateMethodDescription ON_LAYOUT_TRANSITION =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(ClassNames.ANIMATION)
          .name("onLayoutTransition")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  public static final DelegateMethodDescription ON_PREPARE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onPrepare")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT))
          .build();

  public static final DelegateMethodDescription ON_MEASURE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onMeasure")
          .definedParameterTypes(ImmutableList.of(
              ClassNames.COMPONENT_CONTEXT,
              ClassNames.COMPONENT_LAYOUT,
              TypeName.INT,
              TypeName.INT,
              ClassNames.SIZE))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT))
          .extraMethods(
              ImmutableList.of(MethodSpec.methodBuilder("canMeasure")
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
          .definedParameterTypes(ImmutableList.of(
              ClassNames.COMPONENT_CONTEXT,
              TypeName.INT,
              TypeName.INT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT))
          .build();

  public static final DelegateMethodDescription ON_BOUNDS_DEFINED =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onBoundsDefined")
          .definedParameterTypes(ImmutableList.<TypeName>of(
              ClassNames.COMPONENT_CONTEXT,
              ClassNames.COMPONENT_LAYOUT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT))
          .build();

  public static final DelegateMethodDescription ON_CREATE_MOUNT_CONTENT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.OBJECT)
          .name("onCreateMountContent")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.COMPONENT_CONTEXT))
          .optionalParameterTypes(
              ImmutableList.<DelegateMethodDescription.OptionalParameterType>of())
          .build();

  public static final DelegateMethodDescription ON_MOUNT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onMount")
          .definedParameterTypes(ImmutableList.<TypeName>of(
              ClassNames.COMPONENT_CONTEXT,
              ClassNames.OBJECT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE, INTER_STAGE_OUTPUT))
          .build();

  public static final DelegateMethodDescription ON_BIND =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onBind")
          .definedParameterTypes(ImmutableList.<TypeName>of(
              ClassNames.COMPONENT_CONTEXT,
              ClassNames.OBJECT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  public static final DelegateMethodDescription ON_UNBIND =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onUnbind")
          .definedParameterTypes(ImmutableList.<TypeName>of(
              ClassNames.COMPONENT_CONTEXT,
              ClassNames.OBJECT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  public static final DelegateMethodDescription ON_UNMOUNT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onUnmount")
          .definedParameterTypes(ImmutableList.<TypeName>of(
              ClassNames.COMPONENT_CONTEXT,
              ClassNames.OBJECT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  public static final DelegateMethodDescription ON_POPULATE_ACCESSIBILITY_NODE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("onPopulateAccessibilityNode")
          .definedParameterTypes(ImmutableList.<TypeName>of(ClassNames.ACCESSIBILITY_NODE))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
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
          .definedParameterTypes(ImmutableList.of(
              ClassNames.ACCESSIBILITY_NODE,
              TypeName.INT,
              TypeName.INT,
              TypeName.INT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
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
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  public static final DelegateMethodDescription GET_EXTRA_ACCESSIBILITY_NODES_COUNT =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.INT)
          .name("getExtraAccessibilityNodesCount")
          .definedParameterTypes(ImmutableList.<TypeName>of())
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  public static final Map<Class<? extends Annotation>, DelegateMethodDescription>
      LAYOUT_SPEC_DELEGATE_METHODS_MAP;
          
  public static final Map<Class<? extends Annotation>, DelegateMethodDescription>
      MOUNT_SPEC_DELEGATE_METHODS_MAP;

  static {
    Map<Class<? extends Annotation>, DelegateMethodDescription> layoutSpecDelegateMethodsMap = 
        new TreeMap<>(new Comparator<Class<? extends Annotation>>() {
      @Override
      public int compare(
          Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
        return lhs.toString().compareTo(rhs.toString());
      }
    });
    layoutSpecDelegateMethodsMap.put(OnLoadStyle.class, ON_LOAD_STYLE);
    layoutSpecDelegateMethodsMap.put(OnCreateLayout.class, ON_CREATE_LAYOUT);
    layoutSpecDelegateMethodsMap.put(
        OnCreateLayoutWithSizeSpec.class, ON_CREATE_LAYOUT_WITH_SIZE_SPEC);
    layoutSpecDelegateMethodsMap.put(OnCreateInitialState.class, ON_CREATE_INITIAL_STATE);
    layoutSpecDelegateMethodsMap.put(OnLayoutTransition.class, ON_LAYOUT_TRANSITION);
    LAYOUT_SPEC_DELEGATE_METHODS_MAP = Collections.unmodifiableMap(layoutSpecDelegateMethodsMap);

    Map<Class<? extends Annotation>, DelegateMethodDescription> mountSpecDelegateMethodsMap =
        new TreeMap<>(new Comparator<Class<? extends Annotation>>() {
          @Override
          public int compare(
              Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
            return lhs.toString().compareTo(rhs.toString());
          }
        });
    mountSpecDelegateMethodsMap.put(OnCreateInitialState.class, ON_CREATE_INITIAL_STATE);
    mountSpecDelegateMethodsMap.put(OnLoadStyle.class, ON_LOAD_STYLE);
    mountSpecDelegateMethodsMap.put(OnPrepare.class, ON_PREPARE);
    mountSpecDelegateMethodsMap.put(OnMeasure.class, ON_MEASURE);
    mountSpecDelegateMethodsMap.put(OnMeasureBaseline.class, ON_MEASURE_BASELINE);
    mountSpecDelegateMethodsMap.put(OnBoundsDefined.class, ON_BOUNDS_DEFINED);
    mountSpecDelegateMethodsMap.put(OnCreateMountContent.class, ON_CREATE_MOUNT_CONTENT);
    mountSpecDelegateMethodsMap.put(OnMount.class, ON_MOUNT);
    mountSpecDelegateMethodsMap.put(OnBind.class, ON_BIND);
    mountSpecDelegateMethodsMap.put(OnUnbind.class, ON_UNBIND);
    mountSpecDelegateMethodsMap.put(OnUnmount.class, ON_UNMOUNT);
    mountSpecDelegateMethodsMap.put(
        OnPopulateAccessibilityNode.class, ON_POPULATE_ACCESSIBILITY_NODE);
    mountSpecDelegateMethodsMap.put(
        OnPopulateExtraAccessibilityNode.class, ON_POPULATE_EXTRA_ACCESSIBILITY_NODE);
    mountSpecDelegateMethodsMap.put(
        GetExtraAccessibilityNodeAt.class, GET_EXTRA_ACCESSIBILITY_NODE_AT);
    mountSpecDelegateMethodsMap.put(
        GetExtraAccessibilityNodesCount.class, GET_EXTRA_ACCESSIBILITY_NODES_COUNT);
    MOUNT_SPEC_DELEGATE_METHODS_MAP = Collections.unmodifiableMap(mountSpecDelegateMethodsMap);
  }
}
