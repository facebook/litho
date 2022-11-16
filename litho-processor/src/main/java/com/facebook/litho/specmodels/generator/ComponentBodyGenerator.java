/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.DelegateMethodGenerator.isOutputType;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.DYNAMIC_PROPS;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.PREVIOUS_RENDER_DATA_FIELD_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_CONTAINER_GETTER;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_CONTAINER_IMPL_GETTER;
import static com.facebook.litho.specmodels.generator.InterStagePropsContainerGenerator.getInterStagePropsContainerClassName;
import static com.facebook.litho.specmodels.generator.PrepareInterStagePropsContainerGenerator.getPrepareInterStagePropsContainerClassName;
import static com.facebook.litho.specmodels.generator.StateContainerGenerator.getStateContainerClassName;
import static com.facebook.litho.specmodels.model.MethodParamModelUtils.isAnnotatedWith;

import androidx.annotation.Nullable;
import com.facebook.litho.annotations.Comparable;
import com.facebook.litho.annotations.Generated;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.BindDynamicValueMethod;
import com.facebook.litho.specmodels.model.CachedValueParamModel;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethodDescription;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.InjectPropModel;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.PrepareInterStageInputParamModel;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.RenderDataDiffModel;
import com.facebook.litho.specmodels.model.SpecElementType;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.TreePropModel;
import com.facebook.litho.specmodels.model.TypeSpec.DeclaredTypeSpec;
import com.facebook.litho.specmodels.model.UpdateStateMethod;
import com.google.common.base.Preconditions;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/** Class that generates the implementation of a Component. */
public class ComponentBodyGenerator {

  public static final String STATE_CONTAINER_ARGUMENT_NAME = "_stateContainer";
  static final String LOCAL_STATE_CONTAINER_NAME = "_state";
  static final String LOCAL_INTER_STAGE_PROPS_CONTAINER_NAME = "_interStageProps";
  static final String LIFECYCLE_CREATE_INITIAL_STATE = "createInitialState";

  static final Predicate<MethodParamModel> PREDICATE_NEEDS_STATE =
      param -> param instanceof StateParamModel || isAnnotatedWith(param, State.class);

  static final Predicate<DelegateMethodDescription.OptionalParameterType>
      PREDICATE_ALLOWS_INTERSTAGE_OUTPUTS =
          type -> type.equals(DelegateMethodDescription.OptionalParameterType.INTER_STAGE_OUTPUT);

  private ComponentBodyGenerator() {}

  public static TypeSpecDataHolder generate(
      SpecModel specModel, @Nullable MethodParamModel optionalField, EnumSet<RunMode> runMode) {
    final TypeSpecDataHolder.Builder builder = TypeSpecDataHolder.newBuilder();

    final boolean hasState = !specModel.getStateValues().isEmpty();
    if (hasState) {
      final ClassName stateContainerClass =
          ClassName.bestGuess(getStateContainerClassName(specModel));
      builder.addMethod(generateStateContainerImplGetter(specModel, stateContainerClass));
      builder.addMethod(generateStateContainerCreator(stateContainerClass));
    }

    boolean hasInterstageProps =
        specModel.getInterStageInputs() != null && !specModel.getInterStageInputs().isEmpty();

    final ClassName interstagepropsContainerClass =
        ClassName.bestGuess(getInterStagePropsContainerClassName(specModel));
    if (hasInterstageProps) {
      builder.addType(InterStagePropsContainerGenerator.generate(specModel));
      builder.addMethod(generateInterStagePropsContainerCreator(interstagepropsContainerClass));
      builder.addMethod(
          generateInterstagePropsContainerImplGetter(specModel, interstagepropsContainerClass));
    }

    boolean hasPrepareInterstageProps =
        specModel.getPrepareInterStageInputs() != null
            && !specModel.getPrepareInterStageInputs().isEmpty();

    if (hasPrepareInterstageProps) {
      final ClassName prepareInterStagePropContainerClassName =
          ClassName.bestGuess(getPrepareInterStagePropsContainerClassName(specModel));
      builder.addType(PrepareInterStagePropsContainerGenerator.generate(specModel));
      builder.addMethod(
          generatePrepareInterStagePropsContainerCreator(prepareInterStagePropContainerClassName));
      builder.addMethod(
          generatePrepareInterstagePropsContainerImplGetter(
              specModel, prepareInterStagePropContainerClassName));
    }

    final boolean needsRenderDataInfra = !specModel.getRenderDataDiffs().isEmpty();
    if (needsRenderDataInfra) {
      final ClassName previousRenderDataClass =
          ClassName.bestGuess(RenderDataGenerator.getRenderDataImplClassName(specModel));
      builder.addField(previousRenderDataClass, PREVIOUS_RENDER_DATA_FIELD_NAME, Modifier.PRIVATE);
    }

    builder
        .addTypeSpecDataHolder(generateInjectedFields(specModel))
        .addTypeSpecDataHolder(generateProps(specModel, runMode))
        .addTypeSpecDataHolder(generateTreeProps(specModel, runMode))
        .addTypeSpecDataHolder(generateOptionalField(optionalField))
        .addTypeSpecDataHolder(generateEventHandlers(specModel))
        .addTypeSpecDataHolder(generateEventTriggers(specModel));

    if (specModel.shouldGenerateIsEquivalentTo()) {
      builder.addMethod(generateIsEquivalentPropsMethod(specModel, runMode));
    }

    if (specModel.shouldGenerateIsEquivalentTo()
        && specModel.getTreeProps() != null
        && !specModel.getTreeProps().isEmpty()
        && specModel.getContextClass().equals(ClassNames.COMPONENT_CONTEXT)) {
      builder.addMethod(generateIsEqualivalentTreePropsMethod(specModel, runMode));
    }

    if (hasInterstageProps) {
      builder.addTypeSpecDataHolder(
          generateCopyInterStageImpl(specModel, interstagepropsContainerClass));
    }

    if (hasPrepareInterstageProps) {
      builder.addTypeSpecDataHolder(
          generateCopyPrepareInterStageImpl(specModel, interstagepropsContainerClass));
    }

    builder.addTypeSpecDataHolder(generateMakeShallowCopy(specModel, hasState));
    builder.addTypeSpecDataHolder(generateGetDynamicProps(specModel));
    builder.addTypeSpecDataHolder(generateBindDynamicProp(specModel));

    if (hasState) {
      builder.addType(StateContainerGenerator.generate(specModel, runMode));
    }
    if (needsRenderDataInfra) {
      builder.addType(generatePreviousRenderDataContainerImpl(specModel));
    }

    return builder.build();
  }

