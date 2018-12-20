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
package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * A factory for a {@link SpecModel}. It first performs an {@link #extract(RoundEnvironment)} step
 * in which it selects the elements it wants to process from the annotation processor's round
 * environment and then creates a {@link SpecModel} for each extracted element in {@link
 * #create(Elements, Types, TypeElement, Messager, EnumSet, DependencyInjectionHelper,
 * InterStageStore)}.
 */
public interface SpecModelFactory<T extends SpecModel> {
  /**
   * Extract the relevant Elements to work with from the round environment before they're passed on
   * to {@link #create(Elements, Types, TypeElement, Messager, EnumSet, DependencyInjectionHelper,
   * InterStageStore)}.
   */
  Set<Element> extract(RoundEnvironment roundEnvironment);

  /**
   * Create a {@link SpecModel} from the given {@link TypeElement} and an optional {@link
   * DependencyInjectionHelper}. The optional {@link InterStageStore} can be used to augment name
   * lookups in Java 7.
   */
  T create(
      Elements elements,
      Types types,
      TypeElement element,
      Messager messager,
      EnumSet<RunMode> runMode,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      @Nullable InterStageStore propNameInterStageStore);
}
