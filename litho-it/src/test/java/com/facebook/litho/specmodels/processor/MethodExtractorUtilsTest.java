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
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.TypeVariableName;
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
    assertThat(
            getMethodParams(
                methods.get(1),
                mock(Messager.class),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()))
        .isEmpty();
  }

  @Test
  public void getMethodParams_forMethodwithParams_returnsInfoForAllParams() {
    final List<MethodParamModel> onAttachedMethodParams =
        getMethodParams(
            methods.get(0),
            mock(Messager.class),
            Collections.singletonList(Prop.class),
            Collections.emptyList(),
            Collections.emptyList());

    assertThat(onAttachedMethodParams).hasSize(3);

    assertThat(onAttachedMethodParams.get(0).getName()).isEqualTo("c");
    assertThat(onAttachedMethodParams.get(0).getTypeName().toString())
        .isEqualTo("com.facebook.litho.ComponentContext");
    assertThat(onAttachedMethodParams.get(0).getAnnotations()).isEmpty();

    assertThat(onAttachedMethodParams.get(1).getName()).isEqualTo("num");
    assertThat(onAttachedMethodParams.get(1).getTypeName().toString()).isEqualTo("int");
    assertThat(onAttachedMethodParams.get(1).getAnnotations()).isEmpty();

    assertThat(onAttachedMethodParams.get(2).getName()).isEqualTo("prop");
    assertThat(onAttachedMethodParams.get(2).getTypeName().toString())
        .isEqualTo("java.lang.Object");
    assertThat(onAttachedMethodParams.get(2).getAnnotations()).hasSize(1);
    assertThat(onAttachedMethodParams.get(2).getAnnotations().get(0).annotationType().getTypeName())
        .isEqualTo("com.facebook.litho.annotations.Prop");
  }

  @Test
  public void getTypeVariables_forMethodWithNoTypeVars_returnsEmptyList() {
    assertThat(getTypeVariables(methods.get(0))).isEmpty();
  }

  @Test
  public void getTypeVariables_forMethodWithTypeVars_returnsInfoForAllTypeVars() {
    final List<TypeVariableName> onDetachedTypeVars = getTypeVariables(methods.get(1));

    assertThat(onDetachedTypeVars).hasSize(2);

    assertThat(onDetachedTypeVars.get(0).name).isEqualTo("T");
    assertThat(onDetachedTypeVars.get(0).bounds).isEmpty();

    assertThat(onDetachedTypeVars.get(1).name).isEqualTo("U");
    assertThat(onDetachedTypeVars.get(1).bounds).hasSize(1);
    assertThat(onDetachedTypeVars.get(1).bounds.get(0).toString()).isEqualTo("java.util.List");
  }

  static class TestClass {
    static void onAttached(ComponentContext c, int num, @Prop Object prop) {}

    static <T, U extends List> void onDetached() {}
  }
}
