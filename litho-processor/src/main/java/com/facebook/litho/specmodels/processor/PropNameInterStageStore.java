/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.SpecModel;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Name;

/** This will serve as store for parameter names. TODO(T21953762) */
public class PropNameInterStageStore {
  private final Filer mFiler;

  public PropNameInterStageStore(Filer filer) {
    this.mFiler = filer;
  }

  public Optional<ImmutableList<String>> loadNames(Name qualifiedName) {
    return Optional.empty();
  }

  /** Saves the prop names of the given spec model at a well-known path within the resources. */
  public void saveNames(SpecModel specModel) throws IOException {}
}
