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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.facebook.litho.specmodels.model.TagModel;
import com.squareup.javapoet.ClassName;
import org.junit.Test;

/** Tests {@link TagGenerator} */
public class TagGeneratorTest {

  interface Tag1 {}

  interface Tag2 {}

  @Test
  public void testGenerateTag() {
    final ImmutableList<TagModel> tags =
        ImmutableList.of(
            new TagModel(ClassName.get(Tag1.class), false, false, new Object()),
            new TagModel(ClassName.get(Tag2.class), false, false, new Object()));

    final SpecModel specModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName("com.example.MyComponentSpec")
            .delegateMethods(ImmutableList.<SpecMethodModel<DelegateMethod, Void>>of())
            .representedObject(new Object())
            .tags(tags)
            .build();

    final TypeSpecDataHolder dataHolder = TagGenerator.generate(specModel);

    assertThat(dataHolder.getSuperInterfaces())
        .hasSize(2)
        .contains(ClassName.get(Tag1.class))
        .contains(ClassName.get(Tag2.class));
  }
}
