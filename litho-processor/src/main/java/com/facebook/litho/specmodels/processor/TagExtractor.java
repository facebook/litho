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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.TagModel;
import com.squareup.javapoet.ClassName;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Tags a {@link TypeElement} and extract possible spec tags from it in the form of a {@link
 * TagModel}, retaining information about supertypes and potential methods for validation purposes.
 */
public final class TagExtractor {

  private TagExtractor() {}

  public static ImmutableList<TagModel> extractTagsFromSpecClass(
      Types types, TypeElement element, EnumSet<RunMode> runMode) {
    final List<? extends TypeMirror> interfaces = element.getInterfaces();

    final List<TagModel> tags;
    if (interfaces != null) {
      tags =
          interfaces.stream()
              .map(t -> ((DeclaredType) t).asElement())
              .map(t -> newTagModel(t, types, runMode))
              .collect(Collectors.toList());
    } else {
      tags = Collections.emptyList();
    }

    return ImmutableList.copyOf(tags);
  }

  private static TagModel newTagModel(Element typeElement, Types types, EnumSet<RunMode> runMode) {
    return new TagModel(
        ClassName.bestGuess(typeElement.toString()),
        !runMode.contains(RunMode.ABI)
            && types.directSupertypes(typeElement.asType()).size()
                > 1, // java.lang.Object is always a supertype
        !runMode.contains(RunMode.ABI) && !typeElement.getEnclosedElements().isEmpty(),
        typeElement);
  }
}
