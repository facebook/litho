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

import static com.facebook.litho.specmodels.generator.PreambleGenerator.generateConstructor;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.squareup.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link PreambleGenerator} */
@RunWith(JUnit4.class)
public class PreambleGeneratorTest {
  private static final String TEST_QUALIFIED_SPEC_NAME = "com.facebook.litho.TestSpec";

  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private SpecModel mSpecModelWithoutDI;
  private SpecModel mSpecModelWithDI;

  @Before
  public void setUp() {
    mSpecModelWithoutDI = SpecModelImpl.newBuilder()
        .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
        .delegateMethods(ImmutableList.<SpecMethodModel<DelegateMethod, Void>>of())
        .representedObject(new Object())
        .build();

    mSpecModelWithDI =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .delegateMethods(ImmutableList.<SpecMethodModel<DelegateMethod, Void>>of())
            .dependencyInjectionHelper(mDependencyInjectionHelper)
            .representedObject(new Object())
            .build();
  }

  @Test
  public void testGenerateConstructorWithoutDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder =
        generateConstructor(mSpecModelWithoutDI);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo("private Constructor() {\n  super(\"Test\");\n" + "}\n");
  }

  @Test
  public void testGenerateConstructorWithDependencyInjection() {
    final MethodSpec constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addStatement("final Object testObject = new TestObject()")
            .build();

    when(mDependencyInjectionHelper.generateConstructor(mSpecModelWithDI))
        .thenReturn(constructor);

    TypeSpecDataHolder typeSpecDataHolder = generateConstructor(mSpecModelWithDI);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "public Constructor() {\n"
                + "  super(\"Test\");\n  final Object testObject = new TestObject();\n"
                + "}\n");
  }
}
