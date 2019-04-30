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

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.Component;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link SimpleNameDelegateGenerator} */
@RunWith(JUnit4.class)
public class SimpleNameDelegateGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  @LayoutSpec
  private static class TestWithoutDelegateSpec {

    @OnCreateLayout
    public void onCreateLayout(@Prop boolean arg0, @Prop Component delegate) {}
  }

  @LayoutSpec(simpleNameDelegate = "delegate")
  private static class TestWithDelegateSpec {

    @OnCreateLayout
    public void onCreateLayout(@Prop boolean arg0, @Prop Component delegate) {}
  }

  private LayoutSpecModel getSpecModel(Class specClass) {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement = elements.getTypeElement(specClass.getCanonicalName());

    return mLayoutSpecModelFactory.create(
        elements, types, typeElement, mock(Messager.class), RunMode.normal(), null, null);
  }

  @Test
  public void testGenerateWithoutDelegate() {
    TypeSpecDataHolder dataHolder =
        SimpleNameDelegateGenerator.generate(getSpecModel(TestWithoutDelegateSpec.class));

    assertThat(dataHolder.getMethodSpecs()).hasSize(0);
  }

  @Test
  public void testGenerateWithDelegate() {
    TypeSpecDataHolder dataHolder =
        SimpleNameDelegateGenerator.generate(getSpecModel(TestWithDelegateSpec.class));

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.Component getSimpleNameDelegate() {\n"
                + "  return delegate;\n"
                + "}\n");
  }
}
