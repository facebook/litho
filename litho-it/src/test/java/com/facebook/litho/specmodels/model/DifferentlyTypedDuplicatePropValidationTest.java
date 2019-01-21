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
import static org.mockito.Mockito.mock;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Rule;
import org.junit.Test;

public class DifferentlyTypedDuplicatePropValidationTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  private final LayoutSpecModelFactory mFactory = new LayoutSpecModelFactory();

  interface Drawable {}

  @Event(returnType = boolean.class)
  public static class TestEvent {
    public boolean testValue;
  }

  @LayoutSpec
  public static class DupeLayoutSpec {

    @OnCreateLayout
    static void OnCreateLayout(ComponentContext c, @Prop String prop1) {}

    // Note that prop1 here has the same name but a different type.
    @OnEvent(TestEvent.class)
    static void onEvent(@Prop StringBuffer prop1) {}
  }

  @Test
  public void testDuplicatePropValidationError() {
    final Elements elements = mCompilationRule.getElements();
    final Types types = mCompilationRule.getTypes();
    final TypeElement typeElement =
        elements.getTypeElement(DupeLayoutSpec.class.getCanonicalName());
    final LayoutSpecModel layoutSpecModel =
        mFactory.create(
            elements, types, typeElement, mock(Messager.class), RunMode.normal(), null, null);

    final List<SpecModelValidationError> specModelValidationErrors =
        SpecModelValidation.validateLayoutSpecModel(layoutSpecModel, RunMode.normal());

    assertThat(specModelValidationErrors)
        .extracting("message")
        .contains(
            "The prop prop1 is defined differently in different methods. "
                + "Ensure that each instance of this prop is declared in the same way "
                + "(this means having the same type, resType and values for isOptional, "
                + "isCommonProp and overrideCommonPropBehavior).");
  }
}
