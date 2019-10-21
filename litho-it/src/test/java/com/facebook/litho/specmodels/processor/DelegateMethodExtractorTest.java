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

import static org.mockito.Mockito.mock;

import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.google.testing.compile.CompilationRule;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link DelegateMethodExtractor} */
@RunWith(JUnit4.class)
public class DelegateMethodExtractorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  static class TestClass {

    @OnCreateLayout
    public void testMethod(
        @Prop boolean testProp, @State int testState, @Event Object testPermittedAnnotation) {
      // Don't do anything.
    }

    @OnEvent(Object.class)
    public void ignored() {}

    @OnUpdateState
    public void alsoIgnored() {}
  }

  @Test
  public void testMethodExtraction() {
    Elements elements = mCompilationRule.getElements();
    TypeElement typeElement = elements.getTypeElement(TestClass.class.getCanonicalName());

    List<Class<? extends Annotation>> permittedParamAnnotations = new ArrayList<>();
    permittedParamAnnotations.add(Event.class);

    ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods =
        DelegateMethodExtractor.getDelegateMethods(
            typeElement,
            new ArrayList<>(DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP.keySet()),
            permittedParamAnnotations,
            ImmutableList.<Class<? extends Annotation>>of(),
            mock(Messager.class));

    DelegateMethodExtractorTestHelper.assertDelegateMethodExtraction(delegateMethods);
  }
}
