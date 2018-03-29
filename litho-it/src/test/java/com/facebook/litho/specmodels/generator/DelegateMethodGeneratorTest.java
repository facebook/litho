/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.DelegateMethodGenerator.generateDelegates;
import static com.facebook.litho.specmodels.model.DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DelegateMethodDescription;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MethodParamModelFactory;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.facebook.litho.specmodels.model.TypeSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

/** Tests {@link DelegateMethodGenerator} */
public class DelegateMethodGeneratorTest {
  private static final String TEST_QUALIFIED_SPEC_NAME = "com.facebook.litho.TestSpec";

  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private SpecModel mSpecModelWithoutDI;
  private SpecModel mSpecModelWithDI;

  private SpecMethodModel<DelegateMethod, Void> mDelegateMethodModel;

  @Before
  public void setUp() {
    when(mDependencyInjectionHelper.hasSpecInjection()).thenReturn(true);
    mDelegateMethodModel =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(ImmutableList.of(createAnnotation(OnCreateLayout.class)))
            .modifiers(ImmutableList.of(Modifier.PROTECTED))
            .name("onCreateLayout")
            .returnTypeSpec(new TypeSpec(DelegateMethodDescriptions.ON_CREATE_LAYOUT.returnType))
            .typeVariables(ImmutableList.of())
            .methodParams(
                ImmutableList.of(
                    MethodParamModelFactory.create(
                        new TypeSpec(ClassNames.COMPONENT_CONTEXT),
                        "c",
                        ImmutableList.of(),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null),
                    MethodParamModelFactory.create(
                        new TypeSpec(TypeName.BOOLEAN),
                        "prop",
                        ImmutableList.of(createAnnotation(Prop.class)),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null),
                    MethodParamModelFactory.create(
                        new TypeSpec(TypeName.INT),
                        "state",
                        ImmutableList.of(createAnnotation(State.class)),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null)))
            .representedObject(null)
            .typeModel(null)
            .build();

    mSpecModelWithoutDI = SpecModelImpl.newBuilder()
        .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
        .componentClass(ClassNames.COMPONENT)
        .delegateMethods(ImmutableList.of(mDelegateMethodModel))
        .representedObject(new Object())
        .build();

    mSpecModelWithDI =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .componentClass(ClassNames.COMPONENT)
            .delegateMethods(ImmutableList.of(mDelegateMethodModel))
            .dependencyInjectionHelper(mDependencyInjectionHelper)
            .representedObject(new Object())
            .build();
  }

  @Test
  public void testGenerateWithoutDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder =
        generateDelegates(
            mSpecModelWithoutDI,
            LAYOUT_SPEC_DELEGATE_METHODS_MAP);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.Component onCreateLayout(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _result = (com.facebook.litho.Component) TestSpec.onCreateLayout(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (boolean) prop,\n"
                + "    (int) state);\n"
                + "  return _result;\n"
                + "}\n");
  }

  @Test
  public void testGenerateWithDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder =
        generateDelegates(
            mSpecModelWithDI,
            LAYOUT_SPEC_DELEGATE_METHODS_MAP);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.Component onCreateLayout(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _result = (com.facebook.litho.Component) mSpec.onCreateLayout(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (boolean) prop,\n"
                + "    (int) state);\n"
                + "  return _result;\n"
                + "}\n");
  }

  @Test
  public void testExtraOptionalParameterHasNoEffectIfSpecMethodDoesntUseIt() throws Exception {
    Map<Class<? extends Annotation>, DelegateMethodDescription> map =
        new TreeMap<>(
            new Comparator<Class<? extends Annotation>>() {
              @Override
              public int compare(Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
                return lhs.toString().compareTo(rhs.toString());
              }
            });
    map.put(
        OnCreateLayout.class,
        DelegateMethodDescription.fromDelegateMethodDescription(
                LAYOUT_SPEC_DELEGATE_METHODS_MAP.get(OnCreateLayout.class))
            .optionalParameters(
                ImmutableList.of(
                    MethodParamModelFactory.createSimpleMethodParamModel(
                        new TypeSpec(TypeName.CHAR), "optionalParam", new Object())))
            .build());

    TypeSpecDataHolder typeSpecDataHolder = generateDelegates(mSpecModelWithDI, map);

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.Component onCreateLayout(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _result = (com.facebook.litho.Component) mSpec.onCreateLayout(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (boolean) prop,\n"
                + "    (int) state);\n"
                + "  return _result;\n"
                + "}\n");
  }

  @Test
  public void testExtraOptionalParameterIncludedIfSpecMethodUsesIt() throws Exception {
    Map<Class<? extends Annotation>, DelegateMethodDescription> map =
        new TreeMap<>(
            new Comparator<Class<? extends Annotation>>() {
              @Override
              public int compare(Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
                return lhs.toString().compareTo(rhs.toString());
              }
            });
    map.put(
        OnCreateLayout.class,
        DelegateMethodDescription.fromDelegateMethodDescription(
                LAYOUT_SPEC_DELEGATE_METHODS_MAP.get(OnCreateLayout.class))
            .optionalParameters(
                ImmutableList.of(
                    MethodParamModelFactory.createSimpleMethodParamModel(
                        new TypeSpec(TypeName.CHAR), "optionalParam", new Object())))
            .build());

    SpecMethodModel<DelegateMethod, Void> delegateMethodExpectingOptionalParameter =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(ImmutableList.of(createAnnotation(OnCreateLayout.class)))
            .modifiers(ImmutableList.of(Modifier.PROTECTED))
            .name("onCreateLayout")
            .returnTypeSpec(new TypeSpec(DelegateMethodDescriptions.ON_CREATE_LAYOUT.returnType))
            .typeVariables(ImmutableList.of())
            .methodParams(
                ImmutableList.of(
                    MethodParamModelFactory.create(
                        new TypeSpec(ClassNames.COMPONENT_CONTEXT),
                        "c",
                        ImmutableList.of(),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null),
                    MethodParamModelFactory.createSimpleMethodParamModel(
                        new TypeSpec(TypeName.CHAR), "unimportantName", new Object()),
                    MethodParamModelFactory.create(
                        new TypeSpec(TypeName.BOOLEAN),
                        "prop",
                        ImmutableList.of(createAnnotation(Prop.class)),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null)))
            .representedObject(null)
            .typeModel(null)
            .build();

    SpecModel specModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .componentClass(ClassNames.COMPONENT)
            .delegateMethods(ImmutableList.of(delegateMethodExpectingOptionalParameter))
            .representedObject(new Object())
            .build();

    TypeSpecDataHolder typeSpecDataHolder = generateDelegates(specModel, map);

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.Component onCreateLayout(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _result = (com.facebook.litho.Component) TestSpec.onCreateLayout(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    optionalParam,\n"
                + "    (boolean) prop);\n"
                + "  return _result;\n"
                + "}\n");
  }

  private static Annotation createAnnotation(final Class<? extends Annotation> annotationClass) {
    return new Annotation() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return annotationClass;
      }
    };
  }
}
