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

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests {@link LayoutSpecModelFactory} */
public class LayoutSpecModelFactoryTest {
  private static final String TEST_QUALIFIED_SPEC_NAME = "com.facebook.litho.TestSpec";
  private static final String TEST_QUALIFIED_COMPONENT_NAME = "com.facebook.litho.Test";

  @Mock private Messager mMessager;

  Elements mElements = mock(Elements.class);
  Types mTypes = mock(Types.class);
  TypeElement mTypeElement = mock(TypeElement.class);
  DependencyInjectionHelper mDependencyInjectionHelper = mock(DependencyInjectionHelper.class);
  LayoutSpecModelFactory mFactory = new LayoutSpecModelFactory();
  LayoutSpec mLayoutSpec = mock(LayoutSpec.class);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mElements.getDocComment(mTypeElement)).thenReturn("");
    when(mTypeElement.getQualifiedName()).thenReturn(new MockName(TEST_QUALIFIED_SPEC_NAME));
    when(mTypeElement.getAnnotation(LayoutSpec.class)).thenReturn(mLayoutSpec);
  }

  @Test
  public void testCreate() {
    LayoutSpecModel layoutSpecModel =
        mFactory.create(
            mElements,
            mTypes,
            mTypeElement,
            mMessager,
            RunMode.normal(),
            mDependencyInjectionHelper,
            null);

    assertThat(layoutSpecModel.getSpecName()).isEqualTo("TestSpec");
    assertThat(layoutSpecModel.getComponentName()).isEqualTo("Test");
    assertThat(layoutSpecModel.getSpecTypeName().toString()).isEqualTo(TEST_QUALIFIED_SPEC_NAME);
    assertThat(layoutSpecModel.getComponentTypeName().toString())
        .isEqualTo(TEST_QUALIFIED_COMPONENT_NAME);

    assertThat(layoutSpecModel.getDelegateMethods()).isEmpty();

    assertThat(layoutSpecModel.getProps()).isEmpty();

    assertThat(layoutSpecModel.hasInjectedDependencies()).isTrue();
    assertThat(layoutSpecModel.getDependencyInjectionHelper()).isSameAs(mDependencyInjectionHelper);
  }

  @Test
  public void testCreateWithSpecifiedName() {
    when(mLayoutSpec.value()).thenReturn("TestComponentName");
    LayoutSpecModel layoutSpecModel =
        mFactory.create(
            mElements,
            mTypes,
            mTypeElement,
            mMessager,
            RunMode.normal(),
            mDependencyInjectionHelper,
            null);

    assertThat(layoutSpecModel.getSpecName()).isEqualTo("TestSpec");
    assertThat(layoutSpecModel.getComponentName()).isEqualTo("TestComponentName");
    assertThat(layoutSpecModel.getSpecTypeName().toString()).isEqualTo(TEST_QUALIFIED_SPEC_NAME);
    assertThat(layoutSpecModel.getComponentTypeName().toString())
        .isEqualTo("com.facebook.litho.TestComponentName");
  }
}
