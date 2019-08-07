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

package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import androidx.annotation.Nullable;
import com.facebook.litho.Component;
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

    @OnCalculateCachedValue(name = "moreExpensiveValue")
    static String onCreateMoreExpensiveValue(@Prop boolean arg0, @State int arg1) {
      return "DEF";
    }

    @OnCreateLayout
    public void testDelegateMethod(
        @Prop boolean arg0,
        @Prop @Nullable Component arg1,
        @Prop List<Component> arg2,
        @Prop List<String> arg3,
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
        CachedValueGenerator.createInputsClass(mLayoutSpecModel, specMethodModel, "expensiveValue")
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
                mLayoutSpecModel, specMethodModel, "moreExpensiveValue")
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
}
