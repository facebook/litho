/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.DELEGATE_FIELD_NAME;
import static java.util.Arrays.asList;

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
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.facebook.litho.specmodels.processor.ComponentsProcessingException;
import com.facebook.litho.specmodels.processor.MultiPrintableException;
import com.facebook.litho.specmodels.processor.PrintableException;
import com.facebook.litho.specmodels.processor.PropDefaultsExtractor;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
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

public class Stages {

  public static final String IMPL_CLASS_NAME_SUFFIX = "Impl";
  private static final String STATE_UPDATE_IMPL_NAME_SUFFIX = "StateUpdate";
  public static final String STATE_CONTAINER_IMPL_NAME_SUFFIX = "StateContainerImpl";
  public static final String STATE_CONTAINER_IMPL_MEMBER = "mStateContainerImpl";

  private static final int ON_STYLE_PROPS = 1;
  private static final int ON_CREATE_INITIAL_STATE = 1;

  private final boolean mSupportState;

  public enum StaticFlag {
    STATIC,
    NOT_STATIC
  }

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
  private List<PropDefaultModel> mPropDefaults;
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

    populatePropDefaults();
    populateStages();
    populateOnCreateInitialStateDefinedProps();
    populateProps();
    populateTreeProps();
    if (mSupportState) {
      populateStateMap();
    }
    populateImplMembers();
    populateImplParameters();
    validateStyleOutputs();
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
   */
  private boolean hasDefaultValue(VariableElement prop) {
    final String name = prop.getSimpleName().toString();
    final TypeName type = TypeName.get(prop.asType());
    for (PropDefaultModel propDefault : mPropDefaults) {
      if (propDefault.mName.equals(name) && propDefault.mType.equals(type)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gather a list of parameters from the given element that are props to this component.
   */
  private static List<VariableElement> getProps(ExecutableElement element) {
    return Utils.getParametersWithAnnotation(element, Prop.class);
  }

  /**
   * Gather a list of parameters from the given element that are state to this component.
   */
  private static List<VariableElement> getState(ExecutableElement element) {
    return Utils.getParametersWithAnnotation(element, State.class);
  }

  /**
   * Gather a list of parameters from the given element that are defined by the spec. That is, they
   * aren't one of the parameters predefined for a given method. For example, OnCreateLayout has a
   * predefined parameter of type LayoutContext. Spec-defined parameters are annotated with one of
   * our prop annotations or are of type {@link com.facebook.litho.Output}.
   */
  private List<VariableElement> getSpecDefinedParameters(ExecutableElement element) {
    return getSpecDefinedParameters(element, true);
  }

  private List<VariableElement> getSpecDefinedParameters(
      ExecutableElement element,
      boolean shouldIncludeOutputs) {
    final ArrayList<VariableElement> specDefinedParameters = new ArrayList<>();
    for (VariableElement v : element.getParameters()) {
      final boolean isAnnotatedParameter = getParameterAnnotation(v) != null;
      final boolean isInterStageOutput = Utils.getGenericTypeArgument(
          v.asType(),
          ClassNames.OUTPUT) != null;
      if (isAnnotatedParameter && isInterStageOutput) {
        throw new ComponentsProcessingException(
            v,
            "Variables that are both prop and output are forbidden.");
      } else if (isAnnotatedParameter || (shouldIncludeOutputs && isInterStageOutput)) {
        specDefinedParameters.add(v);
      }
    }

    return specDefinedParameters;
  }

  private void populateOnCreateInitialStateDefinedProps() {
    final ExecutableElement onCreateInitialState = Utils.getAnnotatedMethod(
        getSourceElement(),
        OnCreateInitialState.class);

    if (onCreateInitialState == null) {
      mOnCreateInitialStateDefinedProps = new ArrayList<>();
    } else {
      mOnCreateInitialStateDefinedProps = getSpecDefinedParameters(onCreateInitialState, false);
    }
  }

  /**
   * Get the annotation, if any, present on a parameter. Annotations are restricted to our whitelist
   * of parameter annotations: e.g. {@link Prop}, {@link State} etc)
   */
  private Annotation getParameterAnnotation(VariableElement element) {
    return getParameterAnnotation(element, mParameterAnnotations);
  }

  /**
   * Get the annotation, if any, present on a parameter. Annotations are restricted to the specified
   * whitelist. If there is a duplicate we will issue an error.
   */
  private Annotation getParameterAnnotation(
      VariableElement element,
      Class<Annotation>[] possibleAnnotations) {
    final ArrayList<Annotation> annotations = new ArrayList<>();
    for (Class<Annotation> annotationClass : possibleAnnotations) {
      final Annotation annotation = element.getAnnotation(annotationClass);
      if (annotation != null) {
        annotations.add(annotation);
      }
    }
    if (annotations.isEmpty()) {
      return null;
    } else if (annotations.size() == 1) {
      return annotations.get(0);
    } else {
      throw new ComponentsProcessingException(
          element,
          "Duplicate parameter annotation: '" + annotations.get(0) + "' and '" +
              annotations.get(1) + "'");
    }
  }

  /**
   * Generate javadoc block describing component props.
   */
  public void generateJavadoc() {
    for (VariableElement v : mProps) {
      final Prop propAnnotation = v.getAnnotation(Prop.class);
      final String propTag = propAnnotation.optional() ? "@prop-optional" : "@prop-required";
      final String javadoc =
          mPropJavadocs != null ? mPropJavadocs.get(v.getSimpleName().toString()) : "";

      final String sanitizedJavadoc =
          javadoc != null ? javadoc.replace('\n', ' ') : null;

      // Adds javadoc with following format:
      // @prop-required name type javadoc.
      // This can be changed later to use clear demarcation for fields.
      // This is a block tag and cannot support inline tags like "{@link something}".
      mClassTypeSpec.addJavadoc(
          "$L $L $L $L\n",
          propTag,
          v.getSimpleName().toString(),
          Utils.getTypeName(v.asType()),
          sanitizedJavadoc);
    }
  }

  public void generateSourceDelegate(boolean initialized) {
    final ClassName specClassName = ClassName.get(mSourceElement);
    generateSourceDelegate(initialized, specClassName);
  }

  public void generateSourceDelegate(boolean initialized, TypeName specTypeName) {
    final FieldSpec.Builder builder = FieldSpec
        .builder(specTypeName, DELEGATE_FIELD_NAME)
        .addModifiers(Modifier.PRIVATE);

    if (initialized) {
      builder.initializer("new $T()", specTypeName);
    }

    mClassTypeSpec.addField(builder.build());
  }

  private MethodSpec generateMakeShallowCopy(ClassName componentClassName, boolean hasDeepCopy) {
    final List<String> componentsInImpl = findComponentsInImpl(componentClassName);
    final List<String> interStageComponentVariables = getInterStageVariableNames();

    if (componentsInImpl.isEmpty() &&
        interStageComponentVariables.isEmpty() &&
        mOnUpdateStateMethods.isEmpty()) {
      return null;
    }

    final String implClassName = getImplClassName();

    return new ShallowCopyMethodSpecBuilder()
        .componentsInImpl(componentsInImpl)
        .interStageVariables(interStageComponentVariables)
        .implClassName(implClassName)
        .hasDeepCopy(hasDeepCopy)
        .stateContainerImplClassName(getStateContainerImplClassName())
        .build();
  }

  private List<String> findComponentsInImpl(ClassName listComponent) {
    final List<String> componentsInImpl = new ArrayList<>();

    for (String key : mImplMembers.keySet()) {
      final VariableElement element = mImplMembers.get(key);
      final Name declaredClassName = Utils.getDeclaredClassNameWithoutGenerics(element);
      if (declaredClassName != null &&
          ClassName.bestGuess(declaredClassName.toString()).equals(listComponent)) {
        componentsInImpl.add(element.getSimpleName().toString());
      }
    }

    return componentsInImpl;
  }

  /**
   * Generates a method to create the initial values for parameters annotated with {@link State}.
   * This method also validates that the delegate method only tries to assign an initial value to
   * State annotated parameters.
   */
  public void generateCreateInitialState(
      ExecutableElement from,
      ClassName contextClass,
      ClassName componentClass) {

    verifyParametersForCreateInitialState(contextClass, from);

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.returnType = null;
    methodDescription.name = "createInitialState";
    methodDescription.parameterTypes = new TypeName[] {contextClass};

    generateDelegate(methodDescription, from, componentClass);
  }

  private void verifyParametersForCreateInitialState(
      ClassName contextClass,
      ExecutableElement executableElement) {
    final List<VariableElement> parameters =
        (List<VariableElement>) executableElement.getParameters();

    if (parameters.size() < ON_CREATE_INITIAL_STATE + 1) {
      throw new ComponentsProcessingException(
          executableElement,
          "The @OnCreateInitialState method should have an " + contextClass +
              "followed by Output parameters matching state parameters.");
    }

    final TypeName firstParamType = ClassName.get(parameters.get(0).asType());
    if (!firstParamType.equals(contextClass)) {
      throw new ComponentsProcessingException(
          parameters.get(0),
          "The first argument of the @OnCreateInitialState method should be an " +
              contextClass + ".");
    }

    for (int i = ON_CREATE_INITIAL_STATE, size = parameters.size(); i < size; i++) {
      final VariableElement element = parameters.get(i);
      final TypeMirror elementInnerClassType =
          Utils.getGenericTypeArgument(element.asType(), ClassNames.STATE_VALUE);

      if (elementInnerClassType != null) {
        final String paramName = element.getSimpleName().toString();
        VariableElement implParameter = mStateMap.get(paramName);

        if (implParameter == null || implParameter.getAnnotation(State.class) == null) {
          throw new ComponentsProcessingException(
              executableElement,
              "Only parameters annotated with @State can be initialized in @OnCreateInitialState," +
                  " parameter without annotation is: " + paramName);
        }
      }
    }
  }

  /**
   * Generate a method implementation that delegates to another method that takes annotated props.
   *
   * @param from description of method signature to be generated
   * @param to method to which to delegate
   * @param propsClass Component / Delegate. The base class of the inner implementation object
   * @throws java.io.IOException If one of the writer methods throw
   */
  public void generateDelegate(
      MethodDescription from,
      ExecutableElement to,
      ClassName propsClass) {
    generateDelegate(
        from,
        to,
        Collections.<TypeName>emptyList(),
        Collections.<String, String>emptyMap(),
        propsClass);
  }

  /**
   * Generate a method implementation that delegates to another method that takes annotated props.
   *
   * @param from description of method signature to be generated
   * @param to method to which to delegate
   * @param propsClass Component / Delegate. The base class of the inner implementation object
   * @throws java.io.IOException If one of the writer methods throw
   */
  public void generateDelegate(
      MethodDescription from,
      ExecutableElement to,
      List<TypeName> expectedTypes,
      Map<String, String> parameterTranslation,
      ClassName propsClass) {

    final Visibility visibility;
    if (Arrays.asList(from.accessType).contains(Modifier.PRIVATE)) {
      visibility = Visibility.PRIVATE;
    } else if (Arrays.asList(from.accessType).contains(Modifier.PROTECTED)) {
      visibility = Visibility.PROTECTED;
    } else if (Arrays.asList(from.accessType).contains(Modifier.PUBLIC)) {
      visibility = Visibility.PUBLIC;
    } else {
      visibility = Visibility.PACKAGE;
    }

    final List<Parameter> toParams = getParams(to);
    final List<Parameter> fromParams = new ArrayList<>();
    for (int i = 0; i < from.parameterTypes.length; i++) {
      fromParams.add(new Parameter(from.parameterTypes[i], toParams.get(i).name));
    }

    final List<PrintableException> errors = new ArrayList<>();
    for (int i = 0; i < expectedTypes.size(); i++) {
      if (!toParams.get(i).type.equals(expectedTypes.get(i))) {
        errors.add(new ComponentsProcessingException(
            to.getParameters().get(i),
            "Expected " + expectedTypes.get(i)));
      }
    }

    if (!errors.isEmpty()) {
      throw new MultiPrintableException(errors);
    }

    writeMethodSpec(new DelegateMethodSpecBuilder()
        .implClassName(getImplClassName())
        .abstractImplType(propsClass)
        .implParameters(mImplParameters)
        .checkedExceptions(
            from.exceptions == null ?
                new ArrayList<TypeName>() :
                Arrays.asList(from.exceptions))
        .overridesSuper(
            from.annotations != null && Arrays.asList(from.annotations).contains(Override.class))
        .parameterTranslation(parameterTranslation)
        .visibility(visibility)
        .fromName(from.name)
        .fromReturnType(from.returnType == null ? TypeName.VOID : from.returnType)
        .fromParams(fromParams)
        .target(mSourceDelegateAccessorName)
        .toName(to.getSimpleName().toString())
        .stateParams(mStateMap.keySet())
        .toReturnType(ClassName.get(to.getReturnType()))
        .toParams(toParams)
        .build());
  }

  /**
   * Returns {@code true} if the given types match.
   */
  public boolean isSameType(TypeMirror a, TypeMirror b) {
    return mProcessingEnv.getTypeUtils().isSameType(a, b);
  }

  /**
   * Generate the static methods of the Component that can be called to update its state.
   */
  public void generateOnStateUpdateMethods(
      ClassName contextClass,
      ClassName componentClassName,
      ClassName stateContainerClassName,
      ClassName stateUpdateInterface,
      Stages.StaticFlag staticFlag) {
    for (ExecutableElement element : mOnUpdateStateMethods) {
      validateOnStateUpdateMethodDeclaration(element);
      generateStateUpdateClass(
          element,
          componentClassName,
          stateContainerClassName,
          stateUpdateInterface,
          staticFlag);
      generateOnStateUpdateMethods(element, contextClass, componentClassName);
    }
  }

  /**
   * Validate that the declaration of a method annotated with {@link OnUpdateState} is correct:
   * <ul>
   *   <li>1. Method parameters annotated with {@link Param} don't have the same name as parameters
   *    annotated with {@link State} or {@link Prop}.</li>
   *   <li>2. Method parameters not annotated with {@link Param} must be of type
   *    com.facebook.litho.StateValue.</li>
   *   <li>3. Names of method parameters not annotated with {@link Param} must match the name of
   *    a parameter annotated with {@link State}.</li>
   *   <li>4. Type of method parameters not annotated with {@link Param} must match the type of
   *    a parameter with the same name annotated with {@link State}.</li>
   * </ul>
   */
  private void validateOnStateUpdateMethodDeclaration(ExecutableElement element) {
    final List<VariableElement> annotatedParams =
        Utils.getParametersWithAnnotation(element, Param.class);

    // Check #1
    for (VariableElement annotatedParam : annotatedParams) {
      if (mStateMap.get(annotatedParam.getSimpleName().toString()) != null) {
        throw new ComponentsProcessingException(
            annotatedParam,
            "Parameters annotated with @Param should not have the same name as a parameter " +
                "annotated with @State or @Prop");
      }
    }

    final List<VariableElement> params = (List<VariableElement>) element.getParameters();

    for (VariableElement param : params) {
      if (annotatedParams.contains(param)) {
        continue;
      }
      final TypeMirror paramType = param.asType();

      // Check #2
      if (paramType.getKind() != TypeKind.DECLARED) {
        throw new ComponentsProcessingException(
            param,
            "Parameters not annotated with @Param must be of type " +
                "com.facebook.litho.StateValue");
      }
      final DeclaredType paramDeclaredType = (DeclaredType) param.asType();
      final String paramDeclaredTypeName = paramDeclaredType
          .asElement()
          .getSimpleName()
          .toString();

      if (!paramDeclaredTypeName.equals(ClassNames.STATE_VALUE.simpleName())) {
        throw new ComponentsProcessingException(
            "All state parameters must be of type com.facebook.litho.StateValue, " +
                param.getSimpleName() + " is of type " +
                param.asType());
      }

      VariableElement stateMatchingParam = mStateMap.get(param.getSimpleName().toString());

      // Check #3
      if (stateMatchingParam == null || stateMatchingParam.getAnnotation(State.class) == null) {
        throw new ComponentsProcessingException(
            param,
            "Names of parameters of type StateValue must match the name of a parameter annotated " +
                "with @State");
      }

      // Check #4
      final List<TypeMirror> typeArguments =
          (List<TypeMirror>) paramDeclaredType.getTypeArguments();
      if (typeArguments.isEmpty()) {
        throw new ComponentsProcessingException(
            param,
            "Type parameter for a parameter of type StateValue should match the type of " +
                "a parameter with the same name annotated with @State");
      }

      final TypeMirror typeArgument = typeArguments.get(0);
      final TypeName stateMatchingParamTypeName = ClassName.get(stateMatchingParam.asType());

      if (stateMatchingParamTypeName.isPrimitive()) {
        TypeName stateMatchingParamBoxedType = stateMatchingParamTypeName.box();
        if (!stateMatchingParamBoxedType.equals(TypeName.get(typeArgument))) {
          throw new ComponentsProcessingException(
              param,
              "Type parameter for a parameter of type StateValue should match the type of " +
                  "a parameter with the same name annotated with @State");
        }
      }
    }
  }

  private void generateOnStateUpdateMethods(
      ExecutableElement element,
      ClassName contextClass,
      ClassName componentClass) {
    final String methodName = element.getSimpleName().toString();

    final List<VariableElement> updateMethodParamElements =
        Utils.getParametersWithAnnotation(element, Param.class);

    final OnStateUpdateMethodSpecBuilder builder = new OnStateUpdateMethodSpecBuilder()
        .componentClass(componentClass)
        .lifecycleImplClass(mSimpleClassName)
        .stateUpdateClassName(getStateUpdateClassName(element));

    for (VariableElement e : updateMethodParamElements) {
      builder.updateMethodParam(
          new Parameter(ClassName.get(e.asType()), e.getSimpleName().toString()));

      List<TypeMirror> genericArgs = getGenericTypeArguments(e.asType());

      if (genericArgs != null) {
        for (TypeMirror genericArg : genericArgs) {
          builder.typeParameter(genericArg.toString());
        }
      }
    }

    writeMethodSpec(builder
        .updateMethodName(methodName)
        .async(false)
        .contextClass(contextClass)
        .build());

    writeMethodSpec(builder
        .updateMethodName(methodName + "Async")
        .async(true)
        .contextClass(contextClass)
        .build());
  }

  static List<TypeMirror> getGenericTypeArguments(TypeMirror diffType) {
    List<TypeMirror> typeVarArguments = new ArrayList<>();
    if (diffType.getKind() == TypeKind.DECLARED) {
      final DeclaredType parameterDeclaredType = (DeclaredType) diffType;
      final List<? extends TypeMirror> typeArguments = parameterDeclaredType.getTypeArguments();

      for (TypeMirror typeArgument : typeArguments) {
        if (typeArgument.getKind() == TypeKind.TYPEVAR) {
          typeVarArguments.add(typeArgument);
        }
      }
    }
    return typeVarArguments;
  }

  public static List<Parameter> getParams(ExecutableElement e) {
    final List<Parameter> params = new ArrayList<>();
    for (VariableElement v : e.getParameters()) {
      params.add(new Parameter(ClassName.get(v.asType()), v.getSimpleName().toString()));
    }

    return params;
  }

  /**
   * Generates a class that implements {@link com.facebook.litho.ComponentLifecycle} given
   *  a method annotated with {@link OnUpdateState}. The class constructor takes as params all the
   *  params annotated with {@link Param} on the method and keeps them in class members.
   * @param element The method annotated with {@link OnUpdateState}
   */
  private void generateStateUpdateClass(
      ExecutableElement element,
      ClassName componentClassName,
      ClassName stateContainerClassName,
      ClassName updateStateInterface,
      StaticFlag staticFlag) {
    final String stateUpdateClassName = getStateUpdateClassName(element);
    final TypeName implClassName = ClassName.bestGuess(getImplClassName());

    final StateUpdateImplClassBuilder stateUpdateImplClassBuilder =
        new StateUpdateImplClassBuilder()
            .withTarget(mSourceDelegateAccessorName)
            .withSpecOnUpdateStateMethodName(element.getSimpleName().toString())
            .withComponentImplClassName(implClassName)
            .withComponentClassName(componentClassName)
            .withComponentStateUpdateInterface(updateStateInterface)
            .withStateContainerClassName(stateContainerClassName)
            .withStateContainerImplClassName(ClassName.bestGuess(getStateContainerImplClassName()))
            .withStateUpdateImplClassName(stateUpdateClassName)
            .withSpecOnUpdateStateMethodParams(getParams(element))
            .withStateValueParams(getStateValueParams(element))
            .withStaticFlag(staticFlag);

    final List<VariableElement> parametersVarElements =
        Utils.getParametersWithAnnotation(element, Param.class);
    final List<Parameter> parameters = new ArrayList<>();

    for (VariableElement v : parametersVarElements) {
      parameters.add(new Parameter(ClassName.get(v.asType()), v.getSimpleName().toString()));

      for (TypeMirror typeVar : getGenericTypeArguments(v.asType())) {
        stateUpdateImplClassBuilder.typeParameter(typeVar.toString());
      }
    }

    stateUpdateImplClassBuilder.withParamsForStateUpdate(parameters);

    writeInnerTypeSpec(stateUpdateImplClassBuilder.build());
  }

  /**
   * Find variables annotated with {@link PropDefault}
   */
  private void populatePropDefaults() {
    mPropDefaults = PropDefaultsExtractor.getPropDefaults(mSourceElement);
  }

  public void generateLazyStateUpdateMethods(
      ClassName context,
      ClassName componentClass,
      TypeName stateContainerComponent,
      TypeName stateUpdateType) {
    for (VariableElement state : mStateMap.values()) {
      if (!state.getAnnotation(State.class).canUpdateLazily()) {
        continue;
      }

      OnLazyStateUpdateMethodSpecBuilder builder = new OnLazyStateUpdateMethodSpecBuilder()
          .contextClass(context)
          .componentClass(componentClass)
          .stateUpdateType(stateUpdateType)
          .stateName(state.getSimpleName().toString())
          .stateType(ClassName.get(state.asType()))
          .withStateContainerClassName(stateContainerComponent)
          .implClass(getImplClassName())
          .lifecycleImplClass(mSimpleClassName);

      builder.typeParameters(getGenericTypeArguments(state.asType()));

      writeMethodSpec(builder.build());
    }
  }

  private void generateStateContainerImplClass(
      Stages.StaticFlag isStatic,
      ClassName stateContainerClassName) {
    final TypeSpec.Builder stateContainerImplClassBuilder = TypeSpec
        .classBuilder(getStateContainerImplClassName())
        .addSuperinterface(stateContainerClassName);

    if (isStatic.equals(Stages.StaticFlag.STATIC)) {
      stateContainerImplClassBuilder.addModifiers(Modifier.STATIC, Modifier.PRIVATE);
      stateContainerImplClassBuilder.addTypeVariables(mTypeVariables);
    }

    for (String stateName : mStateMap.keySet()) {
      VariableElement v = mStateMap.get(stateName);
      stateContainerImplClassBuilder.addField(getPropFieldSpec(v, true));
    }

    writeInnerTypeSpec(stateContainerImplClassBuilder.build());
  }

  private static MethodSpec generateStateContainerGetter(ClassName stateContainerClassName) {
    return MethodSpec.methodBuilder("getStateContainer")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(stateContainerClassName)
        .addStatement("return " + STATE_CONTAINER_IMPL_MEMBER)
        .build();
  }

  public void generateTransferState(
      ClassName contextClassName,
      ClassName componentClassName,
      ClassName stateContainerClassName) {
    if (!mStateMap.isEmpty()) {
      MethodSpec methodSpec = new TransferStateSpecBuilder()
          .contextClassName(contextClassName)
          .componentClassName(componentClassName)
          .componentImplClassName(getImplClassName())
          .stateContainerClassName(stateContainerClassName)
          .stateContainerImplClassName(getStateContainerImplClassName())
          .stateParameters(mStateMap.keySet())
          .build();

      mClassTypeSpec.addMethod(methodSpec);
    }
  }

  public void generateListComponentImplClass(Stages.StaticFlag isStatic) {
    generateStateContainerImplClass(isStatic, SectionClassNames.STATE_CONTAINER_SECTION);

    final ClassName stateContainerImplClass =
        ClassName.bestGuess(getSimpleClassName() + STATE_CONTAINER_IMPL_NAME_SUFFIX);

    final TypeSpec.Builder stateClassBuilder =
        TypeSpec.classBuilder(getImplClassName())
            .addModifiers(Modifier.PRIVATE)
            .superclass(
                ParameterizedTypeName.get(
                    SectionClassNames.SECTION,
                    ClassName.bestGuess(getSimpleClassName())))
            .addSuperinterface(Cloneable.class);

    if (isStatic.equals(Stages.StaticFlag.STATIC)) {
      stateClassBuilder.addModifiers(Modifier.STATIC);
      stateClassBuilder.addTypeVariables(mTypeVariables);
    }

    stateClassBuilder.addField(stateContainerImplClass, STATE_CONTAINER_IMPL_MEMBER);
    stateClassBuilder.addMethod(generateStateContainerGetter(SectionClassNames.STATE_CONTAINER_SECTION));

    generateComponentClassProps(stateClassBuilder, ClassNames.EVENT_HANDLER);

    stateClassBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addStatement("super(get())")
            .addStatement(STATE_CONTAINER_IMPL_MEMBER + " = new $T()", stateContainerImplClass)
            .build());

    final MethodSpec equalsBuilder = generateEqualsMethodDefinition(false);
    stateClassBuilder.addMethod(equalsBuilder);

    for (ExecutableElement element : mOnUpdateStateMethods) {
      final String stateUpdateClassName = getStateUpdateClassName(element);
      final List<Parameter> parameters = getParamsWithAnnotation(element, Param.class);

      stateClassBuilder.addMethod(
          new CreateStateUpdateInstanceMethodSpecBuilder()
              .parameters(parameters)
              .stateUpdateClass(stateUpdateClassName)
              .build());
    }

    final MethodSpec makeShallowCopy =
        generateMakeShallowCopy(SectionClassNames.SECTION, /* hasDeepCopy */ true);

    if (makeShallowCopy != null) {
      stateClassBuilder.addMethod(makeShallowCopy);
    }
    writeInnerTypeSpec(stateClassBuilder.build());
  }

  private MethodSpec generateEqualsMethodDefinition(boolean shouldCheckId) {
    final String implClassName = getImplClassName();
    final String implInstanceName = getImplInstanceName();

    MethodSpec.Builder equalsBuilder = MethodSpec.methodBuilder("equals")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeName.BOOLEAN)
        .addParameter(TypeName.OBJECT, "other")
        .beginControlFlow("if (this == other)")
        .addStatement("return true")
        .endControlFlow()
        .beginControlFlow("if (other == null || getClass() != other.getClass())")
        .addStatement("return false")
        .endControlFlow()
        .addStatement(implClassName +
            " " +
            implInstanceName +
            " = (" +
            implClassName +
            ") other");
    if (shouldCheckId) {
      equalsBuilder
          .beginControlFlow(
              "if (this.getId() == " + implInstanceName + ".getId())")
          .addStatement("return true")
          .endControlFlow();
    }

    for (VariableElement v : mImplMembers.values()) {
      if (!isState(v)) {
        addCompareStatement(implInstanceName, v, equalsBuilder, false);
      }
    }

    for (VariableElement v : mStateMap.values()) {
      addCompareStatement(implInstanceName, v, equalsBuilder, true);
    }

    equalsBuilder.addStatement("return true");

    return equalsBuilder.build();
  }

  private static void addCompareStatement(
      String implInstanceName,
      VariableElement v,
      MethodSpec.Builder equalsBuilder,
      boolean isState) {
    final TypeMirror variableType = v.asType();

    final TypeMirror outputTypeMirror = Utils.getGenericTypeArgument(
        variableType,
        ClassNames.OUTPUT);

    final TypeMirror diffTypeMirror = Utils.getGenericTypeArgument(
        variableType,
        ClassNames.DIFF);

    final TypeKind variableKind = diffTypeMirror != null ?
        diffTypeMirror.getKind() :
        variableType.getKind();

    String qualifiedName = "";
    if (variableType instanceof DeclaredType) {
      final DeclaredType declaredType = (DeclaredType) variableType;
      qualifiedName = ((TypeElement) declaredType.asElement()).getQualifiedName().toString();
    }

    final String stateContainerMember = isState ? "." + STATE_CONTAINER_IMPL_MEMBER : "";
    final CharSequence thisVarName = isState
        ? STATE_CONTAINER_IMPL_MEMBER + "." + v.getSimpleName()
        : v.getSimpleName();

    final boolean isVariableAComponent =
        variableType.toString().equals(ClassNames.COMPONENT.toString()) ||
            (diffTypeMirror != null
                && diffTypeMirror.toString().equals(ClassNames.COMPONENT.toString()));

    if (outputTypeMirror == null) {
      if (variableKind == TypeKind.FLOAT) {
        equalsBuilder
            .beginControlFlow(
                "if (Float.compare($L, " + implInstanceName + stateContainerMember + ".$L) != 0)",
                thisVarName,
                v.getSimpleName())
            .addStatement("return false")
            .endControlFlow();
      } else if (variableKind == TypeKind.DOUBLE) {
        equalsBuilder
            .beginControlFlow(
                "if (Double.compare($L, " + implInstanceName + stateContainerMember + ".$L) != 0)",
                thisVarName,
                v.getSimpleName())
            .addStatement("return false")
            .endControlFlow();
      } else if (variableType.getKind() == TypeKind.ARRAY) {
        equalsBuilder
            .beginControlFlow(
                "if (!Arrays.equals($L, " + implInstanceName + stateContainerMember + ".$L))",
                thisVarName,
                v.getSimpleName())
            .addStatement("return false")
            .endControlFlow();
      } else if (variableType.getKind().isPrimitive()) {
        equalsBuilder
            .beginControlFlow(
                "if ($L != " + implInstanceName + stateContainerMember + ".$L)",
                thisVarName,
                v.getSimpleName())
            .addStatement("return false")
            .endControlFlow();
      } else if (qualifiedName.equals(ClassNames.REFERENCE)) {
        equalsBuilder
            .beginControlFlow(
                "if (Reference.shouldUpdate($L, " +
                    implInstanceName +
                    stateContainerMember +
                    ".$L))",
                thisVarName,
                v.getSimpleName(),
                v.getSimpleName(),
                v.getSimpleName(),
                v.getSimpleName())
            .addStatement("return false")
            .endControlFlow();
      } else if (isVariableAComponent) {
        equalsBuilder
            .beginControlFlow(
                "if ($L != null ? !$L.isEquivalentTo(" +
                    implInstanceName +
                    stateContainerMember +
                    ".$L) : " +
                    implInstanceName +
                    stateContainerMember +
                    ".$L != null)",
                thisVarName,
                thisVarName,
                v.getSimpleName(),
                v.getSimpleName())
            .addStatement("return false")
            .endControlFlow();
      } else {
        equalsBuilder
            .beginControlFlow(
                "if ($L != null ? !$L.equals(" +
                    implInstanceName +
                    stateContainerMember +
                    ".$L) : " +
                    implInstanceName +
                    stateContainerMember +
                    ".$L != null)",
                thisVarName,
                thisVarName,
                v.getSimpleName(),
                v.getSimpleName())
            .addStatement("return false")
            .endControlFlow();
      }
    }
  }

  private boolean isState(VariableElement v) {
    for (VariableElement find : mStateMap.values()) {
      if (find.getSimpleName().equals(v.getSimpleName())) {
        return true;
      }
    }
    return false;
  }

  private void generateComponentClassProps(
      TypeSpec.Builder implClassBuilder,
      ClassName eventHandlerClassName) {
    for (VariableElement v : mImplMembers.values()) {
      implClassBuilder.addField(getPropFieldSpec(v, false));
    }

    if (mExtraStateMembers != null) {
      for (String key : mExtraStateMembers.keySet()) {
        final TypeMirror variableType = mExtraStateMembers.get(key);
        final FieldSpec.Builder fieldBuilder = FieldSpec.builder(TypeName.get(variableType), key);
        implClassBuilder.addField(fieldBuilder.build());
      }
    }

    for (TypeElement event : mEventDeclarations) {
      implClassBuilder.addField(FieldSpec.builder(
          eventHandlerClassName,
          getEventHandlerInstanceName(event.getSimpleName().toString()))
          .build());
    }
  }

  private FieldSpec getPropFieldSpec(VariableElement v, boolean isStateProp) {
    final TypeMirror variableType = v.asType();
    TypeMirror wrappingTypeMirror = Utils.getGenericTypeArgument(
        variableType,
        ClassNames.OUTPUT);
    if (wrappingTypeMirror == null) {
      wrappingTypeMirror = Utils.getGenericTypeArgument(variableType, ClassNames.DIFF);
    }
    final TypeName variableClassName = JPUtil.getTypeFromMirror(
        wrappingTypeMirror != null ? wrappingTypeMirror : variableType);

    final FieldSpec.Builder fieldBuilder = FieldSpec.builder(
        variableClassName,
        v.getSimpleName().toString());

    if (!isInterStageComponentVariable(v)) {
      if (isStateProp) {
        fieldBuilder.addAnnotation(State.class);
      } else {
        fieldBuilder.addAnnotation(Prop.class);
      }
    }

    final boolean hasDefaultValue = hasDefaultValue(v);

    if (hasDefaultValue) {
      fieldBuilder.initializer(
          "$L.$L",
          mSourceElement.getSimpleName().toString(),
          v.getSimpleName().toString());
    }

    return fieldBuilder.build();
  }

  public void generateShouldUpdateMethod(
      ExecutableElement shouldUpdateElement,
      ClassName comparedInstancesClassName) {
    final ClassName implClass = ClassName.bestGuess(getImplClassName());

    final MethodSpec.Builder shouldUpdateComponent =
        MethodSpec.methodBuilder("shouldUpdate")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.BOOLEAN)
            .addParameter(comparedInstancesClassName, "previous")
            .addParameter(comparedInstancesClassName, "next");

    final List<? extends VariableElement> shouldUpdateParams = shouldUpdateElement.getParameters();
    final int shouldUpdateParamSize = shouldUpdateParams.size();

    if (shouldUpdateParamSize > 0) {
      shouldUpdateComponent
          .addStatement(
              "$L previousImpl = ($L) previous",
              implClass,
              implClass)
          .addStatement(
              "$L nextImpl = ($L) next",
              implClass,
              implClass);
    }

    final CodeBlock.Builder delegateParameters = CodeBlock.builder();
    delegateParameters.indent();
    int i = 0;
    final CodeBlock.Builder releaseDiffs = CodeBlock.builder();

    for (VariableElement variableElement : shouldUpdateParams) {
      final Name variableElementName = variableElement.getSimpleName();
      final TypeMirror variableElementType = variableElement.asType();
      final VariableElement componentMember = findPropVariableForName(variableElementName);

      if (componentMember == null) {
        throw new ComponentsProcessingException(
            variableElement,
            "Arguments for ShouldUpdate should match declared Props");
      }

      final TypeMirror innerType = Utils.getGenericTypeArgument(
          variableElementType,
          ClassNames.DIFF);

      if (innerType == null) {
        throw new ComponentsProcessingException(
            variableElement,
            "Arguments for ShouldUpdate should be of type Diff " + componentMember.asType());
      }

      final TypeName typeName;
      final TypeName innerTypeName = JPUtil.getTypeFromMirror(innerType);
      if (componentMember.asType().getKind().isPrimitive()) {
        typeName = JPUtil.getTypeFromMirror(componentMember.asType()).box();
      } else {
        typeName = JPUtil.getTypeFromMirror(componentMember.asType());
      }

      if (!typeName.equals(innerTypeName)) {
        throw new ComponentsProcessingException(
            variableElement,
            "Diff Type parameter does not match Prop " + componentMember);
      }

      shouldUpdateComponent
          .addStatement(
              "$L $L = acquireDiff(previousImpl.$L, nextImpl.$L)",
              variableElementType,
              variableElementName,
              variableElementName,
              variableElementName);

      if (i != 0) {
        delegateParameters.add(",\n");
      }
      delegateParameters.add(variableElementName.toString());
      i++;

      releaseDiffs.addStatement(
          "releaseDiff($L)",
          variableElementName);
    }
    delegateParameters.unindent();

    shouldUpdateComponent.addStatement(
        "boolean shouldUpdate = $L.$L(\n$L)",
        mSourceDelegateAccessorName,
        shouldUpdateElement.getSimpleName(),
        delegateParameters.build());
    shouldUpdateComponent.addCode(releaseDiffs.build());
    shouldUpdateComponent.addStatement(
        "return shouldUpdate");

    mClassTypeSpec.addMethod(shouldUpdateComponent.build());
  }

