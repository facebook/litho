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

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidation;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.google.testing.compile.CompilationRule;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DuplicatePropValidationTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  private final MountSpecModelFactory mFactory = new MountSpecModelFactory();

  interface Drawable {}

  @MountSpec
  public static class DupeMountSpec {

    @OnPrepare
    static void onPrepare(ComponentContext c, @Prop(optional = true) String prop1) {}

    @OnMount
    static void onMount(
        ComponentContext c, Drawable drawable, @Prop String prop1, @Prop int prop2) {}

    @OnCreateMountContent
    static Drawable onCreateMountContent(ComponentContext c) {
      return null;
    }
  }

  @Test
  public void testDuplicatePropValidationError() {
    final Elements elements = mCompilationRule.getElements();
    final Types types = mCompilationRule.getTypes();
    final TypeElement typeElement = elements.getTypeElement(DupeMountSpec.class.getCanonicalName());
    final MountSpecModel mountSpecModel =
        mFactory.create(
            elements, types, typeElement, mock(Messager.class), RunMode.normal(), null, null);

    final List<SpecModelValidationError> specModelValidationErrors =
        SpecModelValidation.validateMountSpecModel(mountSpecModel, RunMode.normal());

    assertThat(specModelValidationErrors)
        .extracting("message")
        .contains(
            "The prop prop1 is defined differently in different methods. "
                + "Ensure that each instance of this prop is declared in the same way "
                + "(this means having the same type, resType and values for isOptional, "
                + "isCommonProp and overrideCommonPropBehavior).");
  }
}
