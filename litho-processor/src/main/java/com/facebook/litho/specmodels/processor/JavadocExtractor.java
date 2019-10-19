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
import com.facebook.litho.specmodels.model.PropJavadocModel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/** Extracts javadocs from the given input. */
public class JavadocExtractor {
  private static final Pattern JAVADOC_SANITIZER = Pattern.compile("^\\s", Pattern.MULTILINE);

  /** Get the class javadoc from the given {@link TypeElement}. */
  @Nullable
  public static String getClassJavadoc(Elements elements, TypeElement typeElement) {
    final String unsanitizedJavadoc = elements.getDocComment(typeElement);

    if (unsanitizedJavadoc == null || unsanitizedJavadoc.isEmpty()) {
      return null;
    }

    final String javadoc = JAVADOC_SANITIZER.matcher(unsanitizedJavadoc).replaceAll("");
    final int firstPropJavadocIndex = javadoc.indexOf("@prop ");

    return firstPropJavadocIndex < 0 ? javadoc : javadoc.substring(0, firstPropJavadocIndex);
  }

  public static ImmutableList<PropJavadocModel> getPropJavadocs(
      Elements elements, TypeElement typeElement) {
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
                propJavadocContents[0], propJavadocContents[1].replace('\n', ' ')));
      }
    }

    return ImmutableList.copyOf(propJavadocModels);
  }
}
