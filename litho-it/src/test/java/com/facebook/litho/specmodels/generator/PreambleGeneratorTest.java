/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.PreambleGenerator.generateConstructor;
import static com.facebook.litho.specmodels.generator.PreambleGenerator.generateGetter;
import static com.facebook.litho.specmodels.generator.PreambleGenerator.generateSourceDelegate;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link PreambleGenerator}
 */
public class PreambleGeneratorTest {
  private static final String TEST_QUALIFIED_SPEC_NAME = "com.facebook.litho.TestSpec";

  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private SpecModel mSpecModelWithoutDI;
  private SpecModel mSpecModelWithDI;

  @Before
  public void setUp() {
    mSpecModelWithoutDI = SpecModelImpl.newBuilder()
        .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
        .delegateMethods(ImmutableList.<DelegateMethodModel>of())
        .representedObject(new Object())
        .build();

    mSpecModelWithDI = SpecModelImpl.newBuilder()
        .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
        .delegateMethods(ImmutableList.<DelegateMethodModel>of())
        .dependencyInjectionGenerator(mDependencyInjectionHelper)
        .representedObject(new Object())
        .build();
  }

  @Test
  public void testGenerateConstructorWithoutDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder =
        generateConstructor(mSpecModelWithoutDI);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo("private Constructor() {\n" + "}\n");
  }

  @Test
  public void testGenerateConstructorWithDependencyInjection() {
    final MethodSpec constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addStatement("final Object testObject = new TestObject()")
            .build();

    when(mDependencyInjectionHelper.generateConstructor(mSpecModelWithDI))
        .thenReturn(constructor);

    TypeSpecDataHolder typeSpecDataHolder = generateConstructor(mSpecModelWithDI);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "public Constructor() {\n" + "  final Object testObject = new TestObject();\n" + "}\n");
  }

  @Test
  public void testGenerateSourceDelegateWithoutDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder =
        generateSourceDelegate(mSpecModelWithoutDI);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();
  }

  @Test
  public void testGenerateSourceDelegateWithDependencyInjection() {
    final FieldSpec field =
        FieldSpec.builder(mSpecModelWithDI.getSpecTypeName(), "mSpec")
            .addModifiers(Modifier.PRIVATE)
            .build();
    final TypeSpecDataHolder sourceDelegate =
        TypeSpecDataHolder.newBuilder().addField(field).build();
    when(mDependencyInjectionHelper.generateSourceDelegate(mSpecModelWithDI))
        .thenReturn(sourceDelegate);

    TypeSpecDataHolder typeSpecDataHolder =
        generateSourceDelegate(mSpecModelWithDI);

    assertThat(typeSpecDataHolder.getFieldSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getMethodSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getFieldSpecs().get(0).toString())
        .isEqualTo("private com.facebook.litho.TestSpec mSpec;\n");
  }

  @Test
  public void testGenerateGetterWithoutDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder = generateGetter(mSpecModelWithoutDI);

    assertThat(typeSpecDataHolder.getFieldSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getFieldSpecs().get(0).toString()).isEqualTo(
        "private static com.facebook.litho.Test sInstance = null;\n");
    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString()).isEqualTo(
        "public static synchronized com.facebook.litho.Test get() {\n" +
            "  if (sInstance == null) {\n" +
            "    sInstance = new com.facebook.litho.Test();\n" +
            "  }\n" +
            "  return sInstance;\n" +
            "}\n");
  }

  @Test
  public void testGenerateGetterWithDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder = generateGetter(mSpecModelWithDI);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString()).isEqualTo(
        "public com.facebook.litho.Test get() {\n" +
            "  return this;\n" +
            "}\n");
  }
}
