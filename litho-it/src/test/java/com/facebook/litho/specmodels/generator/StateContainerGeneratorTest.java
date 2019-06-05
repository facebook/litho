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
package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.SpecModel;
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

/** Tests {@link StateContainerGenerator} */
@RunWith(JUnit4.class)
public class StateContainerGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  @Mock private Messager mMessager;

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  private SpecModel mSpecModelDI;
  private SpecModel mSpecModelWithTransitionDI;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement =
        elements.getTypeElement(ComponentBodyGeneratorTest.TestSpec.class.getCanonicalName());

    mSpecModelDI =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mMessager, RunMode.normal(), null, null);

    TypeElement typeElementWithTransition =
        elements.getTypeElement(
            ComponentBodyGeneratorTest.TestWithTransitionSpec.class.getCanonicalName());
    mSpecModelWithTransitionDI =
        mLayoutSpecModelFactory.create(
            elements, types, typeElementWithTransition, mMessager, RunMode.normal(), null, null);
  }

  @Test
  public void testGenerateStateContainerImpl() {
    assertThat(StateContainerGenerator.generate(mSpecModelDI).toString())
        .isEqualTo(
            "@androidx.annotation.VisibleForTesting(\n"
                + "    otherwise = 2\n"
                + ")\n"
                + "static class TestStateContainer implements com.facebook.litho.StateContainer {\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 3\n"
                + "  )\n"
                + "  int arg1;\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateContainerWithTransitionImpl() {
    assertThat(StateContainerGenerator.generate(mSpecModelWithTransitionDI).toString())
        .isEqualTo(
            "@androidx.annotation.VisibleForTesting(\n"
                + "    otherwise = 2\n"
                + ")\n"
                + "static class TestWithTransitionStateContainer implements com.facebook.litho.StateContainer, "
                + "com.facebook.litho.ComponentLifecycle.TransitionContainer {\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 3\n"
                + "  )\n"
                + "  int arg1;\n"
                + "\n"
                + "  java.util.List<com.facebook.litho.Transition> _transitions = new java.util.ArrayList<>();\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public java.util.List<com.facebook.litho.Transition> consumeTransitions() {\n"
                + "    if (_transitions.isEmpty()) {\n"
                + "      return java.util.Collections.EMPTY_LIST;\n"
                + "    }\n"
                + "    java.util.List<com.facebook.litho.Transition> transitionsCopy;\n"
                + "    synchronized (_transitions) {\n"
                + "      transitionsCopy = new java.util.ArrayList<>(_transitions);\n"
                + "      _transitions.clear();\n"
                + "    }\n"
                + "    return transitionsCopy;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGetStateContainerClassName() {
    assertThat(StateContainerGenerator.getStateContainerClassName(mSpecModelDI))
        .isEqualTo("TestStateContainer");
  }
}
