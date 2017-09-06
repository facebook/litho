/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.specmodels.model;

import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecModel;
import javax.annotation.Nullable;

/** An interface for {@link SpecModel}s that have a service. */
public interface HasService {

  @Nullable
  MethodParamModel getServiceParam();
}
