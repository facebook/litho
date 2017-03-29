/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropJavadocModel;

/**
 * Extracts javadocs from the given input.
 */
public class JavadocExtractor {
  private static final Pattern JAVADOC_SANITIZER = Pattern.compile("^\\s", Pattern.MULTILINE);

  /**
   * Get the class javadoc from the given {@link TypeElement}.
   */
  @Nullable
  static String getClassJavadoc(Elements elements, TypeElement typeElement) {
    final String unsanitizedJavadoc = elements.getDocComment(typeElement);

    if (unsanitizedJavadoc == null || unsanitizedJavadoc.isEmpty()) {
      return null;
    }

    final String javadoc = JAVADOC_SANITIZER.matcher(unsanitizedJavadoc).replaceAll("");
    final int firstPropJavadocIndex = javadoc.indexOf("@prop ");

    return firstPropJavadocIndex < 0 ? javadoc : javadoc.substring(0, firstPropJavadocIndex);
  }

  static ImmutableList<PropJavadocModel> getPropJavadocs(
      Elements elements,
      TypeElement typeElement) {
    final String unsanitizedJavadoc = elements.getDocComment(typeElement);

    if (unsanitizedJavadoc == null || unsanitizedJavadoc.isEmpty()) {
      return ImmutableList.of();
    }

    final String javadoc = JAVADOC_SANITIZER.matcher(unsanitizedJavadoc).replaceAll("");

    final String[] propJavadocs = javadoc.split("@prop ");
    final List<PropJavadocModel> propJavadocModels = new ArrayList<>(propJavadocs.length);

    for (int i = 1, size = propJavadocs.length; i < size; i++) {
      final String propJavadoc = propJavadocs[i];
      // Each prop comment line look like:
      // @prop propName comment for the prop.
      final String[] propJavadocContents = propJavadoc.split(" ", 2);

      if (propJavadocContents.length == 2) {
        propJavadocModels.add(
            new PropJavadocModel(
                propJavadocContents[0],
                propJavadocContents[1].replace('\n', ' ')));
      }
    }

    return ImmutableList.copyOf(propJavadocModels);
  }
}
