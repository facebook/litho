/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.components.javapoet.JPUtil;
import com.facebook.components.processor.GetTreePropsForChildrenMethodBuilder.CreateTreePropMethodData;
import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.PropDefaultModel;
import com.facebook.components.specmodels.processor.PropDefaultsExtractor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import static com.facebook.components.processor.Utils.capitalize;
import static com.facebook.components.processor.Visibility.PRIVATE;
import static com.facebook.components.specmodels.generator.GeneratorConstants.DELEGATE_FIELD_NAME;
import static com.facebook.components.specmodels.generator.GeneratorConstants.SPEC_INSTANCE_NAME;
import static java.util.Arrays.asList;
import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.DOUBLE;
import static javax.lang.model.type.TypeKind.FLOAT;
import static javax.lang.model.type.TypeKind.TYPEVAR;
import static javax.lang.model.type.TypeKind.VOID;

public class Stages {

  public static final String IMPL_CLASS_NAME_SUFFIX = "Impl";
  private static final String INNER_IMPL_BUILDER_CLASS_NAME = "Builder";
  private static final String STATE_UPDATE_IMPL_NAME_SUFFIX = "StateUpdate";
  public static final String STATE_CONTAINER_IMPL_NAME_SUFFIX = "StateContainerImpl";
  public static final String STATE_CONTAINER_IMPL_MEMBER = "mStateContainerImpl";

  private static final String REQUIRED_PROPS_NAMES = "REQUIRED_PROPS_NAMES";
  private static final String REQUIRED_PROPS_COUNT = "REQUIRED_PROPS_COUNT";

  private static final int ON_STYLE_PROPS = 1;
  private static final int ON_CREATE_INITIAL_STATE = 1;

  private final boolean mSupportState;

  public enum StaticFlag {
    STATIC,
    NOT_STATIC
  }

  public enum StyleableFlag {
    STYLEABLE,
    NOT_STYLEABLE
  }

  // Using these names in props might cause conflicts with the method names in the
  // component's generated layout builder class so we trigger a more user-friendly
  // error if the component tries to use them. This list should be kept in sync
  // with BaseLayoutBuilder.
  private static final String[] RESERVED_PROP_NAMES = new String[] {
      "withLayout",
      "key",
      "loadingEventHandler",
  };

  private static final Class<Annotation>[] TREE_PROP_ANNOTATIONS = new Class[] {
      TreeProp.class,
  };

  private static final Class<Annotation>[] PROP_ANNOTATIONS = new Class[] {
      Prop.class,
  };

  private static final Class<Annotation>[] STATE_ANNOTATIONS = new Class[] {
      State.class,
  };

  private final ProcessingEnvironment mProcessingEnv;

  private final TypeElement mSourceElement;
  private final String mQualifiedClassName;
  private final Class<Annotation>[] mStageAnnotations;
  private final Class<Annotation>[] mInterStagePropAnnotations;
  private final Class<Annotation>[] mParameterAnnotations;
  private final TypeSpec.Builder mClassTypeSpec;
  private final List<TypeVariableName> mTypeVariables;
  private final List<TypeElement> mEventDeclarations;
  private final Map<String, String> mPropJavadocs;

  private final String mSimpleClassName;

  private String mSourceDelegateAccessorName = DELEGATE_FIELD_NAME;

  private List<VariableElement> mProps;
  private List<VariableElement> mOnCreateInitialStateDefinedProps;
  private ImmutableList<PropDefaultModel> mPropDefaults;
  private List<VariableElement> mTreeProps;
  private final Map<String, VariableElement> mStateMap = new LinkedHashMap<>();

  // Map of name to VariableElement, for members of the inner implementation class, in order
  private LinkedHashMap<String, VariableElement> mImplMembers;
  private List<Parameter> mImplParameters;

  private final Map<String, TypeMirror> mExtraStateMembers;

  // List of methods that have @OnEvent on it.
  private final List<ExecutableElement> mOnEventMethods;

  // List of methods annotated with @OnUpdateState.
  private final List<ExecutableElement> mOnUpdateStateMethods;

  private final List<ExecutableElement> mOnCreateTreePropsMethods;

  // List of methods that define stages (e.g. OnCreateLayout)
  private List<ExecutableElement> mStages;

  public TypeElement getSourceElement() {
    return mSourceElement;
  }

