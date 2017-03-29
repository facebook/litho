/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

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
import com.facebook.litho.javapoet.JPUtil;
import com.facebook.litho.processor.GetTreePropsForChildrenMethodBuilder.CreateTreePropMethodData;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.facebook.litho.specmodels.processor.PropDefaultsExtractor;

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

import static com.facebook.litho.processor.Utils.capitalize;
import static com.facebook.litho.processor.Visibility.PRIVATE;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.DELEGATE_FIELD_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.SPEC_INSTANCE_NAME;
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

  private void validateStyleOutputs() {
    final ExecutableElement delegateMethod = Utils.getAnnotatedMethod(
        mSourceElement,
        OnLoadStyle.class);
    if (delegateMethod == null) {
      return;
    }

    final List<? extends VariableElement> parameters = delegateMethod.getParameters();

    if (parameters.size() < ON_STYLE_PROPS) {
      throw new ComponentsProcessingException(
          delegateMethod,
          "The @OnLoadStyle method should have an ComponentContext" +
              "followed by Output parameters matching component create.");
    }

    final TypeName firstParamType = ClassName.get(parameters.get(0).asType());
    if (!firstParamType.equals(ClassNames.COMPONENT_CONTEXT)) {
      throw new ComponentsProcessingException(
          parameters.get(0),
          "The first argument of the @OnLoadStyle method should be an ComponentContext.");
    }

    for (int i = ON_STYLE_PROPS, size = parameters.size(); i < size; i++) {
      final VariableElement v = parameters.get(i);
      final TypeMirror outputType = Utils.getGenericTypeArgument(v.asType(), ClassNames.OUTPUT);

      if (outputType == null) {
        throw new ComponentsProcessingException(
            parameters.get(i),
            "The @OnLoadStyle method should have only have Output arguments matching " +
                "component create.");
      }

      final Types typeUtils = mProcessingEnv.getTypeUtils();
      final String name = v.getSimpleName().toString();

      boolean matchesProp = false;
      for (Element prop : mProps) {
        if (!prop.getSimpleName().toString().equals(name)) {
          continue;
        }

        matchesProp = true;

        if (!typeUtils.isAssignable(prop.asType(), outputType)) {
          throw new ComponentsProcessingException(
              v,
              "Searching for prop \"" + name + "\" of type " + ClassName.get(outputType) +
                  " but found prop with the same name of type " +
                  ClassName.get(prop.asType()));
        }
      }

      if (!matchesProp) {
        throw new ComponentsProcessingException(
            v,
            "Output named '" + v.getSimpleName() + "' does not match any prop " +
                "in the component.");
      }
    }
  }

  /**
   * Validate that:
   * <ul>
   *   <li>1. Parameters are consistently typed across stages.</li>
   *   <li>2. Outputs for the same parameter name aren't duplicated.</li>
   *   <li>3. Declared inter-stage prop parameters from previous stages (i.e. not
   *   {@link Prop}) correspond to outputs from that stage</li>
   *   <li>4. Inter-stage prop parameters come from previous stages. i.e. It is illegal to declare
   *   a @FromMeasure parameter in @OnInflate</li>
   *   <li>5. Inter-stage parameters don't have duplicate annotations (and that outputs aren't
   *   annotated as inter-stage props)</li>
   *   <li>6. Ensure props don't use reserved words as names.</li>
   *   <li>7. Ensure required props don't have default values.</li>
   *   <li>8. Ensure same props are annotated identically</li>
   *   <li>9. Ensure props are of legal types</li>
   * </ul>
   */
  private void validateAnnotatedParameters() {
    final List<PrintableException> exceptions = new ArrayList<>();
    final Map<String, VariableElement> variableNameToElementMap = new HashMap<>();
    final Map<String, Class<? extends Annotation>> outputVariableToStage = new HashMap<>();

    for (Class<? extends Annotation> stageAnnotation : mStageAnnotations) {
      final ExecutableElement stage = Utils.getAnnotatedMethod(
          mSourceElement,
          stageAnnotation);
      if (stage == null) {
        continue;
      }

      // Enforce #5: getSpecDefinedParameters will verify that parameters don't have duplicate
      // annotations
      for (VariableElement v : getSpecDefinedParameters(stage)) {
        try {
          final String variableName = v.getSimpleName().toString();
          final Annotation interStagePropAnnotation = getInterStagePropAnnotation(v);
          final boolean isOutput =
              Utils.getGenericTypeArgument(v.asType(), ClassNames.OUTPUT) != null;
          if (isOutput) {
            outputVariableToStage.put(variableName, stageAnnotation);
          }

          // Enforce #3
          if (interStagePropAnnotation != null) {
            final Class<? extends Annotation> outputStage = outputVariableToStage.get(variableName);
            if (!doesInterStagePropAnnotationMatchStage(
                interStagePropAnnotation.annotationType(), outputStage)) {
              throw new ComponentsProcessingException(
                  v,
                  "Inter-stage prop declaration is incorrect, the same name and type must be " +
                      "used in every method where the inter-stage prop is declared.");
            }
          }

          // Enforce #4
          if (interStagePropAnnotation != null
              && !isInterStagePropAnnotationValidInStage(
              interStagePropAnnotation.annotationType(), stageAnnotation)) {
            throw new ComponentsProcessingException(
                v,
                "Inter-stage create must refer to previous stages.");
          }

          final VariableElement existingType = variableNameToElementMap.get(variableName);
          if (existingType != null && !isSameType(existingType.asType(), v.asType())) {
            // We have a type mis-match. This is allowed, provided that the previous type is an
            // outputand the new type is an prop, and the type argument of the output matches the
            // prop. In the future, we may want to allow stages to modify outputs from previous
            // stages, but for now we disallow it.

            // Enforce #1 and #2
            if ((getInterStagePropAnnotation(v) == null ||
                Utils.getGenericTypeArgument(existingType.asType(), ClassNames.OUTPUT) == null) &&
                Utils.getGenericTypeArgument(existingType.asType(), ClassNames.DIFF) == null) {
              throw new ComponentsProcessingException(
                  v,
                  "Inconsistent type for '" + variableName + "': '" + existingType.asType() +
                      "' and '" + v.asType() + "'");
            }
          } else if (existingType == null) {
            // We haven't see a parameter with this name yet. Therefore it must be either @Prop,
            // @State or an output.
            final boolean isFromProp = getParameterAnnotation(v, PROP_ANNOTATIONS) != null;
            final boolean isFromState = getParameterAnnotation(v, STATE_ANNOTATIONS) != null;
            final boolean isFromTreeProp
                = getParameterAnnotation(v, TREE_PROP_ANNOTATIONS) != null;

            if (isFromState && !mSupportState) {
              throw new ComponentsProcessingException(
                  v,
                  "State is not supported in this kind of Spec.");
            }

            if (!isFromProp && !isFromState && !isOutput && !isFromTreeProp) {
              throw new ComponentsProcessingException(
                  v,
                  "Inter-stage prop declared without source.");
            }
          }

          // Enforce #6
          final Prop propAnnotation = v.getAnnotation(Prop.class);
          if (propAnnotation != null) {
            for (String reservedPropName : RESERVED_PROP_NAMES) {
              if (reservedPropName.equals(variableName)) {
                throw new ComponentsProcessingException(
                    v,
                    "'" + reservedPropName + "' is a reserved prop name used by " +
                        "the component's layout builder. Please use another name.");
              }
            }

            // Enforce #7
            final boolean hasDefaultValue = hasDefaultValue(v);
            if (hasDefaultValue && !propAnnotation.optional()) {
              throw new ComponentsProcessingException(
                  v,
                  "Prop is not optional but has a declared default value.");
            }

            // Enforce #8
            if (existingType != null) {
              final Prop existingPropAnnotation = existingType.getAnnotation(Prop.class);
              if (existingPropAnnotation != null) {
                if (!hasSameAnnotations(v, existingType)) {
                  throw new ComponentsProcessingException(
                      v,
                      "The prop '" + variableName + "' is configured differently for different " +
                          "methods. Ensure each instance of this prop is declared identically.");
                }
              }
            }

            // Enforce #9
            TypeName typeName;
            try {
              typeName = ClassName.get(v.asType());
            } catch (IllegalArgumentException e) {
              throw new ComponentsProcessingException(
                  v,
                  "Prop type does not exist");
            }

            // Enforce #10
            final List<ClassName> illegalPropTypes = Arrays.asList(
                ClassNames.COMPONENT_LAYOUT,
                ClassNames.COMPONENT_LAYOUT_BUILDER,
                ClassNames.COMPONENT_LAYOUT_CONTAINER_BUILDER,
                ClassNames.COMPONENT_BUILDER,
                ClassNames.COMPONENT_BUILDER_WITH_LAYOUT,
                ClassNames.REFERENCE_BUILDER);
            if (illegalPropTypes.contains(typeName)) {
              throw new ComponentsProcessingException(
                  v,
                  "Props may not be declared with the following types:" +
                      illegalPropTypes);
            }
          }

          variableNameToElementMap.put(variableName, v);
        } catch (PrintableException e) {
          exceptions.add(e);
        }
      }
    }

    if (!exceptions.isEmpty()) {
      throw new MultiPrintableException(exceptions);
    }
  }

  private boolean hasSameAnnotations(VariableElement v1, VariableElement v2) {
    final List<? extends AnnotationMirror> v1Annotations = v1.getAnnotationMirrors();
    final List<? extends AnnotationMirror> v2Annotations = v2.getAnnotationMirrors();

    if (v1Annotations.size() != v2Annotations.size()) {
      return false;
    }

    final int count = v1Annotations.size();
    for (int i = 0; i < count; i++) {
      final AnnotationMirror a1 = v1Annotations.get(i);
      final AnnotationMirror a2 = v2Annotations.get(i);

      // Some object in this hierarchy don't implement equals correctly.
      // They do however produce very nice strings representations which we can compare instead.
      if (!a1.toString().equals(a2.toString())) {
        return false;
      }
    }

    return true;
  }

  public void validateStatic() {
    validateStaticFields();
    validateStaticMethods();
  }

  private void validateStaticFields() {
    for (Element element : mSourceElement.getEnclosedElements()) {
      if (element.getKind() == ElementKind.FIELD &&
          !element.getModifiers().contains(Modifier.STATIC)) {
        throw new ComponentsProcessingException(
            element,
            "Field " + element.getSimpleName() + " in " + mSourceElement.getQualifiedName() +
                " must be static");
      }
    }
  }

  private void validateStaticMethods() {
    for (Class<? extends Annotation> stageAnnotation : mStageAnnotations) {
      final ExecutableElement stage = Utils.getAnnotatedMethod(
          mSourceElement,
          stageAnnotation);
      if (stage != null && !stage.getModifiers().contains(Modifier.STATIC)) {
        throw new ComponentsProcessingException(
            stage,
            "Method " + stage.getSimpleName() + " in " + mSourceElement.getQualifiedName() +
                " must be static");
      }
    }
  }

  /**
   * Gather a list of VariableElement that are the props to this component
   */
  private void populateProps() {
    // We use a linked hash map to guarantee iteration order
    final LinkedHashMap<String, VariableElement> variableNameToElementMap = new LinkedHashMap<>();

    for (ExecutableElement stage : mStages) {
      for (VariableElement v : getProps(stage)) {
        // Validation unnecessary - already handled by validateAnnotatedParameters
        final String variableName = v.getSimpleName().toString();
        variableNameToElementMap.put(variableName, v);
      }
    }

    mProps = new ArrayList<>(variableNameToElementMap.values());
    addCreateInitialStateDefinedProps(mProps);
  }

  /**
   * Gather a list of VariableElement that are the state to this component
   */
  private void populateStateMap() {
    // We use a linked hash map to guarantee iteration order
    final LinkedHashMap<String, VariableElement> variableNameToElementMap = new LinkedHashMap<>();

    for (ExecutableElement stage : mStages) {
      for (VariableElement v : getState(stage)) {

        final String variableName = v.getSimpleName().toString();

        if (mStateMap.containsKey(variableName)) {
          VariableElement existingType = mStateMap.get(variableName);
          final State existingPropAnnotation = existingType.getAnnotation(State.class);
          if (existingPropAnnotation != null) {
            if (!hasSameAnnotations(v, existingType)) {
              throw new ComponentsProcessingException(
                  v,
                  "The state '" + variableName + "' is configured differently for different " +
                      "methods. Ensure each instance of this state is declared identically.");
            }
          }
        }

        mStateMap.put(
            variableName,
            v);
      }
    }
  }

  private void populateTreeProps() {
    final LinkedHashMap<String, VariableElement> variableNameToElementMap = new LinkedHashMap<>();

    for (ExecutableElement stage : mStages) {
      for (VariableElement v : Utils.getParametersWithAnnotation(stage, TreeProp.class)) {
        final String variableName = v.getSimpleName().toString();
        variableNameToElementMap.put(variableName, v);
      }
    }

    mTreeProps = new ArrayList<>(variableNameToElementMap.values());
  }

  /**
   * Get the list of stages (OnInflate, OnMeasure, OnMount) that are defined for this component.
   */
  private void populateStages() {
    mStages = new ArrayList<>();
    for (Class<Annotation> stageAnnotation : mStageAnnotations) {
      final ExecutableElement stage = Utils.getAnnotatedMethod(
          mSourceElement,
          stageAnnotation);
      if (stage != null) {
        mStages.add(stage);
      }
    }

    if (mOnEventMethods != null) {
      mStages.addAll(mOnEventMethods);
    }
    mStages.addAll(mOnCreateTreePropsMethods);
  }

  /**
   * @param prop The prop to determine if it has a default or not.
   * @return Returns true if the prop has a default, false otherwise.
