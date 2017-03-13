// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.facebook.common.internal.ImmutableList;
import com.facebook.components.annotations.OnCreateInitialState;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.components.annotations.OnLayoutTransition;
import com.facebook.components.annotations.OnLoadStyle;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;

import static com.facebook.components.specmodels.model.DelegateMethodDescription.OptionalParameterType.OUTPUT;
import static com.facebook.components.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP;
import static com.facebook.components.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP_OUTPUT;
import static com.facebook.components.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE;
import static com.facebook.components.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE_OUTPUT;
import static com.facebook.components.specmodels.model.DelegateMethodDescription.OptionalParameterType.TREE_PROP;

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
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE, OUTPUT))
          .build();

  public static final DelegateMethodDescription ON_CREATE_LAYOUT_WITH_SIZE_SPEC =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(ClassNames.COMPONENT_LAYOUT)
          .name("onCreateLayoutWithSizeSpec")
          .definedParameterTypes(
              ImmutableList.of(ClassNames.COMPONENT_CONTEXT, TypeName.INT, TypeName.INT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE, OUTPUT))
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

  public static final Map<Class<? extends Annotation>, DelegateMethodDescription>
      LAYOUT_SPEC_DELEGATE_METHODS_MAP =
          new TreeMap<>(new Comparator<Class<? extends Annotation>>() {
            @Override
            public int compare(
                Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
              return lhs.toString().compareTo(rhs.toString());
            }
          });

  static {
    LAYOUT_SPEC_DELEGATE_METHODS_MAP.put(OnLoadStyle.class, ON_LOAD_STYLE);
    LAYOUT_SPEC_DELEGATE_METHODS_MAP.put(OnCreateLayout.class, ON_CREATE_LAYOUT);
    LAYOUT_SPEC_DELEGATE_METHODS_MAP.put(
        OnCreateLayoutWithSizeSpec.class, ON_CREATE_LAYOUT_WITH_SIZE_SPEC);
    LAYOUT_SPEC_DELEGATE_METHODS_MAP.put(OnCreateInitialState.class, ON_CREATE_INITIAL_STATE);
    LAYOUT_SPEC_DELEGATE_METHODS_MAP.put(OnLayoutTransition.class, ON_LAYOUT_TRANSITION);
  }
}
