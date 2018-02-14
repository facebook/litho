/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.internal.RunMode;
import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;

/**
 * A factory that chooses whether or not to create a {@link DependencyInjectionHelper} based on the
 * {@link TypeElement} of the class it is generated for.
 */
public interface DependencyInjectionHelperFactory {
  @Nullable
  DependencyInjectionHelper create(TypeElement typeElement, RunMode runMode);
}
