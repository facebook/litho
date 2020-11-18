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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
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

/** Tests {@link LayoutSpecModelFactory} */
@RunWith(JUnit4.class)
public class LayoutSpecModelFactoryTest {
  private static final String TEST_QUALIFIED_SPEC_NAME =
      "com.facebook.litho.specmodels.processor.LayoutSpecModelFactoryTest.TestLayoutSpec";
  private static final String TEST_QUALIFIED_COMPONENT_NAME =
      "com.facebook.litho.specmodels.processor.LayoutSpecModelFactoryTest.TestLayoutComponentName";

  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final LayoutSpecModelFactory mFactory = new LayoutSpecModelFactory();
  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private LayoutSpecModel mLayoutSpecModel;

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement =
        elements.getTypeElement(LayoutSpecModelFactoryTest.TestLayoutSpec.class.getCanonicalName());

    mLayoutSpecModel =
        mFactory.create(
            elements,
            types,
            typeElement,
            mock(Messager.class),
            RunMode.normal(),
            mDependencyInjectionHelper,
            null);
  }

  @Test
  public void create_forLayoutSpec_populateGenericSpecInfo() {
    // can't move to helper, because PsiLayoutSpecModelFactoryTest doesn't support qualified names
    assertThat(mLayoutSpecModel.getSpecTypeName().toString()).isEqualTo(TEST_QUALIFIED_SPEC_NAME);
    assertThat(mLayoutSpecModel.getComponentTypeName().toString())
        .isEqualTo(TEST_QUALIFIED_COMPONENT_NAME);
    LayoutSpecModelFactoryTestHelper.create_forLayoutSpec_populateGenericSpecInfo(
        mLayoutSpecModel, mDependencyInjectionHelper);
  }

  @Test
  public void create_forLayoutSpec_populateOnAttachInfo() {
    LayoutSpecModelFactoryTestHelper.create_forLayoutSpec_populateOnAttachInfo(mLayoutSpecModel);
  }

  @Test
  public void create_forLayoutSpec_populateOnDetachInfo() {
    LayoutSpecModelFactoryTestHelper.create_forLayoutSpec_populateOnDetachInfo(mLayoutSpecModel);
  }

  @LayoutSpec(value = "TestLayoutComponentName")
  static class TestLayoutSpec {

    @OnCreateInitialState
    static void createInitialState(@Prop int prop1) {}

    @OnCreateLayout
    static Component onCreateLayout(ComponentContext c, @Prop int prop2) {
      return null;
    }

    @OnAttached
    static void onAttached(ComponentContext c, @Prop Object prop3, @State Object state1) {}

    @OnDetached
    static void onDetached(ComponentContext c, @Prop Object prop4, @State Object state2) {}
  }
}
