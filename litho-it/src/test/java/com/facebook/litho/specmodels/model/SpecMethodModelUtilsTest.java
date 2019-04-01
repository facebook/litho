/*
 * Copyright 2019-present Facebook, Inc.
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
import static org.mockito.Mockito.mock;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests {@link SpecMethodModelUtils} */
public class SpecMethodModelUtilsTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  @LayoutSpec
  static class TestSpec {

    @OnEvent(Object.class)
    public void noStateEventMethod(@Prop boolean arg0) {}

    @OnEvent(Object.class)
    public void hasOnlyNormalStateEventMethod(
        @Prop boolean arg0, @State int arg1, @Param Object arg2, @FromEvent long arg3) {}

    @OnEvent(Object.class)
    public void hasLazyStateEventMethod(
        @Prop boolean arg0, @State int arg1, @State(canUpdateLazily = true) long arg4) {}
  }

  private SpecModel mSpecModel;

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement = elements.getTypeElement(TestSpec.class.getCanonicalName());
    mSpecModel =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mock(Messager.class), RunMode.normal(), null, null);
  }

  @Test
  public void testHasLazyStateParams() {
    SpecMethodModel<?, ?> noStateEventMethod = mSpecModel.getEventMethods().get(0);
    assertThat(noStateEventMethod.name.toString()).isEqualTo("noStateEventMethod");
    assertThat(SpecMethodModelUtils.hasLazyStateParams(noStateEventMethod)).isFalse();

    SpecMethodModel<?, ?> hasOnlyNormalStateEventMethod = mSpecModel.getEventMethods().get(1);
    assertThat(hasOnlyNormalStateEventMethod.name.toString())
        .isEqualTo("hasOnlyNormalStateEventMethod");
    assertThat(SpecMethodModelUtils.hasLazyStateParams(hasOnlyNormalStateEventMethod)).isFalse();

    SpecMethodModel<?, ?> hasLazyStateEventMethod = mSpecModel.getEventMethods().get(2);
    assertThat(hasLazyStateEventMethod.name.toString()).isEqualTo("hasLazyStateEventMethod");
    assertThat(SpecMethodModelUtils.hasLazyStateParams(hasLazyStateEventMethod)).isTrue();
  }
}
