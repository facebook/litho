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

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ErrorEvent;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnError;
import com.facebook.litho.annotations.OnEvent;
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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests {@link ErrorEventHandlerGenerator}. */
@RunWith(JUnit4.class)
public class ErrorEventHandlerGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();
  @Mock Messager mMessager;

  @LayoutSpec
  static class TestSpec {
    @OnError
    public void testErrorHandler(ComponentContext c, Exception e) {}
  }

  @LayoutSpec
  static class ManualErrorHandlerSpec {
    @OnEvent(ErrorEvent.class)
    static void __internalOnErrorHandler(ComponentContext c, @FromEvent Exception exception) {}
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testHasOnErrorDelegate() {
    final Elements elements = mCompilationRule.getElements();
    final TypeElement typeElement = elements.getTypeElement(TestSpec.class.getCanonicalName());
    final Types types = mCompilationRule.getTypes();
    final LayoutSpecModel specModel =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mMessager, RunMode.normal(), null, null);

    assertThat(ErrorEventHandlerGenerator.hasOnErrorDelegateMethod(specModel.getDelegateMethods()))
        .isTrue();
  }

  @Test
  public void testManualErrorHandlerGeneration() {
    final Elements elements = mCompilationRule.getElements();
    final TypeElement typeElement =
        elements.getTypeElement(ManualErrorHandlerSpec.class.getCanonicalName());
    final Types types = mCompilationRule.getTypes();
    final LayoutSpecModel specModel =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mMessager, RunMode.normal(), null, null);

    assertThat(ErrorEventHandlerGenerator.hasOnErrorDelegateMethod(specModel.getDelegateMethods()))
        .isFalse();

    // This verifies that the synthetic model matches the one defined here, i.e. that we
    // internally generate the method we expect.
    final SpecMethodModel<EventMethod, EventDeclarationModel> generatedErrorEventMethod =
        ErrorEventHandlerGenerator.generateErrorEventHandlerDefinition();
    final SpecMethodModel<EventMethod, EventDeclarationModel> localErrorEventMethod =
        specModel.getEventMethods().get(0);

    // These are properties that we can reliably generate as opposed to those which may
    // differ in their runtime representation.
    assertThat(generatedErrorEventMethod)
        .isEqualToComparingOnlyGivenFields(
            localErrorEventMethod,
            "annotations",
            "modifiers",
            "returnTypeSpec",
            "returnType",
            "typeVariables");

    assertThat(generatedErrorEventMethod.name).hasToString(localErrorEventMethod.name.toString());
    assertThat(generatedErrorEventMethod.typeModel.name)
        .hasToString(localErrorEventMethod.typeModel.name.toString());

    assertThat(generatedErrorEventMethod.typeModel.fields).hasSize(1);
    // The Represented object refers to a javax model which we can't obtain here.
    assertThat(generatedErrorEventMethod.typeModel.fields.get(0))
        .isEqualToIgnoringGivenFields(
            localErrorEventMethod.typeModel.fields.get(0), "representedObject");
  }
}
