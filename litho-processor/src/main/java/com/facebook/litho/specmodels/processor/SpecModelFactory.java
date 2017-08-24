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
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * A factory for a {@link SpecModel}. It first performs an {@link #extract(RoundEnvironment)} step
 * in which it selects the elements it wants to process from the annotation processor's round
 * environment and then creates a {@link SpecModel} for each extracted element in
 * {@link #create(Elements, TypeElement, DependencyInjectionHelper)}.
 */
public interface SpecModelFactory {
  /**
   * Extract the relevant Elements to work with from the round environment before they're passed on
   * to {@link #create(Elements, TypeElement, DependencyInjectionHelper)}.
   */
  Set<Element> extract(RoundEnvironment roundEnvironment);

  /**
   * Create a {@link SpecModel} from the given {@link TypeElement} and an optional {@link
   * DependencyInjectionHelper}.
   */
  SpecModel create(
      Elements elements,
      TypeElement element,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper);
}
