/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TestSpec;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.TestSpecGenerator;
import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests {@link TestSpecModelFactory} for an enclosed {@link LayoutSpec}. */
public class TestLayoutSpecModelFactoryTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private Elements mElements;
  private TypeElement mTypeElement;

  @LayoutSpec
  static class MyLayoutSpec {
    @OnCreateLayout
    public static ComponentLayout onCreateLayout(ComponentContext c, @Prop String s) {
      return Row.create(c).build();
    }
  }

  @TestSpec(MyLayoutSpec.class)
  interface TestMyLayoutSpec {}

  @Before
  public void setUp() {
    mElements = mCompilationRule.getElements();
    mTypeElement = mElements.getTypeElement(TestMyLayoutSpec.class.getCanonicalName());
  }

  @Test
  public void testCreate() {
    final TestSpecModelFactory factory = new TestSpecModelFactory();
    final SpecModel layoutSpecModel = factory.create(mElements, mTypeElement, null);

    assertThat(layoutSpecModel.getSpecName()).isEqualTo("TestMyLayoutSpec");
    assertThat(layoutSpecModel.getComponentName()).isEqualTo("TestMyLayout");
    assertThat(layoutSpecModel.getSpecTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.TestLayoutSpecModelFactoryTest.TestMyLayoutSpec");
    assertThat(layoutSpecModel.getComponentTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.TestLayoutSpecModelFactoryTest.TestMyLayout");

    assertThat(layoutSpecModel.getDelegateMethods().stream().map(m -> m.name.toString()).toArray())
        .hasSize(1)
        .contains("onCreateLayout", atIndex(0));
    assertThat(layoutSpecModel.getProps().stream().map(PropModel::getName).toArray())
        .hasSize(1)
        .contains("s", atIndex(0));
  }

  @Test
  public void testDelegation() {
    final TestSpecGenerator specGenerator = mock(TestSpecGenerator.class);
    final TestSpecModelFactory factory = new TestSpecModelFactory(specGenerator);

    final SpecModel layoutSpecModel = factory.create(mElements, mTypeElement, null);
    layoutSpecModel.generate();

    verify(specGenerator).generate(layoutSpecModel);
  }
}
