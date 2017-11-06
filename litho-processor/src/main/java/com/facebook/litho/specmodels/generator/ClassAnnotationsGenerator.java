/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
        if (specModel.getDependencyInjectionHelper()
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