  public Stages(
      ProcessingEnvironment processingEnv,
      TypeElement sourceElement,
      String qualifiedClassName,
      Class<Annotation>[] stageAnnotations,
      Class<Annotation>[] interStagePropAnnotations,
      TypeSpec.Builder typeSpec,
      List<TypeVariableName> typeVariables,
      boolean supportState,
      Map<String, TypeMirror> extraStateMembers,
      List<TypeElement> eventDeclarations,
      Map<String, String> propJavadocs) {
    mProcessingEnv = processingEnv;
    mSourceElement = sourceElement;
    mQualifiedClassName = qualifiedClassName;
    mStageAnnotations = stageAnnotations;
    mInterStagePropAnnotations = interStagePropAnnotations;
    mClassTypeSpec = typeSpec;
    mTypeVariables = typeVariables;
    mEventDeclarations = eventDeclarations;
    mPropJavadocs = propJavadocs;

    final List<Class<Annotation>> parameterAnnotations = new ArrayList<>();
    parameterAnnotations.addAll(asList(PROP_ANNOTATIONS));
    parameterAnnotations.addAll(asList(STATE_ANNOTATIONS));
    parameterAnnotations.addAll(asList(mInterStagePropAnnotations));
    parameterAnnotations.addAll(asList(TREE_PROP_ANNOTATIONS));
    mParameterAnnotations = parameterAnnotations.toArray(
        new Class[parameterAnnotations.size()]);

    mSupportState = supportState;
    mSimpleClassName = Utils.getSimpleClassName(mQualifiedClassName);
    mOnEventMethods = Utils.getAnnotatedMethods(mSourceElement, OnEvent.class);
    mOnUpdateStateMethods = Utils.getAnnotatedMethods(mSourceElement, OnUpdateState.class);
    mOnCreateTreePropsMethods = Utils.getAnnotatedMethods(mSourceElement, OnCreateTreeProp.class);

    mExtraStateMembers = extraStateMembers;
    validateOnEventMethods();

    populatePropDefaults();
    populateStages();
    validateAnnotatedParameters();
    populateOnCreateInitialStateDefinedProps();
    populateProps();
    populateTreeProps();
    if (mSupportState) {
      populateStateMap();
    }
    validatePropDefaults();
    populateImplMembers();
    populateImplParameters();
    validateStyleOutputs();
  }

  private boolean isInterStagePropAnnotationValidInStage(
      Class<? extends Annotation> interStageProp,
      Class<? extends Annotation> stage) {
    final int interStagePropIndex = asList(mInterStagePropAnnotations).indexOf(interStageProp);
    final int stageIndex = asList(mStageAnnotations).indexOf(stage);
    if (interStagePropIndex < 0 || stageIndex < 0) {
      throw new IllegalArgumentException(); // indicates bug in the annotation processor
    }

    // This logic relies on the fact that there are prop annotations for each stage (except for
    // some number at the end)
    return interStagePropIndex < stageIndex;
  }

  private boolean doesInterStagePropAnnotationMatchStage(
      Class<? extends Annotation> interStageProp,
      Class<? extends Annotation> stage) {
    final int interStagePropIndex = asList(mInterStagePropAnnotations).indexOf(interStageProp);

    // Null stage is allowed and indicates prop
    int stageIndex = -1;
    if (stage != null) {
      stageIndex = asList(mStageAnnotations).indexOf(stage);
      if (interStagePropIndex < 0 || stageIndex < 0) {
        throw new IllegalArgumentException(); // indicates bug in the annotation processor
      }
    }

    return interStagePropIndex == stageIndex;
  }

  private void validateOnEventMethods() {
    final Map<String, Boolean> existsMap = new HashMap<>();
    for (ExecutableElement element : mOnEventMethods) {
      if (existsMap.containsKey(element.getSimpleName().toString())) {
        throw new ComponentsProcessingException(
            element,
            "@OnEvent declared methods must have unique names");
      }

      final DeclaredType eventClass = Utils.getAnnotationParameter(
          mProcessingEnv,
          element,
          OnEvent.class,
          "value");

      final TypeMirror returnType = Utils.getAnnotationParameter(
          mProcessingEnv,
          eventClass.asElement(),
          Event.class,
          "returnType");

      if (!mProcessingEnv.getTypeUtils().isSameType(element.getReturnType(), returnType)) {
        throw new ComponentsProcessingException(
            element,
            "Method " + element.getSimpleName() + " must return " + returnType +
                ", since that is what " + eventClass + " expects.");
      }

      final List<? extends VariableElement> parameters =
          Utils.getEnclosedFields((TypeElement) eventClass.asElement());

      for (VariableElement v : Utils.getParametersWithAnnotation(element, FromEvent.class)) {
        boolean hasMatchingParameter = false;
        for (VariableElement parameter : parameters) {
          if (parameter.getSimpleName().equals(v.getSimpleName()) &&
              parameter.asType().toString().equals(v.asType().toString())) {
            hasMatchingParameter = true;
            break;
          }
        }

        if (!hasMatchingParameter) {
          throw new ComponentsProcessingException(
              v,
              v.getSimpleName() + " of this type is not a member of " +
                  eventClass);
        }

        return;
      }

      existsMap.put(element.getSimpleName().toString(), true);
    }
  }

  /**
   * Ensures that the declared events don't clash with the predefined ones.
   */
  private void validateEventDeclarations() {
    for (TypeElement eventDeclaration : mEventDeclarations) {
      final Event eventAnnotation = eventDeclaration.getAnnotation(Event.class);
      if (eventAnnotation == null) {
        throw new ComponentsProcessingException(
            eventDeclaration,
            "Events must be declared with the @Event annotation, event is: " + eventDeclaration);
      }

      final List<? extends VariableElement> fields = Utils.getEnclosedFields(eventDeclaration);
      for (VariableElement field : fields) {
        if (!field.getModifiers().contains(Modifier.PUBLIC) ||
            field.getModifiers().contains(Modifier.FINAL)) {
          throw new ComponentsProcessingException(
              field,
              "Event fields must be declared as public non-final");
        }
      }
    }
  }

