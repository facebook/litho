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

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.DYNAMIC_PROPS;
import static com.facebook.litho.specmodels.generator.GeneratorUtils.annotation;
import static com.facebook.litho.specmodels.generator.GeneratorUtils.parameter;

import com.facebook.litho.annotations.RequiredProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecElementType;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
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
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import javax.lang.model.element.Modifier;

/** Class that generates the builder for a Component. */
public class BuilderGenerator {

  private static final String BUILDER = "Builder";
  private static final String RESOURCE_RESOLVER = "mResourceResolver";
  private static final ClassName BUILDER_CLASS_NAME = ClassName.bestGuess(BUILDER);
  private static final String CONTEXT_MEMBER_NAME = "mContext";
  private static final String CONTEXT_PARAM_NAME = "context";
  private static final String REQUIRED_PROPS_NAMES = "REQUIRED_PROPS_NAMES";
  private static final String REQUIRED_PROPS_COUNT = "REQUIRED_PROPS_COUNT";

  private BuilderGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generateFactoryMethods(specModel))
        .addTypeSpecDataHolder(generateBuilder(specModel))
        .build();
  }

  static TypeSpecDataHolder generateFactoryMethods(SpecModel specModel) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    final MethodSpec.Builder factoryMethod =
        MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.STATIC)
            .returns(getBuilderType(specModel))
            .addParameter(specModel.getContextClass(), "context")
            .addStatement("final $1T builder = new $1T()", BUILDER_CLASS_NAME);

    if (!specModel.getTypeVariables().isEmpty()) {
      factoryMethod.addTypeVariables(specModel.getTypeVariables());
    }

    if (specModel.hasInjectedDependencies()) {
      factoryMethod.addCode(
          specModel
              .getDependencyInjectionHelper()
              .generateFactoryMethodsComponentInstance(specModel));
    } else {
      factoryMethod.addStatement(
          "$L instance = new $L()", specModel.getComponentName(), specModel.getComponentName());
    }

    if (specModel.isStylingSupported()) {
      dataHolder.addMethod(generateDelegatingCreateBuilderMethod(specModel));
      factoryMethod
          .addParameter(int.class, "defStyleAttr")
          .addParameter(int.class, "defStyleRes")
          .addStatement("builder.init(context, defStyleAttr, defStyleRes, $L)", "instance");
    } else {
      factoryMethod.addStatement("builder.init(context, $L)", "instance");
    }

    factoryMethod.addStatement("return builder");

    return dataHolder.addMethod(factoryMethod.build()).build();
  }

  private static MethodSpec generateDelegatingCreateBuilderMethod(SpecModel specModel) {
    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.PUBLIC)
            .returns(getBuilderType(specModel))
            .addParameter(specModel.getContextClass(), "context")
            .addStatement("return create(context, 0, 0)")
            .addModifiers(Modifier.STATIC);

    if (!specModel.getTypeVariables().isEmpty()) {
      methodBuilder.addTypeVariables(specModel.getTypeVariables());
    }

    return methodBuilder.build();
  }

  static TypeSpecDataHolder generateBuilder(SpecModel specModel) {
    final String componentName = specModel.getComponentName();
    final String componentInstanceRefName = ComponentBodyGenerator.getInstanceRefName(specModel);
    final String componentMemberInstanceName = getComponentMemberInstanceName(specModel);
    final ClassName componentClass = ClassName.bestGuess(componentName);
    final MethodSpec.Builder initMethodSpec =
        MethodSpec.methodBuilder("init")
            .addModifiers(Modifier.PRIVATE)
            .addParameter(specModel.getContextClass(), CONTEXT_PARAM_NAME);

    final ImmutableList<PropDefaultModel> propDefaults = specModel.getPropDefaults();
    boolean isResResolvable = false;

    for (PropDefaultModel propDefault : propDefaults) {
      if (propDefault.isResResolvable()) {
        isResResolvable = true;
        break;
      }
    }

    if (specModel.isStylingSupported()) {
      initMethodSpec
          .addParameter(int.class, "defStyleAttr")
          .addParameter(int.class, "defStyleRes")
          .addParameter(componentClass, componentInstanceRefName)
          .addStatement(
              "super.init(context, defStyleAttr, defStyleRes, $L)", componentInstanceRefName);
    } else {
      initMethodSpec
          .addParameter(componentClass, componentInstanceRefName)
          .addStatement("super.init(context, $L)", componentInstanceRefName);
    }

    initMethodSpec
        .addStatement("$L = $L", componentMemberInstanceName, componentInstanceRefName)
        .addStatement("$L = $L", CONTEXT_MEMBER_NAME, CONTEXT_PARAM_NAME);

    if (isResResolvable) {
      initMethodSpec.addStatement("initPropDefaults()");
    }

    final MethodSpec.Builder setComponentMethodSpec =
        MethodSpec.methodBuilder("setComponent")
            .addModifiers(Modifier.PROTECTED)
            .addAnnotation(Override.class)
            .addParameter(ClassNames.COMPONENT, "component")
            .addStatement(
                "$L = ($T) component",
                getComponentMemberInstanceName(specModel),
                specModel.getComponentTypeName());

    final boolean builderHasTypeVariables = !specModel.getTypeVariables().isEmpty();

    TypeName builderType = getBuilderType(specModel);

    final TypeSpec.Builder propsBuilderClassBuilder =
        TypeSpec.classBuilder(BUILDER)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .superclass(
                ParameterizedTypeName.get(
                    ClassName.get(
                        specModel.getComponentClass().packageName(),
                        specModel.getComponentClass().simpleName(),
                        BUILDER),
                    builderType))
            .addField(componentClass, componentMemberInstanceName)
            .addField(specModel.getContextClass(), CONTEXT_MEMBER_NAME);

    if (builderHasTypeVariables) {
      propsBuilderClassBuilder.addTypeVariables(specModel.getTypeVariables());
    }

    final List<String> requiredPropNames = new ArrayList<>();
    int numRequiredProps = 0;
    for (PropModel prop : specModel.getProps()) {
      if (!prop.isOptional()) {
        numRequiredProps++;
        requiredPropNames.add(prop.getName());
      }
    }

    if (numRequiredProps > 0) {
      propsBuilderClassBuilder.addField(
          FieldSpec.builder(String[].class, REQUIRED_PROPS_NAMES, Modifier.PRIVATE, Modifier.FINAL)
              .initializer("new String[] {$L}", commaSeparateAndQuoteStrings(requiredPropNames))
              .build());

      propsBuilderClassBuilder.addField(
          FieldSpec.builder(int.class, REQUIRED_PROPS_COUNT, Modifier.PRIVATE, Modifier.FINAL)
              .initializer("$L", numRequiredProps)
              .build());

      propsBuilderClassBuilder.addField(
          FieldSpec.builder(BitSet.class, "mRequired", Modifier.PRIVATE, Modifier.FINAL)
              .initializer("new $T($L)", BitSet.class, REQUIRED_PROPS_COUNT)
              .build());

      initMethodSpec.addStatement("mRequired.clear()");
    }

    propsBuilderClassBuilder.addMethod(initMethodSpec.build());

    if (specModel instanceof LayoutSpecModel || specModel instanceof MountSpecModel) {
      propsBuilderClassBuilder.addMethod(setComponentMethodSpec.build());
    }

    if (isResResolvable) {
      MethodSpec.Builder initResTypePropDefaultsSpec = MethodSpec.methodBuilder("initPropDefaults");

      for (PropDefaultModel propDefault : propDefaults) {
        if (!propDefault.isResResolvable()) {
          continue;
        }

        initResTypePropDefaultsSpec.addStatement(
            "this.$L.$L = $L",
            getComponentMemberInstanceName(specModel),
            propDefault.getName(),
            generatePropsDefaultInitializers(specModel, propDefault));
      }

      propsBuilderClassBuilder.addMethod(initResTypePropDefaultsSpec.build());
    }

    int requiredPropIndex = 0;
    for (PropModel prop : specModel.getProps()) {
      generatePropsBuilderMethods(specModel, prop, requiredPropIndex)
          .addToTypeSpec(propsBuilderClassBuilder);

      if (!prop.isOptional()) {
        requiredPropIndex++;
      }
    }

    for (EventDeclarationModel eventDeclaration : specModel.getEventDeclarations()) {
      propsBuilderClassBuilder.addMethod(
          generateEventDeclarationBuilderMethod(specModel, eventDeclaration));
    }

    for (SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethod :
        specModel.getTriggerMethods()) {
      propsBuilderClassBuilder.addMethod(
          generateEventTriggerBuilderMethod(specModel, triggerMethod));

      propsBuilderClassBuilder.addMethod(
          generateEventTriggerChangeKeyMethod(specModel, triggerMethod));
    }

    if (!specModel.getTriggerMethods().isEmpty()) {
      propsBuilderClassBuilder.addMethod(
          generateRegisterEventTriggersMethod(specModel.getTriggerMethods()));
    }

    for (BuilderMethodModel builderMethodModel : specModel.getExtraBuilderMethods()) {
      if (builderMethodModel.paramName.equals("key") && !specModel.getTriggerMethods().isEmpty()) {
        // The key setter method has been created if we have trigger methods, ignore it.
        continue;
      }
      propsBuilderClassBuilder.addMethod(generateExtraBuilderMethod(specModel, builderMethodModel));
    }

    propsBuilderClassBuilder
        .addMethod(generateGetThisMethod(specModel))
        .addMethod(generateBuildMethod(specModel, numRequiredProps));

    return TypeSpecDataHolder.newBuilder().addType(propsBuilderClassBuilder.build()).build();
  }

  // Returns either Builder or a Builder<Generic.. >
  private static TypeName getBuilderType(SpecModel specModel) {
    return (specModel.getTypeVariables().isEmpty())
        ? BUILDER_CLASS_NAME
        : ParameterizedTypeName.get(
            BUILDER_CLASS_NAME,
            specModel
                .getTypeVariables()
                .toArray(new TypeName[specModel.getTypeVariables().size()]));
  }

  private static String getComponentMemberInstanceName(SpecModel specModel) {
    return "m" + specModel.getComponentName();
  }

  private static String commaSeparateAndQuoteStrings(List<String> strings) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < strings.size(); i++) {
      sb.append('"');
      sb.append(strings.get(i));
      sb.append('"');
      if (i < strings.size() - 1) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  static TypeSpecDataHolder generatePropsBuilderMethods(
      SpecModel specModel, PropModel prop, int requiredIndex) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    final boolean hasVarArgs = prop.hasVarArgs();

    switch (prop.getResType()) {
      case STRING:
        dataHolder.addTypeSpecDataHolder(regularBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(specModel, prop, requiredIndex, ClassNames.STRING_RES, "resolveString"));
        dataHolder.addTypeSpecDataHolder(
            resWithVarargsBuilders(
                specModel,
                prop,
                requiredIndex,
                ClassNames.STRING_RES,
                "resolveString",
                TypeName.OBJECT,
                "formatArgs"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.STRING_RES, "resolveString"));
        break;
      case STRING_ARRAY:
        dataHolder.addTypeSpecDataHolder(regularBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(
                specModel, prop, requiredIndex, ClassNames.ARRAY_RES, "resolveStringArray"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(
                specModel, prop, requiredIndex, ClassNames.ARRAY_RES, "resolveStringArray"));
        break;
      case INT:
        dataHolder.addTypeSpecDataHolder(regularBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(specModel, prop, requiredIndex, ClassNames.INT_RES, "resolveInt"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.INT_RES, "resolveInt"));
        break;
      case INT_ARRAY:
        dataHolder.addTypeSpecDataHolder(regularBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(
                specModel,
                prop,
                requiredIndex,
                ClassNames.ARRAY_RES,
                hasVarArgs ? "resolveIntegerArray" : "resolveIntArray"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(
                specModel,
                prop,
                requiredIndex,
                ClassNames.ARRAY_RES,
                hasVarArgs ? "resolveIntegerArray" : "resolveIntArray"));
        break;
      case BOOL:
        dataHolder.addTypeSpecDataHolder(regularBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(specModel, prop, requiredIndex, ClassNames.BOOL_RES, "resolveBool"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.BOOL_RES, "resolveBool"));
        break;
      case COLOR:
        dataHolder.addTypeSpecDataHolder(
            regularBuilders(specModel, prop, requiredIndex, annotation(ClassNames.COLOR_INT)));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(specModel, prop, requiredIndex, ClassNames.COLOR_RES, "resolveColor"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.COLOR_RES, "resolveColor"));
        break;
      case DIMEN_SIZE:
        dataHolder.addTypeSpecDataHolder(pxBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(dipBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        break;
      case DIMEN_TEXT:
        dataHolder.addTypeSpecDataHolder(pxBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(dipBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(sipBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        break;
      case DIMEN_OFFSET:
        dataHolder.addTypeSpecDataHolder(pxBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(dipBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(sipBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        break;
      case FLOAT:
        dataHolder.addTypeSpecDataHolder(regularBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveFloat"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveFloat"));
        break;
      case DRAWABLE:
        dataHolder.addTypeSpecDataHolder(regularBuilders(specModel, prop, requiredIndex));
        dataHolder.addTypeSpecDataHolder(
            resBuilders(
                specModel, prop, requiredIndex, ClassNames.DRAWABLE_RES, "resolveDrawable"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(
                specModel, prop, requiredIndex, ClassNames.DRAWABLE_RES, "resolveDrawable"));
        break;
      case NONE:
        if (hasVarArgs) {
          dataHolder.addMethod(varArgBuilder(specModel, prop, requiredIndex));
          ParameterizedTypeName type = (ParameterizedTypeName) prop.getTypeName();
          if (getRawType(type.typeArguments.get(0)).equals(ClassNames.COMPONENT)) {
            dataHolder.addMethod(varArgBuilderBuilder(specModel, prop, requiredIndex));
          }
          // fall through to generate builder method for List<T>
        }

        final TypeName componentClass =
            prop.getTypeName() instanceof ParameterizedTypeName
                ? ((ParameterizedTypeName) prop.getTypeName()).rawType
                : prop.getTypeName();

        if (componentClass.equals(ClassNames.COMPONENT)) {
          dataHolder.addMethod(componentBuilder(specModel, prop, requiredIndex));
        } else if (prop.isDynamic()) {
          final TypeName dynamicValueType =
              ParameterizedTypeName.get(ClassNames.DYNAMIC_VALUE, prop.getTypeName().box());
          dataHolder.addMethod(
              dynamicValueBuilder(specModel, prop, requiredIndex, dynamicValueType));
          dataHolder.addMethod(
              dynamicValueSimpleBuilder(specModel, prop, requiredIndex, dynamicValueType));
        } else {
          dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        }
        break;
    }

    if (getRawType(prop.getTypeName()).equals(ClassNames.COMPONENT)) {
      dataHolder.addMethod(
          builderBuilder(specModel, prop, requiredIndex, ClassNames.COMPONENT_BUILDER, true));
    }

    if (getRawType(prop.getTypeName()).equals(ClassNames.SECTION)) {
      dataHolder.addMethod(
          builderBuilder(specModel, prop, requiredIndex, ClassNames.SECTION_BUILDER, true));
    }

    return dataHolder.build();
  }

  static String generatePropsDefaultInitializers(
      SpecModel specModel, PropDefaultModel propDefault) {

    switch (propDefault.getResType()) {
      case STRING:
        return generatePropDefaultResInitializer("resolveStringRes", propDefault, specModel);
      case STRING_ARRAY:
        return generatePropDefaultResInitializer("resolveStringArrayRes", propDefault, specModel);
      case INT:
        return generatePropDefaultResInitializer("resolveIntRes", propDefault, specModel);
      case INT_ARRAY:
        return generatePropDefaultResInitializer("resolveIntArrayRes", propDefault, specModel);
      case BOOL:
        return generatePropDefaultResInitializer("resolveBoolRes", propDefault, specModel);
      case COLOR:
        return generatePropDefaultResInitializer("resolveColorRes", propDefault, specModel);
      case DIMEN_SIZE:
        return generatePropDefaultResInitializer("resolveDimenSizeRes", propDefault, specModel);
      case DIMEN_TEXT:
        return generatePropDefaultResInitializer("resolveDimenSizeRes", propDefault, specModel);
      case DIMEN_OFFSET:
        return generatePropDefaultResInitializer("resolveDimenOffsetRes", propDefault, specModel);
      case FLOAT:
        return generatePropDefaultResInitializer("resolveFloatRes", propDefault, specModel);
      case DRAWABLE:
        return generatePropDefaultResInitializer("resolveDrawableRes", propDefault, specModel);
      case NONE:
        break;
    }

    return "";
  }

  static TypeName getRawType(TypeName type) {
    return type instanceof ParameterizedTypeName ? ((ParameterizedTypeName) type).rawType : type;
  }

  private static MethodSpec componentBuilder(
      SpecModel specModel, PropModel prop, int requiredIndex) {
    return getMethodSpecBuilder(
            specModel,
            prop,
            requiredIndex,
            prop.getName(),
            Arrays.asList(parameter(prop)),
            "$L == null ? null : $L.makeShallowCopy()",
            prop.getName(),
            prop.getName())
        .build();
  }

  private static MethodSpec dynamicValueBuilder(
      SpecModel specModel, PropModel prop, int requiredIndex, TypeName dynamicValueType) {
    return getMethodSpecBuilder(
            specModel,
            prop,
            requiredIndex,
            prop.getName(),
            Arrays.asList(
                parameter(
                    prop,
                    KotlinSpecUtils.getFieldTypeName(specModel, dynamicValueType),
                    prop.getName())),
            prop.getName())
        .build();
  }

  private static MethodSpec dynamicValueSimpleBuilder(
      SpecModel specModel, PropModel prop, int requiredIndex, TypeName dynamicValueType) {
    return getMethodSpecBuilder(
            specModel,
            prop,
            requiredIndex,
            prop.getName(),
            Arrays.asList(
                parameter(
                    prop,
                    KotlinSpecUtils.getFieldTypeName(specModel, prop.getTypeName()),
                    prop.getName())),
            "new $T($L)",
            dynamicValueType,
            prop.getName())
        .build();
  }

  private static MethodSpec regularBuilder(
      SpecModel specModel, PropModel prop, int requiredIndex, AnnotationSpec... extraAnnotations) {
    return getMethodSpecBuilder(
            specModel,
            prop,
            requiredIndex,
            prop.getName(),
            Arrays.asList(
                parameter(
                    prop,
                    KotlinSpecUtils.getFieldTypeName(specModel, prop.getTypeName()),
                    prop.getName(),
                    extraAnnotations)),
            prop.getName())
        .build();
  }

  private static MethodSpec varArgBuilder(
      SpecModel specModel, PropModel prop, int requiredIndex, AnnotationSpec... extraAnnotations) {
    String varArgName = prop.getVarArgsSingleName();

    final ParameterizedTypeName varArgType = (ParameterizedTypeName) prop.getTypeName();
    final TypeName varArgTypeArgumentTypeName = varArgType.typeArguments.get(0);
    final TypeName varArgTypeName = getParameterTypeName(specModel, varArgTypeArgumentTypeName);

    CodeBlock.Builder codeBlockBuilder =
        CodeBlock.builder()
            .beginControlFlow("if ($L == null)", varArgName)
            .addStatement("return this")
            .endControlFlow();

    createListIfDefault(codeBlockBuilder, specModel, prop, varArgTypeName);

    CodeBlock codeBlock =
        codeBlockBuilder
            .addStatement(
                "this.$L.$L.add($L)",
                getComponentMemberInstanceName(specModel),
                prop.getName(),
                varArgName)
            .build();

    return getMethodSpecBuilder(
            specModel,
            prop,
            requiredIndex,
            varArgName,
            Arrays.asList(parameter(prop, varArgTypeName, varArgName, extraAnnotations)),
            codeBlock)
        .build();
  }

  /**
   * Adds code to the {@code codeBlockBuilder}, which checks if {@code varArgProp} member variable
   * equals default value and assigns a new list if true.
   *
   * <pre>{@code
   * if (this.mComponentName.varArgPropName == ...) {
   *   this.mComponentName.varArgPropName = new ...;
   * }
   * }</pre>
   */
  private static void createListIfDefault(
      CodeBlock.Builder codeBlockBuilder,
      SpecModel specModel,
      PropModel varArgProp,
      TypeName varArgParameterType) {
    final String varArgPropName = varArgProp.getName();
    final String componentMemberInstanceName = getComponentMemberInstanceName(specModel);
    if (varArgProp.hasDefault(specModel.getPropDefaults())) {
      codeBlockBuilder.beginControlFlow(
          "if (this.$L.$L == null || this.$L.$L == $L.$L)",
          componentMemberInstanceName,
          varArgPropName,
          componentMemberInstanceName,
          varArgPropName,
          specModel.getSpecName(),
          varArgPropName);
    } else {
      codeBlockBuilder.beginControlFlow(
          "if (this.$L.$L == null)", componentMemberInstanceName, varArgPropName);
    }
    final ParameterizedTypeName listType =
        ParameterizedTypeName.get(ClassName.get(ArrayList.class), varArgParameterType);
    codeBlockBuilder
        .addStatement(
            "this.$L.$L = new $T()", componentMemberInstanceName, varArgPropName, listType)
        .endControlFlow();
  }

  private static TypeName getParameterTypeName(
      SpecModel specModel, TypeName varArgTypeArgumentTypeName) {

    final String rawVarArgType = varArgTypeArgumentTypeName.toString();
    final boolean isKotlinSpec = specModel.getSpecElementType() == SpecElementType.KOTLIN_SINGLETON;

    TypeName varArgTypeName;

    if (isKotlinSpec) {
      final boolean isNotJvmSuppressWildcardsAnnotated =
          KotlinSpecUtils.isNotJvmSuppressWildcardsAnnotated(rawVarArgType);

      /*
       * If it is a JvmSuppressWildcards annotated type on a Kotlin Spec,
       * we should fallback to previous type detection way.
       * */
      if (!isNotJvmSuppressWildcardsAnnotated) {
        varArgTypeName = varArgTypeArgumentTypeName;
      } else {
        final String[] typeParts = rawVarArgType.split(" ");

        // Just in case something has gone pretty wrong
        if (typeParts.length < 3) {
          varArgTypeName = varArgTypeArgumentTypeName;
        } else {
          // Calculate appropriate ClassName
          final String pureTypeName = typeParts[2];
          varArgTypeName = KotlinSpecUtils.buildClassName(pureTypeName);
        }
      }
    } else {
      // Fallback when it is a Java spec
      varArgTypeName = varArgTypeArgumentTypeName;
    }
    return varArgTypeName;
  }

  private static MethodSpec varArgBuilderBuilder(
      SpecModel specModel, PropModel prop, int requiredIndex) {
    String varArgName = prop.getVarArgsSingleName();
    final ParameterizedTypeName varArgType = (ParameterizedTypeName) prop.getTypeName();
    final TypeName internalType = varArgType.typeArguments.get(0);
    CodeBlock codeBlock =
        CodeBlock.builder()
            .beginControlFlow("if ($L == null)", varArgName + "Builder")
            .addStatement("return this")
            .endControlFlow()
            .addStatement("$L($L.build())", varArgName, varArgName + "Builder")
            .build();
    TypeName builderParameterType =
        ParameterizedTypeName.get(
            ClassNames.COMPONENT_BUILDER,
            getBuilderGenericTypes(internalType, ClassNames.COMPONENT_BUILDER));
    return getMethodSpecBuilder(
            specModel,
            prop,
            requiredIndex,
            varArgName,
            Arrays.asList(parameter(prop, builderParameterType, varArgName + "Builder")),
            codeBlock)
        .build();
  }

  private static TypeSpecDataHolder regularBuilders(
      SpecModel specModel, PropModel prop, int requiredIndex, AnnotationSpec... extraAnnotations) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex, extraAnnotations));
    if (prop.hasVarArgs()) {
      dataHolder.addMethod(varArgBuilder(specModel, prop, requiredIndex));
    }
    return dataHolder.build();
  }

  private static TypeSpecDataHolder resBuilders(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      ClassName annotationClassName,
      String resolver) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    final boolean hasVarArgs = prop.hasVarArgs();
    final String name = hasVarArgs ? prop.getVarArgsSingleName() : prop.getName();

    final String typeCast =
        !prop.hasVarArgs() && prop.getTypeName().equals(ClassName.FLOAT.box()) ? "(float) " : "";

    dataHolder.addMethod(
        resTypeRegularBuilder(
                specModel,
                prop,
                requiredIndex,
                name + "Res",
                Arrays.asList(
                    parameterWithoutNullableAnnotation(
                        prop, TypeName.INT, "resId", annotation(annotationClassName))),
                "$L$L.$L(resId)",
                typeCast,
                RESOURCE_RESOLVER,
                resolver + "Res")
            .build());

    if (hasVarArgs) {
      dataHolder.addMethod(
          resTypeListBuilder(
                  specModel,
                  prop,
                  requiredIndex,
                  prop.getName() + "Res",
                  Arrays.asList(
                      parameterWithoutNullableAnnotation(
                          prop,
                          ParameterizedTypeName.get(ClassNames.LIST, TypeName.INT.box()),
                          "resIds")),
                  "$L$L.$L(resIds.get(i))",
                  typeCast,
                  RESOURCE_RESOLVER,
                  resolver + "Res")
              .build());
    }
    return dataHolder.build();
  }

  private static TypeSpecDataHolder resWithVarargsBuilders(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      ClassName annotationClassName,
      String resolver,
      TypeName varargsType,
      String varargsName) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    final boolean hasVarArgs = prop.hasVarArgs();
    final String name = hasVarArgs ? prop.getVarArgsSingleName() : prop.getName();

    dataHolder.addMethod(
        resTypeRegularBuilder(
                specModel,
                prop,
                requiredIndex,
                name + "Res",
                Arrays.asList(
                    parameterWithoutNullableAnnotation(
                        prop, TypeName.INT, "resId", annotation(annotationClassName)),
                    ParameterSpec.builder(ArrayTypeName.of(varargsType), varargsName).build()),
                "$L.$L(resId, $L)",
                RESOURCE_RESOLVER,
                resolver + "Res",
                varargsName)
            .varargs(true)
            .build());

    if (hasVarArgs) {
      dataHolder.addMethod(
          resTypeListBuilder(
                  specModel,
                  prop,
                  requiredIndex,
                  prop.getName() + "Res",
                  Arrays.asList(
                      parameterWithoutNullableAnnotation(
                          prop,
                          ParameterizedTypeName.get(ClassNames.LIST, TypeName.INT.box()),
                          "resIds",
                          annotation(annotationClassName)),
                      ParameterSpec.builder(ArrayTypeName.of(varargsType), varargsName).build()),
                  "$L.$L(resIds.get(i), $L)",
                  RESOURCE_RESOLVER,
                  resolver + "Res",
                  varargsName)
              .varargs(true)
              .build());
    }

    return dataHolder.build();
  }

  private static TypeSpecDataHolder attrBuilders(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      ClassName annotationClassName,
      String resolver) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    final boolean hasVarArgs = prop.hasVarArgs();
    final String name = hasVarArgs ? prop.getVarArgsSingleName() : prop.getName();
    final String typeCast =
        !prop.hasVarArgs() && prop.getTypeName().equals(ClassName.FLOAT.box()) ? "(float) " : "";

    dataHolder.addMethod(
        resTypeRegularBuilder(
                specModel,
                prop,
                requiredIndex,
                name + "Attr",
                Arrays.asList(
                    parameterWithoutNullableAnnotation(
                        prop, TypeName.INT, "attrResId", annotation(ClassNames.ATTR_RES)),
                    parameterWithoutNullableAnnotation(
                        prop, TypeName.INT, "defResId", annotation(annotationClassName))),
                "$L$L.$L(attrResId, defResId)",
                typeCast,
                RESOURCE_RESOLVER,
                resolver + "Attr")
            .build());

    dataHolder.addMethod(
        resTypeRegularBuilder(
                specModel,
                prop,
                requiredIndex,
                name + "Attr",
                Arrays.asList(
                    parameterWithoutNullableAnnotation(
                        prop, TypeName.INT, "attrResId", annotation(ClassNames.ATTR_RES))),
                "$L$L.$L(attrResId, 0)",
                typeCast,
                RESOURCE_RESOLVER,
                resolver + "Attr")
            .build());

    if (hasVarArgs) {
      dataHolder.addMethod(
          resTypeListBuilder(
                  specModel,
                  prop,
                  requiredIndex,
                  prop.getName() + "Attr",
                  Arrays.asList(
                      parameterWithoutNullableAnnotation(
                          prop,
                          ParameterizedTypeName.get(ClassNames.LIST, TypeName.INT.box()),
                          "attrResIds"),
                      parameterWithoutNullableAnnotation(
                          prop, TypeName.INT, "defResId", annotation(annotationClassName))),
                  "$L$L.$L(attrResIds.get(i), defResId)",
                  typeCast,
                  RESOURCE_RESOLVER,
                  resolver + "Attr")
              .build());

      dataHolder.addMethod(
          resTypeListBuilder(
                  specModel,
                  prop,
                  requiredIndex,
                  prop.getName() + "Attr",
                  Arrays.asList(
                      parameterWithoutNullableAnnotation(
                          prop,
                          ParameterizedTypeName.get(ClassNames.LIST, TypeName.INT.box()),
                          "attrResIds")),
                  "$L$L.$L(attrResIds.get(i), 0)",
                  typeCast,
                  RESOURCE_RESOLVER,
                  resolver + "Attr")
              .build());
    }

    return dataHolder.build();
  }

  private static TypeSpecDataHolder pxBuilders(
      SpecModel specModel, PropModel prop, int requiredIndex) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    final boolean hasVarArgs = prop.hasVarArgs();
    final String name = hasVarArgs ? prop.getVarArgsSingleName() : prop.getName();

    dataHolder.addMethod(
        resTypeRegularBuilder(
                specModel,
                prop,
                requiredIndex,
                name + "Px",
                Arrays.asList(
                    parameterWithoutNullableAnnotation(
                        prop,
                        hasVarArgs
                            ? ((ParameterizedTypeName) prop.getTypeName())
                                .typeArguments
                                .get(0)
                                .unbox()
                            : prop.getTypeName().unbox(),
                        name,
                        annotation(ClassNames.PX))),
                name)
            .build());

    if (hasVarArgs) {
      dataHolder.addMethod(
          resTypeListBuilder(
                  specModel,
                  prop,
                  requiredIndex,
                  prop.getName() + "Px",
                  Arrays.asList(
                      parameterWithoutNullableAnnotation(prop, prop.getTypeName(), prop.getName())),
                  prop.getName() + ".get(i)")
              .build());
    }

    return dataHolder.build();
  }

  private static TypeSpecDataHolder dipBuilders(
      SpecModel specModel, PropModel prop, int requiredIndex) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    final boolean hasVarArgs = prop.hasVarArgs();
    final String name = hasVarArgs ? prop.getVarArgsSingleName() : prop.getName();
    final String statement =
        !prop.hasVarArgs() && prop.getTypeName().equals(ClassName.FLOAT.box())
            ? "(float) mResourceResolver.dipsToPixels(dip)"
            : "mResourceResolver.dipsToPixels(dip)";

    AnnotationSpec dipAnnotation =
        AnnotationSpec.builder(ClassNames.DIMENSION)
            .addMember("unit", "$T.DP", ClassNames.DIMENSION)
            .build();

    dataHolder.addMethod(
        resTypeRegularBuilder(
                specModel,
                prop,
                requiredIndex,
                name + "Dip",
                Arrays.asList(
                    parameterWithoutNullableAnnotation(prop, TypeName.FLOAT, "dip", dipAnnotation)),
                statement)
            .build());

    if (hasVarArgs) {
      dataHolder.addMethod(
          resTypeListBuilder(
                  specModel,
                  prop,
                  requiredIndex,
                  prop.getName() + "Dip",
                  Arrays.asList(
                      parameterWithoutNullableAnnotation(
                          prop,
                          ParameterizedTypeName.get(ClassNames.LIST, TypeName.FLOAT.box()),
                          "dips")),
                  "mResourceResolver.dipsToPixels(dips.get(i))")
              .build());
    }

    return dataHolder.build();
  }

  private static TypeSpecDataHolder sipBuilders(
      SpecModel specModel, PropModel prop, int requiredIndex) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    final boolean hasVarArgs = prop.hasVarArgs();
    final String name = hasVarArgs ? prop.getVarArgsSingleName() : prop.getName();
    final String statement =
        !prop.hasVarArgs() && prop.getTypeName().equals(ClassName.FLOAT.box())
            ? "(float) mResourceResolver.sipsToPixels(sip)"
            : "mResourceResolver.sipsToPixels(sip)";

    AnnotationSpec spAnnotation =
        AnnotationSpec.builder(ClassNames.DIMENSION)
            .addMember("unit", "$T.SP", ClassNames.DIMENSION)
            .build();

    dataHolder.addMethod(
        resTypeRegularBuilder(
                specModel,
                prop,
                requiredIndex,
                name + "Sp",
                Arrays.asList(
                    parameterWithoutNullableAnnotation(prop, TypeName.FLOAT, "sip", spAnnotation)),
                statement)
            .build());

    if (hasVarArgs) {
      dataHolder.addMethod(
          resTypeListBuilder(
                  specModel,
                  prop,
                  requiredIndex,
                  prop.getName() + "Sp",
                  Arrays.asList(
                      parameterWithoutNullableAnnotation(
                          prop,
                          ParameterizedTypeName.get(ClassNames.LIST, TypeName.FLOAT.box()),
                          "sips")),
                  "mResourceResolver.sipsToPixels(sips.get(i))")
              .build());
    }
    return dataHolder.build();
  }

  private static MethodSpec builderBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      ClassName builderClass,
      boolean hasGeneric) {
    return getMethodSpecBuilder(
            specModel,
            prop,
            requiredIndex,
            prop.getName(),
            Arrays.asList(
                parameter(
                    prop,
                    hasGeneric
                        ? ParameterizedTypeName.get(
                            builderClass, getBuilderGenericTypes(prop, builderClass))
                        : builderClass,
                    prop.getName() + "Builder")),
            "$L == null ? null : $L.build()",
            prop.getName() + "Builder",
            prop.getName() + "Builder")
        .build();
  }

  private static TypeName[] getBuilderGenericTypes(PropModel prop, ClassName builderClass) {
    return getBuilderGenericTypes(prop.getTypeName(), builderClass);
  }

  private static TypeName[] getBuilderGenericTypes(TypeName type, ClassName builderClass) {
    if (builderClass.equals(ClassNames.COMPONENT_BUILDER)
        || builderClass.equals(ClassNames.SECTION_BUILDER)) {
      return new TypeName[] {WildcardTypeName.subtypeOf(TypeName.OBJECT)};
    } else {
      final TypeName typeParameter =
          type instanceof ParameterizedTypeName
                  && !((ParameterizedTypeName) type).typeArguments.isEmpty()
              ? ((ParameterizedTypeName) type).typeArguments.get(0)
              : WildcardTypeName.subtypeOf(ClassNames.COMPONENT);

      return new TypeName[] {typeParameter};
    }
  }

  private static ParameterSpec parameterWithoutNullableAnnotation(
      PropModel prop, TypeName type, String name, AnnotationSpec... extraAnnotations) {
    List<AnnotationSpec> externalAnnotations = new ArrayList<>();
    for (AnnotationSpec annotationSpec : prop.getExternalAnnotations()) {
      if (!annotationSpec.type.toString().contains("Nullable")) {
        externalAnnotations.add(annotationSpec);
      }
    }

    return parameter(type, name, externalAnnotations, extraAnnotations);
  }

  private static MethodSpec.Builder resTypeListBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      String name,
      List<ParameterSpec> parameters,
      String statement,
      Object... formatObjects) {
    final String parameterName = parameters.get(0).name;
    final ParameterizedTypeName varArgType = (ParameterizedTypeName) prop.getTypeName();
    final TypeName resType = varArgType.typeArguments.get(0);

    CodeBlock.Builder codeBlockBuilder =
        CodeBlock.builder()
            .beginControlFlow("if ($L == null)", parameterName)
            .addStatement("return this")
            .endControlFlow();

    createListIfDefault(codeBlockBuilder, specModel, prop, resType);

    CodeBlock codeBlock =
        codeBlockBuilder
            .beginControlFlow("for (int i = 0; i < $L.size(); i++)", parameterName)
            .add("final $T res = ", resType.isBoxedPrimitive() ? resType.unbox() : resType)
            .addStatement(statement, formatObjects)
            .addStatement(
                "this.$L.$L.add(res)", getComponentMemberInstanceName(specModel), prop.getName())
            .endControlFlow()
            .build();

    return getMethodSpecBuilder(specModel, prop, requiredIndex, name, parameters, codeBlock);
  }

  private static MethodSpec.Builder resTypeRegularBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      String name,
      List<ParameterSpec> parameters,
      String statement,
      Object... formatObjects) {

    if (prop.hasVarArgs()) {
      final ParameterizedTypeName varArgType = (ParameterizedTypeName) prop.getTypeName();
      final TypeName singleParameterType = varArgType.typeArguments.get(0);

      CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();

      createListIfDefault(codeBlockBuilder, specModel, prop, singleParameterType);

      codeBlockBuilder
          .add(
              "final $T res = ",
              singleParameterType.isBoxedPrimitive()
                  ? singleParameterType.unbox()
                  : singleParameterType)
          .addStatement(statement, formatObjects)
          .addStatement(
              "this.$L.$L.add(res)", getComponentMemberInstanceName(specModel), prop.getName());

      return getMethodSpecBuilder(
          specModel, prop, requiredIndex, name, parameters, codeBlockBuilder.build());
    }

    return getNoVarArgsMethodSpecBuilder(
        specModel, prop, requiredIndex, name, parameters, statement, formatObjects);
  }

  private static MethodSpec.Builder getMethodSpecBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      String name,
      List<ParameterSpec> parameters,
      String statement,
      Object... formatObjects) {

    if (prop.hasVarArgs()) {
      final String propName = prop.getName();
      final String componentMemberInstanceName = getComponentMemberInstanceName(specModel);

      CodeBlock.Builder codeBlockBuilder =
          CodeBlock.builder()
              .beginControlFlow("if ($L == null)", propName)
              .addStatement("return this")
              .endControlFlow();

      if (prop.hasDefault(specModel.getPropDefaults())) {
        codeBlockBuilder.beginControlFlow(
            "if (this.$L.$L == null || this.$L.$L.isEmpty() || this.$L.$L == $L.$L)",
            componentMemberInstanceName,
            propName,
            componentMemberInstanceName,
            propName,
            componentMemberInstanceName,
            propName,
            specModel.getSpecName(),
            propName);
      } else {
        codeBlockBuilder.beginControlFlow(
            "if (this.$L.$L == null || this.$L.$L.isEmpty())",
            componentMemberInstanceName,
            propName,
            componentMemberInstanceName,
            propName);
      }

      codeBlockBuilder
          .addStatement("this.$L.$L = $L", componentMemberInstanceName, propName, propName)
          .nextControlFlow("else")
          .addStatement("this.$L.$L.addAll($L)", componentMemberInstanceName, propName, propName)
          .endControlFlow();

      return getMethodSpecBuilder(
          specModel, prop, requiredIndex, name, parameters, codeBlockBuilder.build());
    }

    return getNoVarArgsMethodSpecBuilder(
        specModel, prop, requiredIndex, name, parameters, statement, formatObjects);
  }

  private static MethodSpec.Builder getNoVarArgsMethodSpecBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      String name,
      List<ParameterSpec> parameters,
      String statement,
      Object... formatObjects) {

    CodeBlock codeBlock =
        CodeBlock.builder()
            .add("this.$L.$L = ", getComponentMemberInstanceName(specModel), prop.getName())
            .addStatement(statement, formatObjects)
            .build();

    return getMethodSpecBuilder(specModel, prop, requiredIndex, name, parameters, codeBlock);
  }

  private static MethodSpec.Builder getMethodSpecBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      String name,
      List<ParameterSpec> parameters,
      CodeBlock codeBlock) {
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .returns(getBuilderType(specModel));

    if (prop.isCommonProp()) {
      builder.addAnnotation(Override.class);

      if (!prop.overrideCommonPropBehavior()) {
        final CodeBlock.Builder superCodeBlock = CodeBlock.builder().add("super.$L(", name);
        boolean isFirstParam = true;
        for (ParameterSpec param : parameters) {
          if (!isFirstParam) {
            superCodeBlock.add(", ");
          }
          superCodeBlock.add("$L", param.name);
          isFirstParam = false;
        }

        builder.addCode(superCodeBlock.add(");\n").build());
      }
    }

    for (ParameterSpec param : parameters) {
      builder.addParameter(param);
    }

    builder.addCode(codeBlock);
    if (!prop.isOptional()) {
      builder.addAnnotation(
          AnnotationSpec.builder(RequiredProp.class)
              .addMember("value", "$S", prop.getName())
              .build());
      builder.addStatement("$L.set($L)", "mRequired", requiredIndex);
    }

    builder.addStatement("return this");

    return builder;
  }

  private static MethodSpec generateEventDeclarationBuilderMethod(
      SpecModel specModel, EventDeclarationModel eventDeclaration) {
    final String eventHandlerName =
        ComponentBodyGenerator.getEventHandlerInstanceName(eventDeclaration.name);
    return MethodSpec.methodBuilder(eventHandlerName)
        .addModifiers(Modifier.PUBLIC)
        .returns(getBuilderType(specModel))
        .addParameter(ClassNames.EVENT_HANDLER, eventHandlerName)
        .addStatement(
            "this.$L.$L = $L",
            getComponentMemberInstanceName(specModel),
            eventHandlerName,
            eventHandlerName)
        .addStatement("return this")
        .build();
  }

  private static MethodSpec generateEventTriggerBuilderMethod(
      SpecModel specModel, SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethodModel) {
    final String eventTriggerName =
        ComponentBodyGenerator.getEventTriggerInstanceName(triggerMethodModel.name);
    final String implMemberName = getComponentMemberInstanceName(specModel);

    return MethodSpec.methodBuilder(eventTriggerName)
        .addModifiers(Modifier.PUBLIC)
        .returns(getBuilderType(specModel))
        .addParameter(ClassNames.EVENT_TRIGGER, eventTriggerName)
        .addStatement("this.$L.$L = $L", implMemberName, eventTriggerName, eventTriggerName)
        .addStatement("return this")
        .build();
  }

  private static MethodSpec generateEventTriggerChangeKeyMethod(
      SpecModel specModel, SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethodModel) {
    final String eventTriggerName =
        ComponentBodyGenerator.getEventTriggerInstanceName(triggerMethodModel.name);
    final String implMemberName = getComponentMemberInstanceName(specModel);

    return MethodSpec.methodBuilder(getEventTriggerKeyResetMethodName(triggerMethodModel.name))
        .addModifiers(Modifier.PRIVATE)
        .addParameter(ClassNames.STRING, "key")
        .addParameter(ClassNames.HANDLE, "handle")
        .addStatement(
            "$L $L = this.$L.$L",
            ClassNames.EVENT_TRIGGER,
            eventTriggerName,
            implMemberName,
            eventTriggerName)
        .beginControlFlow("if ($L == null)", eventTriggerName)
        .addStatement(
            "$L = $L.$L(this.mContext, key, handle)",
            eventTriggerName,
            specModel.getComponentName(),
            eventTriggerName)
        .endControlFlow()
        .addStatement("$L($L)", eventTriggerName, eventTriggerName)
        .build();
  }

  private static MethodSpec generateGetThisMethod(SpecModel specModel) {
    return MethodSpec.methodBuilder("getThis")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addStatement("return this")
        .returns(getBuilderType(specModel))
        .build();
  }

  private static MethodSpec generateRegisterEventTriggersMethod(
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggerMethods) {
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("registerEventTriggers")
            .addModifiers(Modifier.PRIVATE)
            .addParameter(ClassNames.STRING, "key")
            .addParameter(ClassNames.HANDLE, "handle");

    for (SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethod : triggerMethods) {
      builder.addStatement(
          "$L(key, handle)", getEventTriggerKeyResetMethodName(triggerMethod.name));
    }

    return builder.build();
  }

  private static String getEventTriggerKeyResetMethodName(CharSequence eventTriggerClassName) {
    return ComponentBodyGenerator.getEventTriggerInstanceName(eventTriggerClassName);
  }

  private static MethodSpec generateExtraBuilderMethod(
      SpecModel specModel, BuilderMethodModel builderMethodModel) {
    return MethodSpec.methodBuilder(builderMethodModel.paramName)
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(builderMethodModel.paramType, builderMethodModel.paramName)
        .addStatement(
            "return super.$L($L)", builderMethodModel.paramName, builderMethodModel.paramName)
        .returns(getBuilderType(specModel))
        .build();
  }

  private static MethodSpec generateBuildMethod(SpecModel specModel, int numRequiredProps) {
    final MethodSpec.Builder buildMethodBuilder =
        MethodSpec.methodBuilder("build")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(specModel.getComponentTypeName());

    if (numRequiredProps > 0) {
      buildMethodBuilder.addStatement(
          "checkArgs($L, $L, $L)", REQUIRED_PROPS_COUNT, "mRequired", REQUIRED_PROPS_NAMES);
    }

    final List<PropModel> dynamicProps = SpecModelUtils.getDynamicProps(specModel);
    if (!dynamicProps.isEmpty()) {
      final int count = dynamicProps.size();

      final String componentRef = getComponentMemberInstanceName(specModel);
      buildMethodBuilder.addStatement(
          "$L.$L = new $T[$L]", componentRef, DYNAMIC_PROPS, ClassNames.DYNAMIC_VALUE, count);

      for (int i = 0; i < count; i++) {
        buildMethodBuilder.addStatement(
            "$L.$L[$L] = $L.$L",
            componentRef,
            DYNAMIC_PROPS,
            i,
            componentRef,
            dynamicProps.get(i).getName());
      }
    }

    if (!specModel.getTriggerMethods().isEmpty()) {
      String building = getComponentMemberInstanceName(specModel);
      buildMethodBuilder.addStatement(
          "registerEventTriggers($L.getKey(), $L.getHandle())", building, building);
    }

    return buildMethodBuilder
        .addStatement("return $L", getComponentMemberInstanceName(specModel))
        .build();
  }

  private static String generatePropDefaultResInitializer(
      String resourceResolveMethodName, PropDefaultModel propDefaultModel, SpecModel specModel) {
    StringBuilder builtInitializer = new StringBuilder();

    if (propDefaultModel.isResResolvable()) {
      return String.format(
          builtInitializer
              .append(RESOURCE_RESOLVER)
              .append('.')
              .append(resourceResolveMethodName)
              .append('(')
              .append(propDefaultModel.getResId())
              .append(')')
              .toString(),
          specModel.getSpecName(),
          propDefaultModel.getName());
    }

    return builtInitializer.toString();
  }
}
