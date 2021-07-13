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
import static org.mockito.Mockito.mock;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.facebook.litho.widget.EmptyComponent;
import com.google.common.collect.Iterables;
import com.google.testing.compile.CompilationRule;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link TreePropValidation} */
@RunWith(JUnit4.class)
public class TreePropValidationTest {
  @Rule
  public CompilationRule mCompilationRule = new CompilationRule();

  @LayoutSpec
  static class TestSpec {

    @OnCreateLayout
    public static Component onCreateLayout(ComponentContext c) {
      return EmptyComponent.create(c).build();
    }

    @OnCreateTreeProp
    public static void treePropReturningVoid(
        ComponentContext c,
        @Prop int myProp
    ) {
    }

    @OnCreateTreeProp
    public static TestTreeProp treePropWithoutContextArg(
        @Prop TestTreeProp myProp
    ) {
      return myProp;
    }

  }

  static class TestTreeProp {
  }

  @Test
  public void testOnCreateTreePropMethod() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement = elements.getTypeElement(TestSpec.class.getCanonicalName());
    LayoutSpecModel specModel =
        new LayoutSpecModelFactory().create(
            elements, types, typeElement, mock(Messager.class), RunMode.normal(), null, null);

    SpecMethodModel<?, ?> methodReturningVoid = Iterables.find(
        specModel.getDelegateMethods(),
        delegateMethod -> delegateMethod.name.toString().equals("treePropReturningVoid"));
    SpecMethodModel<?, ?> methodWithoutContextArg = Iterables.find(
        specModel.getDelegateMethods(),
        delegateMethod -> delegateMethod.name.toString().equals("treePropWithoutContextArg"));

    List<SpecModelValidationError> validationErrors = TreePropValidation.validate(specModel);
    assertThat(validationErrors).hasSize(2);

    assertThat(validationErrors.get(0).element)
        .isEqualTo(methodReturningVoid.representedObject);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("@OnCreateTreeProp methods cannot return void.");

    assertThat(validationErrors.get(1).element)
        .isEqualTo(methodWithoutContextArg.representedObject);
    assertThat(validationErrors.get(1).message)
        .isEqualTo(
            "The first argument of an @OnCreateTreeProp method should be "
                + "com.facebook.litho.ComponentContext.");
  }
}
