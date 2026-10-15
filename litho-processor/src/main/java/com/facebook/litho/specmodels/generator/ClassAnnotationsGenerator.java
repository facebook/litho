/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import com.facebook.litho.annotations.Generated;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/** Generates class-level annotations for a given {@link SpecModel}. */
public class ClassAnnotationsGenerator {
  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final TypeSpecDataHolder.Builder builder = TypeSpecDataHolder.newBuilder();

    // Generated components reference their own type, their nested StateContainer/Builder, dynamic
    // value props, and generic delegate return types without type arguments, which trip
    // warnings_as_errors_k22 with rawtypes/unchecked/cast warnings that the per-method suppressions
    // do not fully cover. Generated code is not hand-editable, so suppress these at the class level
    // on every generated component; the nested classes inherit it. A spec may already declare its
    // own @SuppressWarnings; merge into it rather than add a second one, which would be a
    // non-repeatable-annotation error.
    boolean mergedSuppressWarnings = false;
    for (AnnotationSpec annotation : specModel.getClassAnnotations()) {
      if (annotation.type.equals(ClassName.get(SuppressWarnings.class))) {
        builder.addAnnotation(withRawTypeSuppressions(annotation));
        mergedSuppressWarnings = true;
      } else {
        builder.addAnnotation(annotation);
      }
    }

    builder.addAnnotation(AnnotationSpec.builder(Generated.class).build());

    if (!mergedSuppressWarnings) {
      builder.addAnnotation(withRawTypeSuppressions(null));
    }

    return builder.build();
  }

  /**
   * Returns a {@code @SuppressWarnings} annotation that carries the rawtypes/unchecked/cast
   * categories emitted by generated components, preserving any values already present on {@code
   * existing} (or starting fresh when it is null).
   */
  private static AnnotationSpec withRawTypeSuppressions(@Nullable AnnotationSpec existing) {
    final AnnotationSpec.Builder builder = AnnotationSpec.builder(SuppressWarnings.class);
    final Set<String> existingValues = new HashSet<>();
    if (existing != null) {
      for (String member : existing.members.keySet()) {
        final List<CodeBlock> values = existing.members.get(member);
        if (values != null) {
          for (CodeBlock value : values) {
            builder.addMember(member, value);
            if (member.equals("value")) {
              existingValues.add(value.toString());
            }
          }
        }
      }
    }
    // Only add a category the spec's own @SuppressWarnings doesn't already list, to avoid emitting
    // duplicate entries like {"rawtypes", "rawtypes", "unchecked", "cast"} in generated sources.
    for (String category : new String[] {"rawtypes", "unchecked", "cast"}) {
      final CodeBlock value = CodeBlock.of("$S", category);
      if (existingValues.add(value.toString())) {
        builder.addMember("value", value);
      }
    }
    return builder.build();
  }
}
