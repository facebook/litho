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

import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.AnnotationSpec;

/** Generates class-level annotations for a given {@link SpecModel}. */
public class ClassAnnotationsGenerator {
  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final TypeSpecDataHolder.Builder holder = TypeSpecDataHolder.newBuilder();

    if (specModel.hasInjectedDependencies()) {
      for (AnnotationSpec annotation : specModel.getClassAnnotations()) {
        if (specModel
            .getDependencyInjectionHelper()
            .isValidGeneratedComponentAnnotation(annotation)) {
          holder.addAnnotation(annotation);
        }
      }
    } else {
      holder.addAnnotations(specModel.getClassAnnotations());
    }

    return holder.build();
  }
}
