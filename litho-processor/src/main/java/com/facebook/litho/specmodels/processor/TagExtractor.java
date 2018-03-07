/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.TagModel;
import com.squareup.javapoet.ClassName;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Tags a {@link TypeElement} and extract possible spec tags from it in the form of a {@link
 * TagModel}, retaining information about supertypes and potential methods for validation
 * purposes.
 */
public final class TagExtractor {

  private TagExtractor() {}

  public static ImmutableList<TagModel> extractTagsFromSpecClass(Types types, TypeElement element) {
    final List<? extends TypeMirror> interfaces = element.getInterfaces();

    final List<TagModel> tags;
    if (interfaces != null) {
      tags =
          interfaces
              .stream()
              .map(t -> ((DeclaredType) t).asElement())
              .map(t -> newTagModel(t, types))
              .collect(Collectors.toList());
    } else {
      tags = Collections.emptyList();
    }

    return ImmutableList.copyOf(tags);
  }

  private static TagModel newTagModel(Element typeElement, Types types) {
    return new TagModel(
        ClassName.bestGuess(typeElement.toString()),
        types.directSupertypes(typeElement.asType()).size()
            > 1, // java.lang.Object is always a supertype
        !typeElement.getEnclosedElements().isEmpty(),
        typeElement);
  }
}
