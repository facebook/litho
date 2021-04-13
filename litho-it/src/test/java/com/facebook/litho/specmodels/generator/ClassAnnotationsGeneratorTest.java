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

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.Generated;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.squareup.javapoet.AnnotationSpec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link ClassAnnotationsGenerator} */
@RunWith(JUnit4.class)
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
        .hasSize(3)
        .contains(AnnotationSpec.builder(Generated.class).build())
        .contains(AnnotationSpec.builder(Deprecated.class).build())
        .contains(AnnotationSpec.builder(OnEvent.class).build());
  }
}
