/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ComponentsProcessor extends AbstractComponentsProcessor {

  @Override
  protected DependencyInjectionHelper getDependencyInjectionGenerator(TypeElement typeElement) {
    return null;
  }

  @Override
  protected SpecModel getLayoutSpecModel(TypeElement typeElement) {
    return LayoutSpecModelFactory.create(
        processingEnv.getElementUtils(),
        typeElement,
        getDependencyInjectionGenerator(typeElement));
  }
}
