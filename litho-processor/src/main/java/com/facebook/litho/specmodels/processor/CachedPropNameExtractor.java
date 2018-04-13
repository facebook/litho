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
