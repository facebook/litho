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

package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Assertions.assertThat;

import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.generator.CachedValueGenerator;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests {@link CachedValueGenerator} */
@RunWith(JUnit4.class)
public class CachedValueGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  @Mock private Messager mMessager;

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  private SpecModel mLayoutSpecModel;

  @LayoutSpec
  static class CachedValueTestSpec {

    @OnCalculateCachedValue(name = "expensiveValue")
    static String onCreateExpensiveValue() {
      return "ABC";
    }

    @OnCalculateCachedValue(name = "expensiveValueWithContext")
    static String onCreateExpensiveValueWithContext(ComponentContext context) {
      return "ABC";
    }

    @OnCalculateCachedValue(name = "moreExpensiveValue")
    static String onCreateMoreExpensiveValue(@Prop boolean arg0, @State int arg1) {
      return "DEF";
    }

    @OnCalculateCachedValue(name = "moreExpensiveValueWithContext")
    static String onCreateMoreExpensiveValueWithContext(
        @Prop boolean arg0, @State int arg1, ComponentContext context) {
      return "DEF";
    }

    @OnCalculateCachedValue(name = "expensiveValueWithGeneric")
    static <E extends CharSequence> String onCreateExpensiveValueWithGeneric(@Prop E genericArg) {
      return "GHI";
    }

    @OnCalculateCachedValue(name = "expensiveValueWithMoreGenerics")
    static <E extends CharSequence> String onCreateExpensiveValueWithMoreGenerics(
        @Prop E genericArg, @Prop E genericArg2) {
      return "JKL";
    }

    @OnCalculateCachedValue(name = "expensiveValueWithMoreGenericsAndContext")
    static <E extends CharSequence> String onCreateExpensiveValueWithMoreGenericsAndContext(
        ComponentContext context, @Prop E genericArg, @Prop E genericArg2) {
      return "JKL";
    }

    @OnCalculateCachedValue(name = "expensiveValueWithTreeProp")
    static String onCreateExpensiveValueWithTreeProp(@Prop boolean arg0, @TreeProp long arg6) {
      return "MNO";
    }

    @OnCreateLayout
    public <E extends CharSequence> void testDelegateMethod(
        @Prop boolean arg0,
        @Prop @Nullable Component arg1,
        @Prop List<Component> arg2,
        @Prop List<String> arg3,
        @Prop E genericArg,
        @State int arg4,
        @Param Object arg5,
        @TreeProp long arg6,
        @TreeProp Set<List<Row>> arg7,
        @TreeProp Set<Integer> arg8) {}

    @OnEvent(Object.class)
    public void testEventMethod(
        @Prop boolean arg0,
        @Prop @Nullable Component arg1,
        @State int arg2,
        @Param Object arg3,
        @TreeProp long arg4) {}

    @OnUpdateState
    public void testUpdateStateMethod() {}
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement =
        elements.getTypeElement(
            CachedValueGeneratorTest.CachedValueTestSpec.class.getCanonicalName());
    mLayoutSpecModel =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mMessager, RunMode.normal(), null, null);
  }

  @Test
  public void testGenerateInputsClassNoParam() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValue"))
            .findFirst()
            .get();
    final String expensiveValueInputsClass =
        CachedValueGenerator.createInputsClass(
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValue",
                RunMode.normal())
            .toString();
    assertThat(expensiveValueInputsClass)
        .isEqualTo(
            "@com.facebook.litho.annotations.Generated\n"
                + "private static class ExpensiveValueInputs {\n"
                + "  private final String globalKey;\n"
                + "\n"
                + "  ExpensiveValueInputs(String globalKey) {\n"
                + "    this.globalKey = globalKey;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(globalKey, getClass());\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof ExpensiveValueInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueInputs cachedValueInputs = (ExpensiveValueInputs) other;\n"
                + "    if (!com.facebook.rendercore.utils.EquivalenceUtils.equals(globalKey,"
                + " cachedValueInputs.globalKey)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateGetterNoParam() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValue"))
            .findFirst()
            .get();
    final String expensiveValueMethod =
        CachedValueGenerator.createGetterMethod(
                mLayoutSpecModel,
                specMethodModel,
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValue",
                0)
            .toString();
    assertThat(expensiveValueMethod)
        .isEqualTo(
            "private java.lang.String getExpensiveValue(com.facebook.litho.ComponentContext c) {\n"
                + "  String globalKey = c.getGlobalKey();\n"
                + "  final ExpensiveValueInputs inputs = new ExpensiveValueInputs(globalKey);\n"
                + "  java.lang.String expensiveValue = (java.lang.String)"
                + " c.getCachedValue(globalKey, 0, inputs);\n"
                + "  if (expensiveValue == null) {\n"
                + "    expensiveValue = CachedValueTestSpec.onCreateExpensiveValue();\n"
                + "    c.putCachedValue(globalKey, 0, inputs, expensiveValue);\n"
                + "  }\n"
                + "  return expensiveValue;\n"
                + "}\n");
  }

  @Test
  public void testGenerateInputsClassWithContextParam() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValueWithContext"))
            .findFirst()
            .get();
    final String expensiveValueInputsClass =
        CachedValueGenerator.createInputsClass(
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithContext",
                RunMode.normal())
            .toString();
    assertThat(expensiveValueInputsClass)
        .isEqualTo(
            "@com.facebook.litho.annotations.Generated\n"
                + "private static class ExpensiveValueWithContextInputs {\n"
                + "  private final String globalKey;\n"
                + "\n"
                + "  ExpensiveValueWithContextInputs(String globalKey) {\n"
                + "    this.globalKey = globalKey;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(globalKey, getClass());\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof ExpensiveValueWithContextInputs))"
                + " {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueWithContextInputs cachedValueInputs ="
                + " (ExpensiveValueWithContextInputs) other;\n"
                + "    if (!com.facebook.rendercore.utils.EquivalenceUtils.equals(globalKey,"
                + " cachedValueInputs.globalKey)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateGetterWithContextParam() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValueWithContext"))
            .findFirst()
            .get();

    final String expensiveValue =
        CachedValueGenerator.createGetterMethod(
                mLayoutSpecModel,
                specMethodModel,
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithContext",
                1)
            .toString();
    assertThat(expensiveValue)
        .isEqualTo(
            "private java.lang.String"
                + " getExpensiveValueWithContext(com.facebook.litho.ComponentContext c) {\n"
                + "  String globalKey = c.getGlobalKey();\n"
                + "  final ExpensiveValueWithContextInputs inputs = new"
                + " ExpensiveValueWithContextInputs(globalKey);\n"
                + "  java.lang.String expensiveValueWithContext = (java.lang.String)"
                + " c.getCachedValue(globalKey, 1, inputs);\n"
                + "  if (expensiveValueWithContext == null) {\n"
                + "    expensiveValueWithContext ="
                + " CachedValueTestSpec.onCreateExpensiveValueWithContext(c);\n"
                + "    c.putCachedValue(globalKey, 1, inputs, expensiveValueWithContext);\n"
                + "  }\n"
                + "  return expensiveValueWithContext;\n"
                + "}\n");
  }

  @Test
  public void testGenerateInputsClassGenericParam() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValueWithGeneric"))
            .findFirst()
            .get();
    final String inputsClassWithGenericParam =
        CachedValueGenerator.createInputsClass(
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithGeneric",
                RunMode.normal())
            .toString();
    assertThat(inputsClassWithGenericParam)
        .isEqualTo(
            "@com.facebook.litho.annotations.Generated\n"
                + "private static class ExpensiveValueWithGenericInputs<E extends"
                + " java.lang.CharSequence> {\n"
                + "  private final String globalKey;\n"
                + "\n"
                + "  private final E genericArg;\n"
                + "\n"
                + "  ExpensiveValueWithGenericInputs(String globalKey, E genericArg) {\n"
                + "    this.globalKey = globalKey;\n"
                + "    this.genericArg = genericArg;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(globalKey, genericArg,"
                + " getClass());\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof ExpensiveValueWithGenericInputs))"
                + " {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueWithGenericInputs cachedValueInputs ="
                + " (ExpensiveValueWithGenericInputs) other;\n"
                + "    if (!com.facebook.rendercore.utils.EquivalenceUtils.equals(globalKey,"
                + " cachedValueInputs.globalKey)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (genericArg != null ? !genericArg.equals(cachedValueInputs.genericArg) :"
                + " cachedValueInputs.genericArg != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateGetterGenericParam() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValueWithGeneric"))
            .findFirst()
            .get();
    final String valueWithGeneric =
        CachedValueGenerator.createGetterMethod(
                mLayoutSpecModel,
                specMethodModel,
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithGeneric",
                4)
            .toString();
    assertThat(valueWithGeneric)
        .isEqualTo(
            "private java.lang.String"
                + " getExpensiveValueWithGeneric(com.facebook.litho.ComponentContext c) {\n"
                + "  String globalKey = c.getGlobalKey();\n"
                + "  final ExpensiveValueWithGenericInputs inputs = new"
                + " ExpensiveValueWithGenericInputs(globalKey,genericArg);\n"
                + "  java.lang.String expensiveValueWithGeneric = (java.lang.String)"
                + " c.getCachedValue(globalKey, 4, inputs);\n"
                + "  if (expensiveValueWithGeneric == null) {\n"
                + "    expensiveValueWithGeneric ="
                + " CachedValueTestSpec.onCreateExpensiveValueWithGeneric(genericArg);\n"
                + "    c.putCachedValue(globalKey, 4, inputs, expensiveValueWithGeneric);\n"
                + "  }\n"
                + "  return expensiveValueWithGeneric;\n"
                + "}\n");
  }

  @Test
  public void testGenerateInputsClassMultipleGenericsParams() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValueWithMoreGenerics"))
            .findFirst()
            .get();
    final String inputsClassWithGenericParam =
        CachedValueGenerator.createInputsClass(
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithMoreGenerics",
                RunMode.normal())
            .toString();
    assertThat(inputsClassWithGenericParam)
        .isEqualTo(
            "@com.facebook.litho.annotations.Generated\n"
                + "private static class ExpensiveValueWithMoreGenericsInputs<E extends"
                + " java.lang.CharSequence> {\n"
                + "  private final String globalKey;\n"
                + "\n"
                + "  private final E genericArg;\n"
                + "\n"
                + "  private final E genericArg2;\n"
                + "\n"
                + "  ExpensiveValueWithMoreGenericsInputs(String globalKey, E genericArg, E"
                + " genericArg2) {\n"
                + "    this.globalKey = globalKey;\n"
                + "    this.genericArg = genericArg;\n"
                + "    this.genericArg2 = genericArg2;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(globalKey, genericArg,"
                + " genericArg2, getClass());\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof"
                + " ExpensiveValueWithMoreGenericsInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueWithMoreGenericsInputs cachedValueInputs ="
                + " (ExpensiveValueWithMoreGenericsInputs) other;\n"
                + "    if (!com.facebook.rendercore.utils.EquivalenceUtils.equals(globalKey,"
                + " cachedValueInputs.globalKey)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (genericArg != null ? !genericArg.equals(cachedValueInputs.genericArg) :"
                + " cachedValueInputs.genericArg != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (genericArg2 != null ? !genericArg2.equals(cachedValueInputs.genericArg2)"
                + " : cachedValueInputs.genericArg2 != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateGetterMultipleGenericsParams() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValueWithMoreGenerics"))
            .findFirst()
            .get();
    final String valueWithMoreGenerics =
        CachedValueGenerator.createGetterMethod(
                mLayoutSpecModel,
                specMethodModel,
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithMoreGenerics",
                5)
            .toString();
    assertThat(valueWithMoreGenerics)
        .isEqualTo(
            "private java.lang.String"
                + " getExpensiveValueWithMoreGenerics(com.facebook.litho.ComponentContext c) {\n"
                + "  String globalKey = c.getGlobalKey();\n"
                + "  final ExpensiveValueWithMoreGenericsInputs inputs = new"
                + " ExpensiveValueWithMoreGenericsInputs(globalKey,genericArg,genericArg2);\n"
                + "  java.lang.String expensiveValueWithMoreGenerics = (java.lang.String)"
                + " c.getCachedValue(globalKey, 5, inputs);\n"
                + "  if (expensiveValueWithMoreGenerics == null) {\n"
                + "    expensiveValueWithMoreGenerics ="
                + " CachedValueTestSpec.onCreateExpensiveValueWithMoreGenerics(genericArg,genericArg2);\n"
                + "    c.putCachedValue(globalKey, 5, inputs, expensiveValueWithMoreGenerics);\n"
                + "  }\n"
                + "  return expensiveValueWithMoreGenerics;\n"
                + "}\n");
  }

  @Test
  public void testGenerateInputsClassMultipleGenericsParamsAndContext() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(
                m -> m.name.toString().equals("onCreateExpensiveValueWithMoreGenericsAndContext"))
            .findFirst()
            .get();
    final String inputsClassWithGenericParam =
        CachedValueGenerator.createInputsClass(
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithMoreGenericsAndContext",
                RunMode.normal())
            .toString();
    assertThat(inputsClassWithGenericParam)
        .isEqualTo(
            "@com.facebook.litho.annotations.Generated\n"
                + "private static class ExpensiveValueWithMoreGenericsAndContextInputs<E extends"
                + " java.lang.CharSequence> {\n"
                + "  private final String globalKey;\n"
                + "\n"
                + "  private final E genericArg;\n"
                + "\n"
                + "  private final E genericArg2;\n"
                + "\n"
                + "  ExpensiveValueWithMoreGenericsAndContextInputs(String globalKey, E genericArg,"
                + " E genericArg2) {\n"
                + "    this.globalKey = globalKey;\n"
                + "    this.genericArg = genericArg;\n"
                + "    this.genericArg2 = genericArg2;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(globalKey, genericArg,"
                + " genericArg2, getClass());\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof"
                + " ExpensiveValueWithMoreGenericsAndContextInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueWithMoreGenericsAndContextInputs cachedValueInputs ="
                + " (ExpensiveValueWithMoreGenericsAndContextInputs) other;\n"
                + "    if (!com.facebook.rendercore.utils.EquivalenceUtils.equals(globalKey,"
                + " cachedValueInputs.globalKey)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (genericArg != null ? !genericArg.equals(cachedValueInputs.genericArg) :"
                + " cachedValueInputs.genericArg != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (genericArg2 != null ? !genericArg2.equals(cachedValueInputs.genericArg2)"
                + " : cachedValueInputs.genericArg2 != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateGetterMultipleGenericsParamsAndContext() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(
                m -> m.name.toString().equals("onCreateExpensiveValueWithMoreGenericsAndContext"))
            .findFirst()
            .get();
    final String valueWithMoreGenericsAndContext =
        CachedValueGenerator.createGetterMethod(
                mLayoutSpecModel,
                specMethodModel,
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithMoreGenericsAndContext",
                6)
            .toString();
    assertThat(valueWithMoreGenericsAndContext)
        .isEqualTo(
            "private java.lang.String getExpensiveValueWithMoreGenericsAndContext(\n"
                + "    com.facebook.litho.ComponentContext c) {\n"
                + "  String globalKey = c.getGlobalKey();\n"
                + "  final ExpensiveValueWithMoreGenericsAndContextInputs inputs = new"
                + " ExpensiveValueWithMoreGenericsAndContextInputs(globalKey,genericArg,genericArg2);\n"
                + "  java.lang.String expensiveValueWithMoreGenericsAndContext = (java.lang.String)"
                + " c.getCachedValue(globalKey, 6, inputs);\n"
                + "  if (expensiveValueWithMoreGenericsAndContext == null) {\n"
                + "    expensiveValueWithMoreGenericsAndContext ="
                + " CachedValueTestSpec.onCreateExpensiveValueWithMoreGenericsAndContext(c,genericArg,genericArg2);\n"
                + "    c.putCachedValue(globalKey, 6, inputs,"
                + " expensiveValueWithMoreGenericsAndContext);\n"
                + "  }\n"
                + "  return expensiveValueWithMoreGenericsAndContext;\n"
                + "}\n");
  }

  @Test
  public void testGenerateInputsClassWithParam() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateMoreExpensiveValue"))
            .findFirst()
            .get();
    final String expensiveValueInputsClass =
        CachedValueGenerator.createInputsClass(
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "moreExpensiveValue",
                RunMode.normal())
            .toString();
    assertThat(expensiveValueInputsClass)
        .isEqualTo(
            "@com.facebook.litho.annotations.Generated\n"
                + "private static class MoreExpensiveValueInputs {\n"
                + "  private final String globalKey;\n"
                + "\n"
                + "  private final boolean arg0;\n"
                + "\n"
                + "  private final int arg1;\n"
                + "\n"
                + "  MoreExpensiveValueInputs(String globalKey, boolean arg0, int arg1) {\n"
                + "    this.globalKey = globalKey;\n"
                + "    this.arg0 = arg0;\n"
                + "    this.arg1 = arg1;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(globalKey, arg0, arg1,"
                + " getClass());\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof MoreExpensiveValueInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    MoreExpensiveValueInputs cachedValueInputs = (MoreExpensiveValueInputs)"
                + " other;\n"
                + "    if (!com.facebook.rendercore.utils.EquivalenceUtils.equals(globalKey,"
                + " cachedValueInputs.globalKey)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (arg0 != cachedValueInputs.arg0) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (arg1 != cachedValueInputs.arg1) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateGetterWithParam() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateMoreExpensiveValue"))
            .findFirst()
            .get();
    final String moreExpensiveValue =
        CachedValueGenerator.createGetterMethod(
                mLayoutSpecModel,
                specMethodModel,
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "moreExpensiveValue",
                2)
            .toString();
    assertThat(moreExpensiveValue)
        .isEqualTo(
            "private java.lang.String getMoreExpensiveValue(com.facebook.litho.ComponentContext c)"
                + " {\n"
                + "  String globalKey = c.getGlobalKey();\n"
                + "  final MoreExpensiveValueInputs inputs = new"
                + " MoreExpensiveValueInputs(globalKey,arg0,getStateContainerImpl(c).arg1);\n"
                + "  java.lang.String moreExpensiveValue = (java.lang.String)"
                + " c.getCachedValue(globalKey, 2, inputs);\n"
                + "  if (moreExpensiveValue == null) {\n"
                + "    moreExpensiveValue ="
                + " CachedValueTestSpec.onCreateMoreExpensiveValue(arg0,getStateContainerImpl(c).arg1);\n"
                + "    c.putCachedValue(globalKey, 2, inputs, moreExpensiveValue);\n"
                + "  }\n"
                + "  return moreExpensiveValue;\n"
                + "}\n");
  }

  @Test
  public void testGenerateInputsClassWithParamAndContext() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateMoreExpensiveValueWithContext"))
            .findFirst()
            .get();
    final String expensiveValueInputsClass =
        CachedValueGenerator.createInputsClass(
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "moreExpensiveValueWithContext",
                RunMode.normal())
            .toString();
    assertThat(expensiveValueInputsClass)
        .isEqualTo(
            "@com.facebook.litho.annotations.Generated\n"
                + "private static class MoreExpensiveValueWithContextInputs {\n"
                + "  private final String globalKey;\n"
                + "\n"
                + "  private final boolean arg0;\n"
                + "\n"
                + "  private final int arg1;\n"
                + "\n"
                + "  MoreExpensiveValueWithContextInputs(String globalKey, boolean arg0, int arg1)"
                + " {\n"
                + "    this.globalKey = globalKey;\n"
                + "    this.arg0 = arg0;\n"
                + "    this.arg1 = arg1;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(globalKey, arg0, arg1,"
                + " getClass());\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof"
                + " MoreExpensiveValueWithContextInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    MoreExpensiveValueWithContextInputs cachedValueInputs ="
                + " (MoreExpensiveValueWithContextInputs) other;\n"
                + "    if (!com.facebook.rendercore.utils.EquivalenceUtils.equals(globalKey,"
                + " cachedValueInputs.globalKey)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (arg0 != cachedValueInputs.arg0) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (arg1 != cachedValueInputs.arg1) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateGetterWithParamAndContext() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateMoreExpensiveValueWithContext"))
            .findFirst()
            .get();
    final String moreExpensiveValueWithContext =
        CachedValueGenerator.createGetterMethod(
                mLayoutSpecModel,
                specMethodModel,
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "moreExpensiveValueWithContext",
                3)
            .toString();
    assertThat(moreExpensiveValueWithContext)
        .isEqualTo(
            "private java.lang.String"
                + " getMoreExpensiveValueWithContext(com.facebook.litho.ComponentContext c) {\n"
                + "  String globalKey = c.getGlobalKey();\n"
                + "  final MoreExpensiveValueWithContextInputs inputs = new"
                + " MoreExpensiveValueWithContextInputs(globalKey,arg0,getStateContainerImpl(c).arg1);\n"
                + "  java.lang.String moreExpensiveValueWithContext = (java.lang.String)"
                + " c.getCachedValue(globalKey, 3, inputs);\n"
                + "  if (moreExpensiveValueWithContext == null) {\n"
                + "    moreExpensiveValueWithContext ="
                + " CachedValueTestSpec.onCreateMoreExpensiveValueWithContext(arg0,getStateContainerImpl(c).arg1,c);\n"
                + "    c.putCachedValue(globalKey, 3, inputs, moreExpensiveValueWithContext);\n"
                + "  }\n"
                + "  return moreExpensiveValueWithContext;\n"
                + "}\n");
  }

  @Test
  public void testGenerateInputsClassWithTreeProp() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValueWithTreeProp"))
            .findFirst()
            .get();
    final String expensiveValueInputsClass =
        CachedValueGenerator.createInputsClass(
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithTreeProp",
                RunMode.normal())
            .toString();
    assertThat(expensiveValueInputsClass)
        .isEqualTo(
            "@com.facebook.litho.annotations.Generated\n"
                + "private static class ExpensiveValueWithTreePropInputs {\n"
                + "  private final String globalKey;\n"
                + "\n"
                + "  private final boolean arg0;\n"
                + "\n"
                + "  private final long arg6;\n"
                + "\n"
                + "  ExpensiveValueWithTreePropInputs(String globalKey, boolean arg0, long arg6)"
                + " {\n"
                + "    this.globalKey = globalKey;\n"
                + "    this.arg0 = arg0;\n"
                + "    this.arg6 = arg6;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(globalKey, arg0, arg6,"
                + " getClass());\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof ExpensiveValueWithTreePropInputs))"
                + " {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueWithTreePropInputs cachedValueInputs ="
                + " (ExpensiveValueWithTreePropInputs) other;\n"
                + "    if (!com.facebook.rendercore.utils.EquivalenceUtils.equals(globalKey,"
                + " cachedValueInputs.globalKey)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (arg0 != cachedValueInputs.arg0) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (arg6 != cachedValueInputs.arg6) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateGetterWithTreeProp() {
    final List<SpecMethodModel<DelegateMethod, Void>> models =
        SpecModelUtils.getMethodModelsWithAnnotation(
            mLayoutSpecModel, OnCalculateCachedValue.class);
    final SpecMethodModel<DelegateMethod, Void> specMethodModel =
        models.stream()
            .filter(m -> m.name.toString().equals("onCreateExpensiveValueWithTreeProp"))
            .findFirst()
            .get();
    final String expensiveValueWithTreeProp =
        CachedValueGenerator.createGetterMethod(
                mLayoutSpecModel,
                specMethodModel,
                CachedValueGenerator.getCachedValueInputs(specMethodModel),
                "expensiveValueWithTreeProp",
                7)
            .toString();
    assertThat(expensiveValueWithTreeProp)
        .isEqualTo(
            "private java.lang.String"
                + " getExpensiveValueWithTreeProp(com.facebook.litho.ComponentContext c) {\n"
                + "  String globalKey = c.getGlobalKey();\n"
                + "  final ExpensiveValueWithTreePropInputs inputs = new"
                + " ExpensiveValueWithTreePropInputs(globalKey,arg0,(c.getParentTreeProp(long.class)));\n"
                + "  java.lang.String expensiveValueWithTreeProp = (java.lang.String)"
                + " c.getCachedValue(globalKey, 7, inputs);\n"
                + "  if (expensiveValueWithTreeProp == null) {\n"
                + "    expensiveValueWithTreeProp ="
                + " CachedValueTestSpec.onCreateExpensiveValueWithTreeProp(arg0,(c.getParentTreeProp(long.class)));\n"
                + "    c.putCachedValue(globalKey, 7, inputs, expensiveValueWithTreeProp);\n"
                + "  }\n"
                + "  return expensiveValueWithTreeProp;\n"
                + "}\n");
  }
}