  public void generateTreePropsMethods(ClassName contextClassName, ClassName componentClassName) {
    verifyOnCreateTreePropsForChildren(contextClassName);
    if (!mTreeProps.isEmpty()) {
      final PopulateTreePropsMethodBuilder builder = new PopulateTreePropsMethodBuilder();
      builder.componentClassName = componentClassName;
      builder.lifecycleImplClass = getImplClassName();
      for (VariableElement treeProp : mTreeProps) {
        builder.treeProps.add(
            new Parameter(ClassName.get(treeProp.asType()), treeProp.getSimpleName().toString()));
      }
      mClassTypeSpec.addMethod(builder.build());
    }

    if (mOnCreateTreePropsMethods.isEmpty()) {
      return;
    }

    final GetTreePropsForChildrenMethodBuilder builder = new GetTreePropsForChildrenMethodBuilder();
    builder.lifecycleImplClass = getImplClassName();
    builder.delegateName = getSourceDelegateAccessorName();
    builder.contextClassName = contextClassName;
    builder.componentClassName = componentClassName;

    for (ExecutableElement executable : mOnCreateTreePropsMethods) {
      final GetTreePropsForChildrenMethodBuilder.CreateTreePropMethodData
          method = new GetTreePropsForChildrenMethodBuilder.CreateTreePropMethodData();
      method.parameters = getParams(executable);
      method.returnType = ClassName.get(executable.getReturnType());
      method.name = executable.getSimpleName().toString();
      builder.createTreePropMethods.add(method);
    }

    mClassTypeSpec.addMethod(builder.build());
  }

