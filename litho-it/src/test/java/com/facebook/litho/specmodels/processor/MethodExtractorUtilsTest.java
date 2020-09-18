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

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getMethodParams;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getTypeVariables;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtilsTestHelper.assertOnAttachedHasInfoForAllParams;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtilsTestHelper.assertOnAttachedHasNoTypeVars;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtilsTestHelper.assertOnDetachedHasInfoForAllTypeVars;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtilsTestHelper.assertOnDetachedHasNoParams;
import static org.mockito.Mockito.mock;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.Prop;
import com.google.testing.compile.CompilationRule;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MethodExtractorUtilsTest {
  @Rule public final CompilationRule mCompilationRule = new CompilationRule();
  List<ExecutableElement> methods;

  @Before
  public void setUp() {
    final Elements elements = mCompilationRule.getElements();
    final TypeElement typeElement =
        elements.getTypeElement(MethodExtractorUtilsTest.TestClass.class.getCanonicalName());
    methods = ElementFilter.methodsIn(typeElement.getEnclosedElements());
  }

  @Test
  public void getMethodParams_forMethodWithNoParams_returnsEmptyList() {
    assertOnDetachedHasNoParams(
        getMethodParams(
            methods.get(1),
            mock(Messager.class),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
  }

  @Test
  public void getMethodParams_forMethodwithParams_returnsInfoForAllParams() {
    assertOnAttachedHasInfoForAllParams(
        getMethodParams(
            methods.get(0),
            mock(Messager.class),
            Collections.singletonList(Prop.class),
            Collections.emptyList(),
            Collections.emptyList()));
  }

  @Test
  public void getTypeVariables_forMethodWithNoTypeVars_returnsEmptyList() {
    assertOnAttachedHasNoTypeVars(getTypeVariables(methods.get(0)));
  }

  @Test
  public void getTypeVariables_forMethodWithTypeVars_returnsInfoForAllTypeVars() {
    assertOnDetachedHasInfoForAllTypeVars(getTypeVariables(methods.get(1)));
  }

  static class TestClass {
    static void onAttached(ComponentContext c, int num, @Prop Object prop) {}

    static <T, U extends List> void onDetached() {}
  }
}
