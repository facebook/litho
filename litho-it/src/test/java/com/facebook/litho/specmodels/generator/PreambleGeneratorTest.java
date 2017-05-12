/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        PreambleGenerator.generateConstructor(mSpecModelWithoutDI);

    assertThat(typeSpecDataHolder.getFieldSpecs().size()).isEqualTo(0);
    assertThat(typeSpecDataHolder.getMethodSpecs().size()).isEqualTo(1);
    assertThat(typeSpecDataHolder.getTypeSpecs().size()).isEqualTo(0);

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString()).isEqualTo(
        "private Constructor() {\n" +
            "}\n");
  }

  @Test
  public void testGenerateConstructorWithDependencyInjection() {
    MethodSpec constructor = MethodSpec.constructorBuilder().build();
    when(mDependencyInjectionHelper.generateConstructor(mSpecModelWithDI))
        .thenReturn(constructor);

    TypeSpecDataHolder typeSpecDataHolder = PreambleGenerator.generateConstructor(mSpecModelWithDI);

    assertThat(typeSpecDataHolder.getFieldSpecs().size()).isEqualTo(0);
    assertThat(typeSpecDataHolder.getMethodSpecs().size()).isEqualTo(1);
    assertThat(typeSpecDataHolder.getTypeSpecs().size()).isEqualTo(0);

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0)).isSameAs(constructor);
  }

  @Test
  public void testGenerateSourceDelegateWithoutDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder =
        PreambleGenerator.generateSourceDelegate(mSpecModelWithoutDI);

    assertThat(typeSpecDataHolder.getFieldSpecs().size()).isEqualTo(1);
    assertThat(typeSpecDataHolder.getMethodSpecs().size()).isEqualTo(0);
    assertThat(typeSpecDataHolder.getTypeSpecs().size()).isEqualTo(0);

    assertThat(typeSpecDataHolder.getFieldSpecs().get(0).toString()).isEqualTo(
        "private com.facebook.litho.TestSpec mSpec = " +
            "new com.facebook.litho.TestSpec();\n");
  }

  @Test
  public void testGenerateSourceDelegateWithDependencyInjection() {
    when(mDependencyInjectionHelper.getSourceDelegateTypeName(mSpecModelWithDI))
        .thenReturn(
            ParameterizedTypeName.get(
                ClassName.bestGuess("Lazy"),
                mSpecModelWithDI.getSpecTypeName()));

    TypeSpecDataHolder typeSpecDataHolder =
        PreambleGenerator.generateSourceDelegate(mSpecModelWithDI);

    assertThat(typeSpecDataHolder.getFieldSpecs().size()).isEqualTo(1);
    assertThat(typeSpecDataHolder.getMethodSpecs().size()).isEqualTo(0);
    assertThat(typeSpecDataHolder.getTypeSpecs().size()).isEqualTo(0);

    assertThat(typeSpecDataHolder.getFieldSpecs().get(0).toString()).isEqualTo(
        "private Lazy<com.facebook.litho.TestSpec> mSpec;\n");
  }

  @Test
  public void testGenerateGetterWithoutDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder = PreambleGenerator.generateGetter(mSpecModelWithoutDI);

    assertThat(typeSpecDataHolder.getFieldSpecs().size()).isEqualTo(1);
    assertThat(typeSpecDataHolder.getMethodSpecs().size()).isEqualTo(1);
    assertThat(typeSpecDataHolder.getTypeSpecs().size()).isEqualTo(0);

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
    TypeSpecDataHolder typeSpecDataHolder = PreambleGenerator.generateGetter(mSpecModelWithDI);

    assertThat(typeSpecDataHolder.getFieldSpecs().size()).isEqualTo(0);
    assertThat(typeSpecDataHolder.getMethodSpecs().size()).isEqualTo(1);
    assertThat(typeSpecDataHolder.getTypeSpecs().size()).isEqualTo(0);

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString()).isEqualTo(
        "public com.facebook.litho.Test get() {\n" +
            "  return this;\n" +
            "}\n");
  }
}
