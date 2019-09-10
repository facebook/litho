/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.DYNAMIC_PROPS;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.PREVIOUS_RENDER_DATA_FIELD_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_CONTAINER_FIELD_NAME;
import static com.facebook.litho.specmodels.generator.StateContainerGenerator.getStateContainerClassName;

import com.facebook.litho.annotations.Comparable;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BindDynamicValueMethod;
import com.facebook.litho.specmodels.model.CachedValueParamModel;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

/** Class that generates the implementation of a Component. */
public class ComponentBodyGenerator {

  private ComponentBodyGenerator() {}

  public static TypeSpecDataHolder generate(
      SpecModel specModel, @Nullable MethodParamModel optionalField) {
    final TypeSpecDataHolder.Builder builder = TypeSpecDataHolder.newBuilder();

    final boolean hasState = !specModel.getStateValues().isEmpty();
    if (hasState) {
      final ClassName stateContainerClass =
          ClassName.bestGuess(getStateContainerClassName(specModel));
      builder.addField(
          FieldSpec.builder(stateContainerClass, STATE_CONTAINER_FIELD_NAME, Modifier.PRIVATE)
              .addAnnotation(
                  AnnotationSpec.builder(Comparable.class)
                      .addMember("type", "$L", Comparable.STATE_CONTAINER)
                      .build())
              .build());
      builder.addMethod(generateStateContainerGetter(specModel.getStateContainerClass()));
    }

    final boolean needsRenderDataInfra = !specModel.getRenderDataDiffs().isEmpty();
    if (needsRenderDataInfra) {
      final ClassName previousRenderDataClass =
          ClassName.bestGuess(RenderDataGenerator.getRenderDataImplClassName(specModel));
      builder.addField(previousRenderDataClass, PREVIOUS_RENDER_DATA_FIELD_NAME, Modifier.PRIVATE);
    }

    builder
        .addTypeSpecDataHolder(generateInjectedFields(specModel))
        .addTypeSpecDataHolder(generateProps(specModel))
        .addTypeSpecDataHolder(generateTreeProps(specModel))
        .addTypeSpecDataHolder(generateInterStageInputs(specModel))
        .addTypeSpecDataHolder(generateOptionalField(optionalField))
        .addTypeSpecDataHolder(generateEventHandlers(specModel))
        .addTypeSpecDataHolder(generateEventTriggers(specModel));

    if (specModel.shouldGenerateIsEquivalentTo()) {
      builder.addMethod(generateIsEquivalentMethod(specModel));
    }

    builder.addTypeSpecDataHolder(generateCopyInterStageImpl(specModel));
    builder.addTypeSpecDataHolder(generateMakeShallowCopy(specModel, hasState));
    builder.addTypeSpecDataHolder(generateGetDynamicProps(specModel));
    builder.addTypeSpecDataHolder(generateBindDynamicProp(specModel));

    if (hasState) {
      builder.addType(StateContainerGenerator.generate(specModel));
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
        TypeSpec.classBuilder(className).addSuperinterface(ClassNames.RENDER_DATA);

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
          "$L = $L.$L", name, recordParamName, getImplAccessor(specModel, modelToDiff));
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

  static MethodSpec generateStateContainerGetter(TypeName stateContainerClassName) {
    return MethodSpec.methodBuilder("getStateContainer")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(stateContainerClassName)
        .addStatement("return $N", STATE_CONTAINER_FIELD_NAME)
        .build();
  }

  public static TypeSpecDataHolder generateProps(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<PropModel> props = specModel.getProps();

    boolean hasDynamicProps = false;
    for (PropModel prop : props) {
      final TypeName propTypeName = prop.getTypeName();
      final TypeName fieldTypeName =
          !prop.isDynamic()
              ? propTypeName
              : ParameterizedTypeName.get(ClassNames.DYNAMIC_VALUE, propTypeName.box());

      final FieldSpec.Builder fieldBuilder =
          FieldSpec.builder(
                  KotlinSpecUtils.getFieldTypeName(specModel, fieldTypeName), prop.getName())
              .addAnnotations(prop.getExternalAnnotations())
              .addAnnotation(
                  AnnotationSpec.builder(Prop.class)
                      .addMember("resType", "$T.$L", ResType.class, prop.getResType())
                      .addMember("optional", "$L", prop.isOptional())
                      .build())
              .addAnnotation(
                  AnnotationSpec.builder(Comparable.class)
                      .addMember("type", "$L", getComparableType(fieldTypeName, prop.getTypeSpec()))
                      .build());
      if (prop.hasDefault(specModel.getPropDefaults())) {
        assignInitializer(fieldBuilder, specModel, prop);
      }

      typeSpecDataHolder.addField(fieldBuilder.build());

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
      FieldSpec.Builder fieldBuilder, SpecModel specModel, PropModel prop) {

    if (specModel.getSpecElementType() == SpecElementType.KOTLIN_SINGLETON) {
      final String propName = prop.getName();
      final String propAccessor =
          "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1) + "()";

      fieldBuilder.initializer("$L.$L.$L", specModel.getSpecName(), "INSTANCE", propAccessor);
    } else {
      fieldBuilder.initializer("$L.$L", specModel.getSpecName(), prop.getName());
    }
  }

  public static TypeSpecDataHolder generateInjectedFields(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    if (specModel.hasInjectedDependencies()) {
      typeSpecDataHolder.addTypeSpecDataHolder(
          specModel
              .getDependencyInjectionHelper()
              .generateInjectedFields(specModel.getInjectProps()));

      final List<MethodSpec> testAccessors =
          specModel.getInjectProps().stream()
              .map(p -> specModel.getDependencyInjectionHelper().generateTestingFieldAccessor(p))
              .collect(Collectors.toList());
      typeSpecDataHolder.addMethods(testAccessors);
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateTreeProps(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<TreePropModel> treeProps = specModel.getTreeProps();

    for (TreePropModel treeProp : treeProps) {
      typeSpecDataHolder.addField(
          FieldSpec.builder(treeProp.getTypeName(), treeProp.getName())
              .addAnnotation(TreeProp.class)
              .addAnnotation(
                  AnnotationSpec.builder(Comparable.class)
                      .addMember("type", "$L", getComparableType(specModel, treeProp))
                      .build())
              .build());
    }

    return typeSpecDataHolder.build();
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
                  ClassNames.EVENT_HANDLER, getEventHandlerInstanceName(eventDeclaration.name))
              .build());
    }

    return typeSpecDataHolder.build();
  }

  static String getEventHandlerInstanceName(ClassName eventHandlerClassName) {
    final String eventHandlerName = eventHandlerClassName.simpleName();
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

  static MethodSpec generateIsEquivalentMethod(SpecModel specModel) {
    final String className = specModel.getComponentName();
    final String instanceRefName = getInstanceRefName(specModel);

    MethodSpec.Builder isEquivalentBuilder =
        MethodSpec.methodBuilder("isEquivalentTo")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.BOOLEAN)
            .addParameter(specModel.getComponentClass(), "other")
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
      isEquivalentBuilder.addCode(getCompareStatement(specModel, instanceRefName, prop));
    }

    for (StateParamModel state : specModel.getStateValues()) {
      isEquivalentBuilder.addCode(getCompareStatement(specModel, instanceRefName, state));
    }

    for (TreePropModel treeProp : specModel.getTreeProps()) {
      isEquivalentBuilder.addCode(getCompareStatement(specModel, instanceRefName, treeProp));
    }

    isEquivalentBuilder.addStatement("return true");

    return isEquivalentBuilder.build();
  }