  private static TypeSpecDataHolder generateOptionalField(MethodParamModel optionalField) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    if (optionalField == null) {
      return typeSpecDataHolder.build();
    }

    typeSpecDataHolder.addField(
        FieldSpec.builder(optionalField.getTypeName(), optionalField.getName()).build());
    return typeSpecDataHolder.build();
  }

  static TypeSpec generatePreviousRenderDataContainerImpl(SpecModel specModel) {
    final String className = RenderDataGenerator.getRenderDataImplClassName(specModel);
    final TypeSpec.Builder renderInfoClassBuilder =
        TypeSpec.classBuilder(className)
            .addSuperinterface(ClassNames.RENDER_DATA)
            .addAnnotation(Generated.class);

    if (!specModel.hasInjectedDependencies()) {
      renderInfoClassBuilder.addModifiers(Modifier.STATIC, Modifier.PRIVATE);
      renderInfoClassBuilder.addTypeVariables(specModel.getTypeVariables());
    }

    final String copyParamName = "info";
    final String recordParamName = "component";
    final MethodSpec.Builder copyBuilder =
        MethodSpec.methodBuilder("copy")
            .addParameter(ClassName.bestGuess(className), copyParamName);
    final MethodSpec.Builder recordBuilder =
        MethodSpec.methodBuilder("record")
            .addParameter(specModel.getContextClass(), "c")
            .addParameter(specModel.getComponentTypeName(), recordParamName);

    for (RenderDataDiffModel diff : specModel.getRenderDataDiffs()) {
      final MethodParamModel modelToDiff =
          SpecModelUtils.getReferencedParamModelForDiff(specModel, diff);

      if (modelToDiff == null) {
        throw new RuntimeException(
            "Got Diff of a param that doesn't seem to exist: "
                + diff.getName()
                + ". This should "
                + "have been caught in the validation pass.");
      }

      if (!(modelToDiff instanceof PropModel || modelToDiff instanceof StateParamModel)) {
        throw new RuntimeException(
            "Got Diff of a param that is not a @Prop or @State! ("
                + diff.getName()
                + ", a "
                + modelToDiff.getClass().getSimpleName()
                + "). This should have been caught in the "
                + "validation pass.");
      }

      final String name = modelToDiff.getName();
      if (modelToDiff instanceof PropModel) {
        renderInfoClassBuilder.addField(
            FieldSpec.builder(modelToDiff.getTypeName(), name).addAnnotation(Prop.class).build());
      } else {
        renderInfoClassBuilder.addField(
            FieldSpec.builder(modelToDiff.getTypeName(), modelToDiff.getName())
                .addAnnotation(State.class)
                .build());
      }

      copyBuilder.addStatement("$L = $L.$L", name, copyParamName, name);
      recordBuilder.addStatement(
          "$L = $L.$L",
          name,
          recordParamName,
          getImplAccessor("record", specModel, modelToDiff, "c"));
    }

    return renderInfoClassBuilder
        .addMethod(copyBuilder.build())
        .addMethod(recordBuilder.build())
        .build();
  }

  static String getInstanceRefName(SpecModel specModel) {
    final String refClassName = specModel.getComponentName() + "Ref";
    return refClassName.substring(0, 1).toLowerCase(Locale.ROOT) + refClassName.substring(1);
  }

  static MethodSpec generateStateContainerImplGetter(
      SpecModel specModel, TypeName stateContainerImplClassName) {
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder(STATE_CONTAINER_IMPL_GETTER)
            .addModifiers(Modifier.PRIVATE)
            .addParameter(specModel.getContextClass(), "c")
            .returns(stateContainerImplClassName);

    if (specModel.isStateful()) {
      builder.addStatement(
          "return ($T) $T." + STATE_CONTAINER_GETTER + "(c, this)",
          stateContainerImplClassName,
          StateGenerator.getStateContainerGetterClassName(specModel));
    } else {
      builder.addStatement(
          "return ($T) c.getScopedComponentInfo().getStateContainer()",
          stateContainerImplClassName);
    }
    return builder.build();
  }

  static MethodSpec generateStateContainerCreator(ClassName stateContainerImplClassName) {
    return MethodSpec.methodBuilder("createStateContainer")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(stateContainerImplClassName)
        .addStatement("return new $T()", stateContainerImplClassName)
        .build();
  }

  public static TypeSpecDataHolder generateProps(SpecModel specModel, EnumSet<RunMode> runMode) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<PropModel> props = specModel.getProps();

    boolean hasDynamicProps = false;
    for (PropModel prop : props) {
      final TypeName propTypeName = prop.getTypeName();
      final TypeName fieldTypeName =
          !prop.isDynamic()
              ? propTypeName
              : ParameterizedTypeName.get(ClassNames.DYNAMIC_VALUE, propTypeName.box());

      AnnotationSpec.Builder propAnnotationBuilder =
          AnnotationSpec.builder(Prop.class)
              .addMember("resType", "$T.$L", ResType.class, prop.getResType())
              .addMember("optional", "$L", prop.isOptional());
      TypeName propFieldTypeName = fieldTypeName;
      if (prop.hasVarArgs()) {
        propAnnotationBuilder.addMember("varArg", "$S", prop.getVarArgsSingleName());
        propFieldTypeName =
            KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(specModel, fieldTypeName);
      }
      final FieldSpec.Builder fieldBuilder =
          FieldSpec.builder(propFieldTypeName, prop.getName())
              .addAnnotations(prop.getExternalAnnotations())
              .addAnnotation(propAnnotationBuilder.build())
              .addAnnotation(
                  AnnotationSpec.builder(Comparable.class)
                      .addMember("type", "$L", getComparableType(prop, runMode))
                      .build());
      if (prop.hasDefault(specModel.getPropDefaults())) {
        final PropDefaultModel propDefault =
            Preconditions.checkNotNull(prop.getDefault(specModel.getPropDefaults()));
        assignInitializer(fieldBuilder, specModel, prop, propDefault);
      } else if (prop.hasVarArgs()) {
        fieldBuilder.initializer("$T.emptyList()", ClassName.get(Collections.class));
      }

      FieldSpec field = fieldBuilder.build();
      typeSpecDataHolder.addField(field);

      if (runMode.contains(RunMode.TESTING)) {
        MethodSpec getter =
            GeneratorUtils.getter(field)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotations(field.annotations)
                .build();
        typeSpecDataHolder.addMethod(getter);
      }

      if (prop.isDynamic()) {
        hasDynamicProps = true;
      }
    }

    // If there are dynamic props we also need to generate mDynamicProps fields, which assembles all
    // of them
    if (hasDynamicProps) {
      typeSpecDataHolder.addField(
          FieldSpec.builder(ArrayTypeName.of(ClassNames.DYNAMIC_VALUE), DYNAMIC_PROPS)
              .addModifiers(Modifier.PRIVATE)
              .build());
    }

    return typeSpecDataHolder.build();
  }

  private static void assignInitializer(
      FieldSpec.Builder fieldBuilder,
      SpecModel specModel,
      PropModel prop,
      PropDefaultModel propDefault) {

    SpecElementType type = specModel.getSpecElementType();

    if (isKotlinPropDefaultWithGetterMethod(specModel, propDefault)) {
      final String propName = prop.getName();
      final boolean needGetter = !propName.startsWith("is");
      final String propAccessor =
          (needGetter ? "get" + propName.substring(0, 1).toUpperCase() : propName.substring(0, 1))
              + propName.substring(1)
              + "()";

      fieldBuilder.initializer(
          "$L.$L.$L",
          specModel.getSpecName(),
          type == SpecElementType.KOTLIN_SINGLETON ? "INSTANCE" : "Companion",
          propAccessor);

    } else {
      fieldBuilder.initializer("$L.$L", specModel.getSpecName(), prop.getName());
    }
  }

  /**
   * When the spec is written in Kotlin, the generated code will at times require that we need to
   * access the field via a getter method. This is always the case when an the spec is a Kotlin
   * object but only sometimes the case when it's a Kotlin class.
   *
   * @return true when we need to access the PropDefault via a getter method such as <code>
   *     getSomePropDefault()</code>. Otherwise, false as we want to access the field via <code>
   *     somePropDefault</code>
   */
  private static boolean isKotlinPropDefaultWithGetterMethod(
      final SpecModel specModel, final PropDefaultModel propDefault) {
    return KotlinSpecHelper.isKotlinSpec(specModel) && propDefault.isGetterMethodAccessor();
  }

  public static TypeSpecDataHolder generateInjectedFields(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    if (specModel.hasInjectedDependencies()) {
      typeSpecDataHolder.addTypeSpecDataHolder(
          specModel
              .getDependencyInjectionHelper()
              .generateInjectedFields(specModel, specModel.getInjectProps()));

      final List<MethodSpec> testAccessors =
          specModel.getInjectProps().stream()
              .map(
                  p ->
                      specModel
                          .getDependencyInjectionHelper()
                          .generateTestingFieldAccessor(specModel, p))
              .collect(Collectors.toList());
      typeSpecDataHolder.addMethods(testAccessors);
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateTreeProps(SpecModel specModel, EnumSet<RunMode> runMode) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<TreePropModel> treeProps = specModel.getTreeProps();

    for (TreePropModel treeProp : treeProps) {
      final FieldSpec field =
          FieldSpec.builder(treeProp.getTypeName(), treeProp.getName())
              .addAnnotation(TreeProp.class)
              .addAnnotation(
                  AnnotationSpec.builder(Comparable.class)
                      .addMember("type", "$L", getComparableType(treeProp, runMode))
                      .build())
              .build();
      if (runMode.contains(RunMode.TESTING)) {
        MethodSpec getter =
            GeneratorUtils.getter(field)
                .addParameter(specModel.getContextClass(), "c")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotations(field.annotations)
                .build();
        typeSpecDataHolder.addMethod(getter);
      }
      typeSpecDataHolder.addField(field);
    }

    return typeSpecDataHolder.build();
  }

  static MethodSpec generateInterstagePropsContainerImplGetter(
      SpecModel specModel, TypeName interstagePropsContainerImplClassName) {
    return MethodSpec.methodBuilder("getInterStagePropsContainerImpl")
        .addModifiers(Modifier.PRIVATE)
        .addParameter(specModel.getContextClass(), "c")
        .addParameter(ClassNames.INTER_STAGE_PROPS_CONTAINER, "interStageProps")
        .returns(interstagePropsContainerImplClassName)
        .addStatement(
            "return ($T) super.getInterStagePropsContainer(c, interStageProps)",
            interstagePropsContainerImplClassName)
        .build();
  }

  static MethodSpec generatePrepareInterstagePropsContainerImplGetter(
      SpecModel specModel, TypeName prepareInterstagePropsContainerImplClassName) {
    return MethodSpec.methodBuilder("getPrepareInterStagePropsContainerImpl")
        .addModifiers(Modifier.PRIVATE)
        .addParameter(specModel.getContextClass(), "c")
        .returns(prepareInterstagePropsContainerImplClassName)
        .addStatement(
            "return ($T) super.getPrepareInterStagePropsContainer(c)",
            prepareInterstagePropsContainerImplClassName)
        .build();
  }

  static MethodSpec generateInterStagePropsContainerCreator(
      ClassName interStagePropsContainerImplClassName) {
    return MethodSpec.methodBuilder("createInterStagePropsContainer")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(interStagePropsContainerImplClassName)
        .addStatement("return new $T()", interStagePropsContainerImplClassName)
        .build();
  }

  static MethodSpec generatePrepareInterStagePropsContainerCreator(
      ClassName prepareInterStagePropsContainerImplClassName) {
    return MethodSpec.methodBuilder("createPrepareInterStagePropsContainer")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(prepareInterStagePropsContainerImplClassName)
        .addStatement("return new $T()", prepareInterStagePropsContainerImplClassName)
        .build();
  }

  static TypeSpecDataHolder generateInterStageInputs(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<InterStageInputParamModel> interStageInputs =
        specModel.getInterStageInputs();

    for (InterStageInputParamModel interStageInput : interStageInputs) {
      typeSpecDataHolder.addField(
          FieldSpec.builder(interStageInput.getTypeName().box(), interStageInput.getName())
              .build());
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateEventHandlers(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    for (EventDeclarationModel eventDeclaration : specModel.getEventDeclarations()) {
      typeSpecDataHolder.addField(
          FieldSpec.builder(
                  ParameterizedTypeName.get(ClassNames.EVENT_HANDLER, eventDeclaration.name),
                  getEventHandlerInstanceName(eventDeclaration))
              .addAnnotation(ClassNames.NULLABLE)
              .build());
    }

    return typeSpecDataHolder.build();
  }

  static String getEventHandlerInstanceName(EventDeclarationModel model) {
    final String eventHandlerName = ClassName.bestGuess(model.getRawName().toString()).simpleName();
    return eventHandlerName.substring(0, 1).toLowerCase(Locale.ROOT)
        + eventHandlerName.substring(1)
        + "Handler";
  }

  static TypeSpecDataHolder generateEventTriggers(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    for (SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel :
        specModel.getTriggerMethods()) {
      typeSpecDataHolder.addField(
          FieldSpec.builder(
                  ClassNames.EVENT_TRIGGER, getEventTriggerInstanceName(eventMethodModel.name))
              .build());
    }

    return typeSpecDataHolder.build();
  }

  static String getEventTriggerInstanceName(CharSequence eventTriggerClassName) {
    final String eventTriggerName = eventTriggerClassName.toString();

    return eventTriggerName.substring(0, 1).toLowerCase(Locale.ROOT)
        + eventTriggerName.substring(1)
        + "Trigger";
  }

  static MethodSpec generateIsEqualivalentTreePropsMethod(
      SpecModel specModel, EnumSet<RunMode> runMode) {
    MethodSpec.Builder isEquivalentBuilder =
        MethodSpec.methodBuilder("isEqualivalentTreeProps")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PROTECTED)
            .returns(TypeName.BOOLEAN)
            .addParameter(specModel.getContextClass(), "current")
            .addParameter(specModel.getContextClass(), "next");
    for (TreePropModel treeProp : specModel.getTreeProps()) {
      isEquivalentBuilder.addCode(
          getCompareStatement(
              "isEqualivalentTreeProps",
              treeProp,
              "current.getParentTreeProp("
                  + TreePropGenerator.findTypeByTypeName(treeProp.getTypeName())
                  + ".class"
                  + ")",
              "next.getParentTreeProp("
                  + TreePropGenerator.findTypeByTypeName(treeProp.getTypeName())
                  + ".class"
                  + ")",
              runMode));
    }

    isEquivalentBuilder.addStatement("return true");
    return isEquivalentBuilder.build();
  }

  static MethodSpec generateIsEquivalentPropsMethod(SpecModel specModel, EnumSet<RunMode> runMode) {
    final String className = specModel.getComponentName();
    final String instanceRefName = getInstanceRefName(specModel);

    MethodSpec.Builder isEquivalentBuilder =
        MethodSpec.methodBuilder("isEquivalentProps")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.BOOLEAN)
            .addParameter(specModel.getComponentClass(), "other")
            .addParameter(TypeName.BOOLEAN, "shouldCompareCommonProps")
            .beginControlFlow("if (this == other)")
            .addStatement("return true")
            .endControlFlow()
            .beginControlFlow("if (other == null || getClass() != other.getClass())")
            .addStatement("return false")
            .endControlFlow()
            .addStatement("$N $N = ($N) other", className, instanceRefName, className);

    if (specModel.shouldCheckIdInIsEquivalentToMethod()) {
      isEquivalentBuilder
          .beginControlFlow("if (this.getId() == $N.getId())", instanceRefName)
          .addStatement("return true")
          .endControlFlow();
    }

    for (PropModel prop : specModel.getProps()) {
      isEquivalentBuilder.addCode(
          getCompareStatement("isEquivalentProps", specModel, instanceRefName, prop, runMode));
    }

    ImmutableList<TreePropModel> treeProps = specModel.getTreeProps();
    if (treeProps != null && !treeProps.isEmpty() && specModel.isStateful()) {
      for (TreePropModel treeProp : specModel.getTreeProps()) {
        isEquivalentBuilder.addCode(
            getCompareStatement(
                "isEquivalentProps", specModel, instanceRefName, treeProp, runMode));
      }
    }

    isEquivalentBuilder.addStatement("return true");

    return isEquivalentBuilder.build();
  }

  static TypeSpecDataHolder generateCopyInterStageImpl(
      SpecModel specModel, ClassName implClassName) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<InterStageInputParamModel> interStageInputs =
        specModel.getInterStageInputs();

    if (specModel.shouldGenerateCopyMethod() && !interStageInputs.isEmpty()) {
      final String className =
          InterStagePropsContainerGenerator.getInterStagePropsContainerClassName(specModel);
      final String copyIntoParam = "copyIntoInterStagePropsContainer";
      final String copyFromParam = "copyFromInterStagePropsContainer";
      final String copyIntoInstanceName = copyIntoParam + "_ref";
      final String copyFromInstanceName = copyFromParam + "_ref";
      final MethodSpec.Builder copyInterStageComponentBuilder =
          MethodSpec.methodBuilder("copyInterStageImpl")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PROTECTED)
              .returns(TypeName.VOID)
              .addParameter(ClassNames.INTER_STAGE_PROPS_CONTAINER, copyIntoParam)
              .addParameter(ClassNames.INTER_STAGE_PROPS_CONTAINER, copyFromParam)
              .addStatement(
                  "$N $N = ($N) $N", className, copyIntoInstanceName, className, copyIntoParam)
              .addStatement(
                  "$N $N = ($N) $N", className, copyFromInstanceName, className, copyFromParam);

      for (InterStageInputParamModel interStageInput : interStageInputs) {
        copyInterStageComponentBuilder.addStatement(
            "$N.$N = $N.$N",
            copyIntoInstanceName,
            interStageInput.getName(),
            copyFromInstanceName,
            interStageInput.getName());
      }

      typeSpecDataHolder.addMethod(copyInterStageComponentBuilder.build());
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateCopyPrepareInterStageImpl(
      SpecModel specModel, ClassName implClassName) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<PrepareInterStageInputParamModel> interStageInputs =
        specModel.getPrepareInterStageInputs();

    if (specModel.shouldGenerateCopyMethod() && !interStageInputs.isEmpty()) {
      final String className =
          PrepareInterStagePropsContainerGenerator.getPrepareInterStagePropsContainerClassName(
              specModel);
      final String copyIntoParam = "copyIntoInterStagePropsContainer";
      final String copyFromParam = "copyFromInterStagePropsContainer";
      final String copyIntoInstanceName = copyIntoParam + "_ref";
      final String copyFromInstanceName = copyFromParam + "_ref";
      final MethodSpec.Builder copyPrepareInterStageComponentBuilder =
          MethodSpec.methodBuilder("copyPrepareInterStageImpl")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PROTECTED)
              .returns(TypeName.VOID)
              .addParameter(ClassNames.PREPARE_INTER_STAGE_PROPS_CONTAINER, copyIntoParam)
              .addParameter(ClassNames.PREPARE_INTER_STAGE_PROPS_CONTAINER, copyFromParam)
              .addStatement(
                  "$N $N = ($N) $N", className, copyIntoInstanceName, className, copyIntoParam)
              .addStatement(
                  "$N $N = ($N) $N", className, copyFromInstanceName, className, copyFromParam);

      for (PrepareInterStageInputParamModel interStageInput : interStageInputs) {
        copyPrepareInterStageComponentBuilder.addStatement(
            "$N.$N = $N.$N",
            copyIntoInstanceName,
            interStageInput.getName(),
            copyFromInstanceName,
            interStageInput.getName());
      }

      typeSpecDataHolder.addMethod(copyPrepareInterStageComponentBuilder.build());
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateMakeShallowCopy(SpecModel specModel, boolean hasState) {
    TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    if (!specModel.shouldGenerateCopyMethod()) {
      return typeSpecDataHolder.build();
    }

    final List<MethodParamModel> componentsInImpl = findComponentsInImpl(specModel);
    final ImmutableList<InterStageInputParamModel> interStageComponentVariables =
        specModel.getInterStageInputs();
    final ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateMethodModels =
        specModel.getUpdateStateMethods();
    final @Nullable ImmutableList<SpecMethodModel<UpdateStateMethod, Void>>
        updateStateWithTransitionMethodModels = specModel.getUpdateStateWithTransitionMethods();
    final boolean hasDeepCopy = specModel.hasDeepCopy();

    if (componentsInImpl.isEmpty()
        && interStageComponentVariables.isEmpty()
        && updateStateMethodModels.isEmpty()
        && (updateStateWithTransitionMethodModels == null
            || updateStateWithTransitionMethodModels.isEmpty())) {
      return typeSpecDataHolder.build();
    }

    final String className = specModel.getComponentName();
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("makeShallowCopy")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ClassName.bestGuess(className));

    String deepCopy = hasDeepCopy ? "deepCopy" : "";

    if (hasDeepCopy) {
      builder.addParameter(ParameterSpec.builder(TypeName.BOOLEAN, "deepCopy").build());
    }

    builder.addStatement(
        "$L $L = ($L) super.makeShallowCopy($L)", className, "component", className, deepCopy);

    for (MethodParamModel componentParam : componentsInImpl) {
      builder.addStatement(
          "component.$L = component.$L != null ? component.$L.makeShallowCopy() : null",
          componentParam.getName(),
          componentParam.getName(),
          componentParam.getName());
    }

    if (specModel.isStateful()) {

      if (hasDeepCopy) {
        builder.beginControlFlow("if (!deepCopy)");
      }

      final String stateContainerClassName = getStateContainerClassName(specModel);
      if (hasState) {
        builder.addStatement(
            "component.setStateContainer(new $T())", ClassName.bestGuess(stateContainerClassName));
      }

      final boolean hasInterStageProps =
          specModel.getInterStageInputs() != null && !specModel.getInterStageInputs().isEmpty();
      if (hasInterStageProps) {
        builder.addStatement(
            "component.setInterStagePropsContainer(createInterStagePropsContainer())");
      }

      boolean hasPrepareInterstageProps =
          specModel.getPrepareInterStageInputs() != null
              && !specModel.getPrepareInterStageInputs().isEmpty();
      if (hasPrepareInterstageProps) {
        builder.addStatement(
            "component.setPrepareInterStagePropsContainer(createPrepareInterStagePropsContainer())");
      }

      if (hasDeepCopy) {
        builder.endControlFlow();
      }
    }

    builder.addStatement("return component");

    return typeSpecDataHolder.addMethod(builder.build()).build();
  }

  static TypeSpecDataHolder generateGetDynamicProps(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    if (SpecModelUtils.getDynamicProps(specModel).isEmpty()) {
      return typeSpecDataHolder.build();
    }

    final MethodSpec methodSpec =
        MethodSpec.methodBuilder("getDynamicProps")
            .addModifiers(Modifier.PROTECTED)
            .addAnnotation(Override.class)
            .returns(ArrayTypeName.of(ClassNames.DYNAMIC_VALUE))
            .addStatement("return $L", DYNAMIC_PROPS)
            .build();

    return typeSpecDataHolder.addMethod(methodSpec).build();
  }

  static TypeSpecDataHolder generateBindDynamicProp(SpecModel specModel) {
    TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    final List<PropModel> dynamicProps = SpecModelUtils.getDynamicProps(specModel);
    if (dynamicProps.isEmpty()) {
      return typeSpecDataHolder.build();
    }

    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("bindDynamicProp")
            .addModifiers(Modifier.PROTECTED)
            .addAnnotation(Override.class)
            .returns(ClassName.VOID)
            .addParameter(ClassName.INT, "dynamicPropIndex")
            .addParameter(ClassName.OBJECT, "value")
            .addParameter(ClassName.OBJECT, "mountedContent");

    final String sourceDelegateAccessor = SpecModelUtils.getSpecAccessor(specModel);

    methodBuilder.beginControlFlow("switch (dynamicPropIndex)");

    for (int index = 0, size = dynamicProps.size(); index < size; index++) {
      final PropModel prop = dynamicProps.get(index);
      final SpecMethodModel<BindDynamicValueMethod, Void> delegate =
          SpecModelUtils.getBindDelegateMethodForDynamicProp(specModel, prop);

      methodBuilder.addCode("case $L:\n", index);
      methodBuilder.addStatement(
          "$>$L.$L(($T) mountedContent, retrieveValue($L))",
          sourceDelegateAccessor,
          delegate.name,
          delegate.methodParams.get(0).getTypeName(),
          prop.getName());
      methodBuilder.addStatement("break$<");
    }

    methodBuilder.addCode("default:\n");
    methodBuilder.addStatement("$>break$<");
    methodBuilder.endControlFlow();

    typeSpecDataHolder.addMethod(methodBuilder.build());

    return typeSpecDataHolder.build();
  }

  private static List<MethodParamModel> findComponentsInImpl(SpecModel specModel) {
    final List<MethodParamModel> componentsInImpl = new ArrayList<>();

    for (PropModel prop : specModel.getProps()) {
      TypeName typeName = prop.getTypeName();
      if (typeName instanceof ParameterizedTypeName) {
        typeName = ((ParameterizedTypeName) typeName).rawType;
      }

      if (typeName.equals(ClassNames.COMPONENT) || typeName.equals(ClassNames.SECTION)) {
        componentsInImpl.add(prop);
      }
    }

    return componentsInImpl;
  }

  static CodeBlock getCompareStatement(
      String methodName,
      SpecModel specModel,
      String implInstanceName,
      MethodParamModel field,
      EnumSet<RunMode> runMode) {
    final String implAccessor = getImplAccessor(methodName, specModel, field, null, true);

    return getCompareStatement(
        methodName, field, implAccessor, implInstanceName + "." + implAccessor, runMode);
  }

  static CodeBlock getCompareStatement(
      String methodName,
      MethodParamModel field,
      String firstComparator,
      String secondComparator,
      EnumSet<RunMode> runMode) {
    final CodeBlock.Builder codeBlock = CodeBlock.builder();

    @Comparable.Type int comparableType = getComparableType(field, runMode);
    switch (comparableType) {
      case Comparable.FLOAT:
        codeBlock
            .beginControlFlow("if (Float.compare($L, $L) != 0)", firstComparator, secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;

      case Comparable.DOUBLE:
        codeBlock
            .beginControlFlow("if (Double.compare($L, $L) != 0)", firstComparator, secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;

      case Comparable.ARRAY:
        codeBlock
            .beginControlFlow(
                "if (!$T.equals($L, $L))", Arrays.class, firstComparator, secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;

      case Comparable.PRIMITIVE:
        codeBlock
            .beginControlFlow("if ($L != $L)", firstComparator, secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;

      case Comparable.COMPARABLE_DRAWABLE:
        codeBlock
            .beginControlFlow("if (!$L.isEquivalantTo($L))", firstComparator, secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;

      case Comparable.COLLECTION_COMPLEVEL_0:
        codeBlock
            .beginControlFlow(
                "if ($L != null ? !$L.equals($L) : $L != null)",
                firstComparator,
                firstComparator,
                secondComparator,
                secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;

      case Comparable.COLLECTION_COMPLEVEL_1:
      case Comparable.COLLECTION_COMPLEVEL_2:
      case Comparable.COLLECTION_COMPLEVEL_3:
      case Comparable.COLLECTION_COMPLEVEL_4:
        // N.B. This relies on the IntDef to be in increasing order.
        int level = comparableType - Comparable.COLLECTION_COMPLEVEL_0;
        codeBlock
            .beginControlFlow("if ($L != null)", firstComparator)
            .beginControlFlow(
                "if ($L == null || $L.size() != $L.size())",
                secondComparator,
                firstComparator,
                secondComparator)
            .addStatement("return false")
            .endControlFlow()
            .add(
                getComponentCollectionCompareStatement(
                    level,
                    (DeclaredTypeSpec) field.getTypeSpec(),
                    firstComparator,
                    secondComparator))
            .nextControlFlow("else if ($L != null)", secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;

      case Comparable.COMPONENT:
        codeBlock
            .beginControlFlow(
                "if ($L != null ? !$L.isEquivalentTo($L, shouldCompareCommonProps) : $L != null)",
                firstComparator,
                firstComparator,
                secondComparator,
                secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;
      case Comparable.SECTION:
      case Comparable.EVENT_HANDLER:
      case Comparable.EVENT_HANDLER_IN_PARAMETERIZED_TYPE:
        codeBlock
            .beginControlFlow(
                "if ($L != null ? !$L.isEquivalentTo($L) : $L != null)",
                firstComparator,
                firstComparator,
                secondComparator,
                secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;

      case Comparable.OTHER:
        codeBlock
            .beginControlFlow(
                "if ($L != null ? !$L.equals($L) : $L != null)",
                firstComparator,
                firstComparator,
                secondComparator,
                secondComparator)
            .addStatement("return false")
            .endControlFlow();
        break;
    }

    return codeBlock.build();
  }

  static @Comparable.Type int getComparableType(MethodParamModel field, EnumSet<RunMode> runMode) {

    if (field instanceof PropModel) {
      if (((PropModel) field).isDynamic()) {
        return Comparable.OTHER;
      }
    }

    return getComparableType(field.getTypeName(), field.getTypeSpec(), runMode);
  }

  private static @Comparable.Type int getComparableType(
      TypeName typeName,
      com.facebook.litho.specmodels.model.TypeSpec typeSpec,
      EnumSet<RunMode> runMode) {
    if (typeName.equals(TypeName.FLOAT)) {
      return Comparable.FLOAT;
    } else if (typeName.equals(TypeName.DOUBLE)) {
      return Comparable.DOUBLE;

    } else if (typeName instanceof ArrayTypeName) {
      return Comparable.ARRAY;

    } else if (typeName.isPrimitive()) {
      return Comparable.PRIMITIVE;
    } else if (typeName.equals(ClassNames.COMPARABLE_DRAWABLE)) {
      return Comparable.COMPARABLE_DRAWABLE;
    } else if (!runMode.contains(RunMode.ABI) && typeSpec.isSubInterface(ClassNames.COLLECTION)) {
      final int level = calculateLevelOfComponentInCollections((DeclaredTypeSpec) typeSpec);
      switch (level) {
        case 0:
          return Comparable.COLLECTION_COMPLEVEL_0;
        case 1:
          return Comparable.COLLECTION_COMPLEVEL_1;
        case 2:
          return Comparable.COLLECTION_COMPLEVEL_2;
        case 3:
          return Comparable.COLLECTION_COMPLEVEL_3;
        case 4:
          return Comparable.COLLECTION_COMPLEVEL_4;
        default:
          throw new IllegalStateException("Collection Component level not supported.");
      }

    } else if (typeName.equals(ClassNames.COMPONENT)) {
      return Comparable.COMPONENT;

    } else if (typeName.equals(ClassNames.SECTION)) {
      return Comparable.SECTION;

    } else if (typeName.equals(ClassNames.EVENT_HANDLER)) {
      return Comparable.EVENT_HANDLER;

    } else if (typeName instanceof ParameterizedTypeName
        && ((ParameterizedTypeName) typeName).rawType.equals(ClassNames.EVENT_HANDLER)) {
      return Comparable.EVENT_HANDLER_IN_PARAMETERIZED_TYPE;
    }

    return Comparable.OTHER;
  }

  static String getImplAccessor(
      String methodName,
      SpecModel specModel,
      MethodParamModel methodParamModel,
      String contextParamName) {
    return getImplAccessor(methodName, specModel, methodParamModel, contextParamName, false);
  }

  static String getImplAccessor(
      String methodName,
      SpecModel specModel,
      MethodParamModel methodParamModel,
      String contextParamName,
      boolean shallow) {

    return getImplAccessorFromContainer(
        methodName, specModel, methodParamModel, contextParamName, shallow, null);
  }

  static String getImplAccessorFromContainer(
      String methodName,
      SpecModel specModel,
      MethodParamModel methodParamModel,
      String contextParamName,
      @Nullable String cachedContainerVariableName) {
    return getImplAccessorFromContainer(
        methodName,
        specModel,
        methodParamModel,
        contextParamName,
        false,
        cachedContainerVariableName);
  }

  static String getImplAccessorFromContainer(
      String methodName,
      SpecModel specModel,
      MethodParamModel methodParamModel,
      String contextParamName,
      boolean shallow,
      @Nullable String cachedContainerVariableName) {

    if (methodParamModel instanceof StateParamModel
        || SpecModelUtils.getStateValueWithName(specModel, methodParamModel.getName()) != null) {
      if (contextParamName == null) {
        throw new IllegalStateException(
            "Cannot access state in method " + methodName + " without a scoped component context.");
      }
      return cachedContainerVariableName != null
          ? cachedContainerVariableName + "." + methodParamModel.getName()
          : STATE_CONTAINER_IMPL_GETTER
              + "("
              + contextParamName
              + ")."
              + methodParamModel.getName();
    } else if (methodParamModel instanceof CachedValueParamModel) {
      if (contextParamName == null) {
        throw new IllegalStateException("Need a scoped context to access cached values.");
      }

      return "get"
          + methodParamModel.getName().substring(0, 1).toUpperCase()
          + methodParamModel.getName().substring(1)
          + "("
          + contextParamName
          + ")";
    } else if (methodParamModel instanceof PropModel
        && ((PropModel) methodParamModel).isDynamic()
        && !shallow) {
      return "retrieveValue(" + methodParamModel.getName() + ")";
    } else if (methodParamModel instanceof InjectPropModel) {
      DependencyInjectionHelper dependencyInjectionHelper =
          specModel.getDependencyInjectionHelper();
      if (dependencyInjectionHelper != null) {
        return dependencyInjectionHelper.generateImplAccessor(specModel, methodParamModel);
      }
    } else if (methodParamModel instanceof PrepareInterStageInputParamModel) {
      if (contextParamName == null) {
        throw new IllegalStateException(
            "Cannot access param of type layout inter-stage prop in method "
                + methodName
                + " because it doesn't have a scoped ComponentContext as defined parameter.");
      }

      return "getPrepareInterStagePropsContainerImpl("
          + contextParamName
          + ")."
          + methodParamModel.getName();
    } else if (methodParamModel instanceof InterStageInputParamModel) {
      if (contextParamName == null) {
        throw new IllegalStateException(
            "Cannot access param of type inter-stage prop in method "
                + methodName
                + " because it doesn't have a scoped ComponentContext as defined parameter.");
      }

      return "getInterStagePropsContainerImpl("
          + contextParamName
          + ", "
          + LOCAL_INTER_STAGE_PROPS_CONTAINER_NAME
          + ")."
          + methodParamModel.getName();
    } else if (methodParamModel instanceof TreePropModel) {
      if (contextParamName == null) {
        return methodParamModel.getName();
      }
      if (specModel.isStateful()) {
        return "(" + methodParamModel.getName() + ")";
      } else {
        return "("
            + contextParamName
            + ".getParentTreeProp("
            + TreePropGenerator.findTypeByTypeName(methodParamModel.getTypeName())
            + ".class"
            + "))";
      }
    }

    return methodParamModel.getName();
  }

  static String getImplAccessor(
      SpecModel specModel,
      DelegateMethodDescription methodDescription,
      MethodParamModel methodParamModel,
      String contextParamName) {
    if (isOutputType(methodParamModel.getTypeName())) {
      if (methodDescription.optionalParameterTypes.contains(
              DelegateMethodDescription.OptionalParameterType.INTER_STAGE_OUTPUT)
          || methodDescription.optionalParameterTypes.contains(
              DelegateMethodDescription.OptionalParameterType.PREPARE_INTER_STAGE_OUTPUT)) {
        if (contextParamName == null) {
          throw new IllegalStateException(
              "Cannot access param of type inter-stage prop in method "
                  + methodDescription.name
                  + " because it doesn't have a scoped ComponentContext as defined parameter.");
        }
        if (methodDescription.mInterStagePropsTarget.equals(
            DelegateMethodDescription.InterStagePropsTarget.PREPARE)) {
          return "getPrepareInterStagePropsContainerImpl("
              + contextParamName
              + ")."
              + methodParamModel.getName();
        } else {
          return "getInterStagePropsContainerImpl("
              + contextParamName
              + ", "
              + LOCAL_INTER_STAGE_PROPS_CONTAINER_NAME
              + ")."
              + methodParamModel.getName();
        }
      }
    }

    return getImplAccessor(
        methodDescription.name, specModel, methodParamModel, contextParamName, false);
  }

  /**
   * Calculate the level of the target component. The level here means how many bracket pairs are
   * needed to break until reaching the component type. For example, the level of {@literal
   * List<Component>} is 1, and the level of {@literal List<List<Component>>} is 2.
   *
   * @return the level of the target component, or 0 if the target isn't a component.
   */
  static int calculateLevelOfComponentInCollections(DeclaredTypeSpec typeSpec) {
    int level = 0;
    DeclaredTypeSpec declaredTypeSpec = typeSpec;
    while (declaredTypeSpec.isSubInterface(ClassNames.COLLECTION)) {
      Optional<DeclaredTypeSpec> result =
          declaredTypeSpec.getTypeArguments().stream()
              .filter(it -> it != null && it instanceof DeclaredTypeSpec)
              .findFirst()
              .map(it -> (DeclaredTypeSpec) it);
      if (!result.isPresent()) {
        return 0;
      }
      declaredTypeSpec = result.get();
      level++;
    }
    return declaredTypeSpec.isSubType(ClassNames.COMPONENT) ? level : 0;
  }

  private static CodeBlock getComponentCollectionCompareStatement(
      int level, DeclaredTypeSpec declaredTypeSpec, String it, String ref) {
    final TypeName argumentTypeName = declaredTypeSpec.getTypeArguments().get(0).getTypeName();
    CodeBlock.Builder builder =
        CodeBlock.builder()
            .addStatement(
                "$T<$L> _e1_$L = $L.iterator()", Iterator.class, argumentTypeName, level, it)
            .addStatement(
                "$T<$L> _e2_$L = $L.iterator()", Iterator.class, argumentTypeName, level, ref)
            .beginControlFlow("while (_e1_$L.hasNext() && _e2_$L.hasNext())", level, level);

    if (level == 1) {
      builder.add(
          CodeBlock.builder()
              .beginControlFlow("if (!_e1_$L.next().isEquivalentTo(_e2_$L.next()))", level, level)
              .addStatement("return false")
              .endControlFlow()
              .build());
    } else {
      builder
          .beginControlFlow("if (_e1_$L.next().size() != _e2_$L.next().size())", level, level)
          .addStatement("return false")
          .endControlFlow()
          .add(
              getComponentCollectionCompareStatement(
                  level - 1,
                  (DeclaredTypeSpec) declaredTypeSpec.getTypeArguments().get(0),
                  "_e1_" + level + ".next()",
                  "_e2_" + level + ".next()"));
    }
    return builder.endControlFlow().build();
  }

  public static boolean requiresInterStatePropContainer(
      ImmutableList<MethodParamModel> params,
      @Nullable ImmutableList<DelegateMethodDescription.OptionalParameterType> types) {

    final boolean allowsInterStageOutputs =
        types != null && types.stream().anyMatch(PREDICATE_ALLOWS_INTERSTAGE_OUTPUTS);

    Predicate<MethodParamModel> hasInterStageProps =
        param ->
            param instanceof InterStageInputParamModel
                || (allowsInterStageOutputs && isOutputType(param.getTypeName()));

    return params.stream().anyMatch(hasInterStageProps);
  }
}
