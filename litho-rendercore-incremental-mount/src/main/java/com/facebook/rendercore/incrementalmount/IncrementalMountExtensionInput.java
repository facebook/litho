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

package com.facebook.rendercore.incrementalmount;

import com.facebook.rendercore.RenderUnit;
import java.util.List;
import javax.annotation.Nullable;

public interface IncrementalMountExtensionInput {

  /** Returns the position of {@link RenderUnit} given its id. */
  int getPositionForId(long id);

  @Nullable
  List<IncrementalMountOutput> getOutputsOrderedByTopBounds();

  @Nullable
  List<IncrementalMountOutput> getOutputsOrderedByBottomBounds();

  IncrementalMountOutput getIncrementalMountOutputAt(int position);

  int getIncrementalMountOutputCount();

  boolean renderUnitWithIdHostsRenderTrees(long id);
}