  private void verifyOnCreateTreePropsForChildren(ClassName contextClassName) {
    for (ExecutableElement method : mOnCreateTreePropsMethods) {
      if (method.getReturnType().getKind().equals(TypeKind.VOID)) {
        throw new ComponentsProcessingException(
            method,
            "@OnCreateTreeProp annotated method" +
                method.getSimpleName() +
                "cannot have a void return type");
      }

      final List<? extends VariableElement> params = method.getParameters();
      if (params.isEmpty()
          || !ClassName.get(params.get(0).asType()).equals(contextClassName)) {
        throw new ComponentsProcessingException(
            method,
            "The first argument of an @OnCreateTreeProp method should be the "
                + contextClassName.simpleName());
      }
    }
  }

  private VariableElement findPropVariableForName(Name variableElementName) {
    for (VariableElement prop : mProps) {
      if (prop.getSimpleName().equals(variableElementName)) {
        return prop;
      }
    }

    return null;
  }

  private List<String> getInterStageVariableNames() {
    final List<String> elementList = new ArrayList<>();

    for (VariableElement v : mImplMembers.values()) {
      if (isInterStageComponentVariable(v)) {
        elementList.add(v.getSimpleName().toString());
      }
    }

    return elementList;
  }

  private static boolean isInterStageComponentVariable(VariableElement variableElement) {
    final TypeMirror variableType = variableElement.asType();
    final TypeMirror outputTypeMirror = Utils.getGenericTypeArgument(
        variableType,
        ClassNames.OUTPUT);
    return outputTypeMirror != null;
  }

