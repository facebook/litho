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
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.testing.TestSpecGenerator;
import com.facebook.litho.specmodels.model.testing.TestSpecModel;
import com.facebook.litho.specmodels.processor.InterStageStore;
import com.facebook.litho.specmodels.processor.PropNameInterStageStore;
import com.google.testing.compile.CompilationRule;
import java.util.Optional;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Name;
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

/** Tests {@link TestSpecModelFactory} for an enclosed {@link LayoutSpec}. */
@RunWith(JUnit4.class)
public class TestLayoutSpecModelFactoryTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private Elements mElements;
  private Types mTypes;
  private TypeElement mTypeElement;
  @Mock private Messager mMessager;

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
    MockitoAnnotations.initMocks(this);
    mElements = mCompilationRule.getElements();
    mTypes = mCompilationRule.getTypes();
    mTypeElement = mElements.getTypeElement(TestMyLayoutSpec.class.getCanonicalName());
  }

  @Test
  public void testCreate() {
    final TestSpecModelFactory factory = new TestSpecModelFactory();
    final TestSpecModel layoutSpecModel =
        factory.create(mElements, mTypes, mTypeElement, mMessager, RunMode.normal(), null, null);

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
        factory.create(
            mElements, mTypes, mTypeElement, mMessager, RunMode.normal(), null, interStageStore);

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

    final TestSpecModel layoutSpecModel =
        factory.create(mElements, mTypes, mTypeElement, mMessager, RunMode.normal(), null, null);
    layoutSpecModel.generate(RunMode.normal());

    verify(specGenerator).generate(layoutSpecModel);
  }
}
