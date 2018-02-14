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
import org.junit.Rule;
import org.junit.Test;

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
    final TypeElement typeElement = elements.getTypeElement(DupeMountSpec.class.getCanonicalName());
    final MountSpecModel mountSpecModel =
        mFactory.create(elements, typeElement, mock(Messager.class), RunMode.NORMAL, null, null);

    final List<SpecModelValidationError> specModelValidationErrors =
        SpecModelValidation.validateMountSpecModel(mountSpecModel, RunMode.NORMAL);

    assertThat(specModelValidationErrors)
        .extracting("message")
        .contains(
            "The prop prop1 is defined differently in different methods. "
                + "Ensure that each instance of this prop is declared in the same way "
                + "(this means having the same type, resType and value for isOptional).");
  }
}
