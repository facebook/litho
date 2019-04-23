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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.facebook.litho.specmodels.model.TagModel;
import com.squareup.javapoet.ClassName;
import java.util.LinkedHashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link TagGenerator} */
@RunWith(JUnit4.class)
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

    final TypeSpecDataHolder dataHolder = TagGenerator.generate(specModel, new LinkedHashSet<>());

    assertThat(dataHolder.getSuperInterfaces())
        .hasSize(2)
        .contains(ClassName.get(Tag1.class))
        .contains(ClassName.get(Tag2.class));
  }
}
