/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.squareup.javapoet.AnnotationSpec;
import org.junit.Test;

/** Tests {@link ClassAnnotationsGenerator} */
public class ClassAnnotationsGeneratorTest {
  @Test
  public void testGenerateAnnotations() {
    final ImmutableList<AnnotationSpec> annotations =
        ImmutableList.of(
            AnnotationSpec.builder(Deprecated.class).build(),
            AnnotationSpec.builder(OnEvent.class).build());
    final SpecModel specModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName("com.example.MyComponentSpec")
            .delegateMethods(ImmutableList.<SpecMethodModel<DelegateMethod, Void>>of())
            .representedObject(new Object())
            .classAnnotations(annotations)
            .build();

    final TypeSpecDataHolder dataHolder = ClassAnnotationsGenerator.generate(specModel);

    assertThat(dataHolder.getAnnotationSpecs())
        .hasSize(2)
        .contains(AnnotationSpec.builder(Deprecated.class).build())
        .contains(AnnotationSpec.builder(OnEvent.class).build());
  }
}