  private static String getEventHandlerInstanceName(String eventHandlerClassName) {
    return Character.toLowerCase(eventHandlerClassName.charAt(0)) +
        eventHandlerClassName.substring(1) +
        "Handler";
  }

  /**
   * Gather a list of VariableElement that should form the members of the generated Impl class
   * for this component. This list is stored in the form of a LinkedHashMap, to preserve ordering,
   * and to allow easy lookup of type information for a given variable.
   */
  private void populateImplMembers() {
    // We use a linked hash map to guarantee iteration order
    final LinkedHashMap<String, VariableElement> variableNameToElementMap = new LinkedHashMap<>();

    final List<VariableElement> specDefinedParameters = new ArrayList<>();

    for (ExecutableElement stage : mStages) {
      specDefinedParameters.addAll(getSpecDefinedParameters(stage));
    }

    addCreateInitialStateDefinedProps(specDefinedParameters);

    for (VariableElement v : specDefinedParameters) {
      if (v.getAnnotation(State.class) != null) {
        continue;
      }
      if (!variableNameToElementMap.containsKey(v.getSimpleName().toString())) {
        // Validation unnecessary - already handled by validateAnnotatedParameters
        final String name = v.getSimpleName().toString();
        variableNameToElementMap.put(name, v);
      }
    }

    mImplMembers = variableNameToElementMap;
  }

