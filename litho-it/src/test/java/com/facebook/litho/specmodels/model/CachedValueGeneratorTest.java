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

package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

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
        CachedValueGenerator.createInputsClass(specMethodModel, "expensiveValue", RunMode.normal())
            .toString();
    assertThat(expensiveValueInputsClass)
        .isEqualTo(
            "private static class ExpensiveValueInputs {\n"
                + "  ExpensiveValueInputs() {\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(getClass());\n"
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
                + "    return true;\n"
                + "  }\n"
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
                specMethodModel, "expensiveValueWithContext", RunMode.normal())
            .toString();
    assertThat(expensiveValueInputsClass)
        .isEqualTo(
            "private static class ExpensiveValueWithContextInputs {\n"
                + "  ExpensiveValueWithContextInputs() {\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(getClass());\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof ExpensiveValueWithContextInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueWithContextInputs cachedValueInputs = (ExpensiveValueWithContextInputs) other;\n"
                + "    return true;\n"
                + "  }\n"
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
                specMethodModel, "expensiveValueWithGeneric", RunMode.normal())
            .toString();
    assertThat(inputsClassWithGenericParam)
        .isEqualTo(
            "private static class ExpensiveValueWithGenericInputs<E extends java.lang.CharSequence> {\n"
                + "  private final E genericArg;\n"
                + "\n"
                + "  ExpensiveValueWithGenericInputs(E genericArg) {\n"
                + "    this.genericArg = genericArg;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(genericArg);\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof ExpensiveValueWithGenericInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueWithGenericInputs cachedValueInputs = (ExpensiveValueWithGenericInputs) other;\n"
                + "    if (genericArg != null ? !genericArg.equals(cachedValueInputs.genericArg) : cachedValueInputs.genericArg != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
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
                specMethodModel, "expensiveValueWithMoreGenerics", RunMode.normal())
            .toString();
    assertThat(inputsClassWithGenericParam)
        .isEqualTo(
            "private static class ExpensiveValueWithMoreGenericsInputs<E extends java.lang.CharSequence> {\n"
                + "  private final E genericArg;\n"
                + "\n"
                + "  private final E genericArg2;\n"
                + "\n"
                + "  ExpensiveValueWithMoreGenericsInputs(E genericArg, E genericArg2) {\n"
                + "    this.genericArg = genericArg;\n"
                + "    this.genericArg2 = genericArg2;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(genericArg, genericArg2);\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof ExpensiveValueWithMoreGenericsInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueWithMoreGenericsInputs cachedValueInputs = (ExpensiveValueWithMoreGenericsInputs) other;\n"
                + "    if (genericArg != null ? !genericArg.equals(cachedValueInputs.genericArg) : cachedValueInputs.genericArg != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (genericArg2 != null ? !genericArg2.equals(cachedValueInputs.genericArg2) : cachedValueInputs.genericArg2 != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
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
                specMethodModel, "expensiveValueWithMoreGenericsAndContext", RunMode.normal())
            .toString();
    assertThat(inputsClassWithGenericParam)
        .isEqualTo(
            "private static class ExpensiveValueWithMoreGenericsAndContextInputs<E extends java.lang.CharSequence> {\n"
                + "  private final E genericArg;\n"
                + "\n"
                + "  private final E genericArg2;\n"
                + "\n"
                + "  ExpensiveValueWithMoreGenericsAndContextInputs(E genericArg, E genericArg2) {\n"
                + "    this.genericArg = genericArg;\n"
                + "    this.genericArg2 = genericArg2;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(genericArg, genericArg2);\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof ExpensiveValueWithMoreGenericsAndContextInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    ExpensiveValueWithMoreGenericsAndContextInputs cachedValueInputs = (ExpensiveValueWithMoreGenericsAndContextInputs) other;\n"
                + "    if (genericArg != null ? !genericArg.equals(cachedValueInputs.genericArg) : cachedValueInputs.genericArg != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    if (genericArg2 != null ? !genericArg2.equals(cachedValueInputs.genericArg2) : cachedValueInputs.genericArg2 != null) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    return true;\n"
                + "  }\n"
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
                specMethodModel, "moreExpensiveValue", RunMode.normal())
            .toString();
    assertThat(expensiveValueInputsClass)
        .isEqualTo(
            "private static class MoreExpensiveValueInputs {\n"
                + "  private final boolean arg0;\n"
                + "\n"
                + "  private final int arg1;\n"
                + "\n"
                + "  MoreExpensiveValueInputs(boolean arg0, int arg1) {\n"
                + "    this.arg0 = arg0;\n"
                + "    this.arg1 = arg1;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(arg0, arg1);\n"
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
                + "    MoreExpensiveValueInputs cachedValueInputs = (MoreExpensiveValueInputs) other;\n"
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
                specMethodModel, "moreExpensiveValueWithContext", RunMode.normal())
            .toString();
    assertThat(expensiveValueInputsClass)
        .isEqualTo(
            "private static class MoreExpensiveValueWithContextInputs {\n"
                + "  private final boolean arg0;\n"
                + "\n"
                + "  private final int arg1;\n"
                + "\n"
                + "  MoreExpensiveValueWithContextInputs(boolean arg0, int arg1) {\n"
                + "    this.arg0 = arg0;\n"
                + "    this.arg1 = arg1;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public int hashCode() {\n"
                + "    return com.facebook.litho.CommonUtils.hash(arg0, arg1);\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public boolean equals(java.lang.Object other) {\n"
                + "    if (this == other) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (other == null || !(other instanceof MoreExpensiveValueWithContextInputs)) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    MoreExpensiveValueWithContextInputs cachedValueInputs = (MoreExpensiveValueWithContextInputs) other;\n"
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
}
