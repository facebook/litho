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
import java.util.Optional;
import javax.lang.model.element.Name;

final class CachedPropNameExtractor {

  private CachedPropNameExtractor() {}

  /**
   * Extracts prop names for the Name of the spec model under construction
   *
   * @param interStageStore The store constructed with the current round env.
   * @param qualifiedName The name of the spec being constructed. Note: This is NOT the name of the
   *     generated component.
   * @return A list of cached properties. May be empty in case of failure or when no properties are
   *     available.
   */
  static ImmutableList<String> getCachedPropNames(
      InterStageStore interStageStore, Name qualifiedName) {
    final Optional<ImmutableList<String>> strings =
        interStageStore.getPropNameInterStageStore().loadNames(qualifiedName);
    // TODO(T23487428): Use the processing env Messager to report this.
    return strings.orElseGet(ImmutableList::of);
  }
}