  private void populateImplParameters() {
    // We use a linked hash map to guarantee iteration order.
    final List<Parameter> componentParameters = new ArrayList<>();

    List<VariableElement> specDefinedParameters = new ArrayList<>();

    for (ExecutableElement stage : mStages) {
      specDefinedParameters.addAll(getSpecDefinedParameters(stage));
    }

    addCreateInitialStateDefinedProps(specDefinedParameters);

    for (VariableElement v : specDefinedParameters) {
      final TypeMirror diffInnerType = Utils.getGenericTypeArgument(v.asType(), ClassNames.DIFF);

      final Parameter componentParameter =
          new Parameter(
              diffInnerType != null ? ClassName.get(diffInnerType) : ClassName.get(v.asType()),
              v.getSimpleName().toString());

      if (!componentParameters.contains(componentParameter)) {
        componentParameters.add(componentParameter);
      }
    }

    if (mExtraStateMembers != null) {
      for (String key : mExtraStateMembers.keySet()) {
        componentParameters.add(new Parameter(TypeName.get(mExtraStateMembers.get(key)), key));
      }
    }

    mImplParameters = componentParameters;
  }

  /**
   * @return The list of {@link Parameter}s that will be part of the inner Impl class.
   */
  public List<Parameter> getImplParameters() {
    return mImplParameters;
  }

