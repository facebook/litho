/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor.testing;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.TestSpec;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.testing.TestSpecGenerator;
import com.facebook.litho.specmodels.model.testing.TestSpecModel;
import com.facebook.litho.specmodels.processor.InterStageStore;
import com.facebook.litho.specmodels.processor.PropNameInterStageStore;
import com.google.testing.compile.CompilationRule;
import java.util.Optional;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Name;
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
    public static Component onCreateLayout(
        ComponentContext c,
        @Prop String s,
        @Prop Component child,
        @Prop(resType = ResType.DIMEN_SIZE) float size,
        @Prop(optional = true) int i) {
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
    final TestSpecModel layoutSpecModel = factory.create(mElements, mTypeElement, null, null);

    assertThat(layoutSpecModel.getSpecName()).isEqualTo("TestMyLayoutSpec");
    assertThat(layoutSpecModel.getComponentName()).isEqualTo("TestMyLayout");
    assertThat(layoutSpecModel.getSpecTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.testing.TestLayoutSpecModelFactoryTest.TestMyLayoutSpec");
    assertThat(layoutSpecModel.getComponentTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.testing.TestLayoutSpecModelFactoryTest.TestMyLayout");

    assertThat(layoutSpecModel.getProps().stream().map(PropModel::getName).toArray())
        .hasSize(4)
        .contains("child", atIndex(0))
        .contains("i", atIndex(1))
        .contains("s", atIndex(2))
        .contains("size", atIndex(3));
  }

  @Test
  public void testCreateWithCachedProps() {
    final TestSpecModelFactory factory = new TestSpecModelFactory();
    final Filer mockFiler = mock(Filer.class, RETURNS_DEEP_STUBS);
    final InterStageStore interStageStore =
        new InterStageStore() {
          @Override
          public PropNameInterStageStore getPropNameInterStageStore() {
            return new PropNameInterStageStore(mockFiler) {
              @Override
              public Optional<ImmutableList<String>> loadNames(Name qualifiedName) {
                return Optional.of(ImmutableList.of("a", "b", "c", "d"));
              }
            };
          }
        };

    final TestSpecModel layoutSpecModel =
        factory.create(mElements, mTypeElement, null, interStageStore);

    assertThat(layoutSpecModel.getSpecName()).isEqualTo("TestMyLayoutSpec");
    assertThat(layoutSpecModel.getComponentName()).isEqualTo("TestMyLayout");
    assertThat(layoutSpecModel.getSpecTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.testing.TestLayoutSpecModelFactoryTest.TestMyLayoutSpec");
    assertThat(layoutSpecModel.getComponentTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.testing.TestLayoutSpecModelFactoryTest.TestMyLayout");

    assertThat(layoutSpecModel.getProps().stream().map(PropModel::getName).toArray())
        .hasSize(4)
        .contains("a", atIndex(0))
        .contains("b", atIndex(1))
        .contains("c", atIndex(2))
        .contains("d", atIndex(3));
  }

  @Test
  public void testDelegation() {
    final TestSpecGenerator specGenerator = mock(TestSpecGenerator.class);
    final TestSpecModelFactory factory = new TestSpecModelFactory(specGenerator);

    final TestSpecModel layoutSpecModel = factory.create(mElements, mTypeElement, null, null);
    layoutSpecModel.generate();

    verify(specGenerator).generate(layoutSpecModel);
  }
}