  static TypeSpecDataHolder generateCopyInterStageImpl(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<InterStageInputParamModel> interStageInputs =
        specModel.getInterStageInputs();

    if (specModel.shouldGenerateCopyMethod() && !interStageInputs.isEmpty()) {
      final String className = specModel.getComponentName();
      final String instanceName = getInstanceRefName(specModel);
      final MethodSpec.Builder copyInterStageComponentBuilder =
          MethodSpec.methodBuilder("copyInterStageImpl")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PROTECTED)
              .returns(TypeName.VOID)
              .addParameter(specModel.getComponentClass(), "component")
              .addStatement("$N $N = ($N) component", className, instanceName, className);

      for (InterStageInputParamModel interStageInput : interStageInputs) {
        copyInterStageComponentBuilder.addStatement(
            "$N = $N.$N", interStageInput.getName(), instanceName, interStageInput.getName());
      }

      typeSpecDataHolder.addMethod(copyInterStageComponentBuilder.build());
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

    if (hasDeepCopy) {
      builder.beginControlFlow("if (!deepCopy)");
    }

    for (InterStageInputParamModel interStageInput : specModel.getInterStageInputs()) {
      builder.addStatement("component.$L = null", interStageInput.getName());
    }

    final String stateContainerClassName = getStateContainerClassName(specModel);
    if (hasState) {
      builder.addStatement(
          "component.$N = new $T()",
          STATE_CONTAINER_FIELD_NAME,
          ClassName.bestGuess(stateContainerClassName));
    }

    if (hasDeepCopy) {
      builder.endControlFlow();
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

  private static List<MethodParamModel> getParams(
      SpecMethodModel<UpdateStateMethod, Void> updateStateMethodModel) {
    final List<MethodParamModel> params = new ArrayList<>();
    for (MethodParamModel methodParamModel : updateStateMethodModel.methodParams) {
      for (Annotation annotation : methodParamModel.getAnnotations()) {
        if (annotation.annotationType().equals(Param.class)) {
          params.add(methodParamModel);
          break;
        }
      }
    }

    return params;
  }

  static CodeBlock getCompareStatement(
      SpecModel specModel, String implInstanceName, MethodParamModel field) {
    final String implAccessor = getImplAccessor(specModel, field, true);

    return getCompareStatement(
        specModel, field, implAccessor, implInstanceName + "." + implAccessor);
  }

  static CodeBlock getCompareStatement(
      SpecModel specModel,
      MethodParamModel field,
      String firstComparator,
      String secondComparator) {
    final CodeBlock.Builder codeBlock = CodeBlock.builder();

    @Comparable.Type int comparableType = getComparableType(specModel, field);
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

  static @Comparable.Type int getComparableType(SpecModel specModel, MethodParamModel field) {
    return getComparableType(field.getTypeName(), field.getTypeSpec());
  }

  private static @Comparable.Type int getComparableType(
      TypeName typeName, com.facebook.litho.specmodels.model.TypeSpec typeSpec) {
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
    } else if (typeSpec.isSubInterface(ClassNames.COLLECTION)) {
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

  static String getImplAccessor(SpecModel specModel, MethodParamModel methodParamModel) {
    return getImplAccessor(specModel, methodParamModel, false);
  }

  static String getImplAccessor(
      SpecModel specModel, MethodParamModel methodParamModel, boolean shallow) {
    if (methodParamModel instanceof StateParamModel
        || SpecModelUtils.getStateValueWithName(specModel, methodParamModel.getName()) != null) {
      return STATE_CONTAINER_FIELD_NAME + "." + methodParamModel.getName();
    } else if (methodParamModel instanceof CachedValueParamModel) {
      return "get"
          + methodParamModel.getName().substring(0, 1).toUpperCase()
          + methodParamModel.getName().substring(1)
          + "()";
    } else if (methodParamModel instanceof PropModel
        && ((PropModel) methodParamModel).isDynamic()
        && !shallow) {
      return "retrieveValue(" + methodParamModel.getName() + ")";
    }

    return methodParamModel.getName();
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
}