  public String getSimpleClassName() {
    return mSimpleClassName;
  }

  public String getSourceDelegateAccessorName() {
    return mSourceDelegateAccessorName;
  }

  public void setSourceDelegateAccessorName(String sourceDelegateAccessorName) {
    mSourceDelegateAccessorName = sourceDelegateAccessorName;
  }

  private String getImplClassName() {
    return getSimpleClassName() + IMPL_CLASS_NAME_SUFFIX;
  }

  private String getImplInstanceName() {
    String outerClassName = getSimpleClassName();
    return outerClassName.substring(0, 1).toLowerCase() +
        outerClassName.substring(1) +
        IMPL_CLASS_NAME_SUFFIX;
  }

  private static String getStateUpdateClassName(ExecutableElement updateMethod) {
    String methodName = updateMethod.getSimpleName().toString();
    return methodName.substring(0, 1).toUpperCase() +
        methodName.substring(1) +
        STATE_UPDATE_IMPL_NAME_SUFFIX;
  }

  private void writeMethodSpec(MethodSpec methodSpec) {
    mClassTypeSpec.addMethod(methodSpec);
  }

  private void writeInnerTypeSpec(TypeSpec typeSpec) {
    mClassTypeSpec.addType(typeSpec);
  }

  private static List<Parameter> getStateValueParams(ExecutableElement element) {
    final List<Parameter> params = new ArrayList<>();
    for (VariableElement v : element.getParameters()) {
      if (v.getAnnotation(Param.class) == null) {
        params.add(new Parameter(ClassName.get(v.asType()), v.getSimpleName().toString()));
      }
    }

    return params;
  }

  private static List<Parameter> getParamsWithAnnotation(
      ExecutableElement e,
      Class annotationClass) {
    final List<Parameter> params = new ArrayList<>();

    for (VariableElement v : e.getParameters()) {
      if (v.getAnnotation(annotationClass) != null) {
        params.add(new Parameter(ClassName.get(v.asType()), v.getSimpleName().toString()));
      }
    }

    return params;
  }

  public Set<String> getStateParamNames() {
    return mStateMap.keySet();
  }

  private String getStateContainerImplClassName() {
    return getSimpleClassName() + STATE_CONTAINER_IMPL_NAME_SUFFIX;
  }

  private void addCreateInitialStateDefinedProps(List<VariableElement> stagesProps) {
    final Set<String> stagesPropNames = new LinkedHashSet<>();

    for (VariableElement v : stagesProps) {
      stagesPropNames.add(v.getSimpleName().toString());
    }

    for (VariableElement v : mOnCreateInitialStateDefinedProps) {
      if (!stagesPropNames.contains(v.getSimpleName().toString())) {
        stagesPropNames.add(v.getSimpleName().toString());
        stagesProps.add(v);
      }
    }
  }
}
