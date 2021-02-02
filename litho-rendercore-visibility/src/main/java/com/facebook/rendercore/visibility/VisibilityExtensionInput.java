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

package com.facebook.rendercore.visibility;

import com.facebook.rendercore.RenderTreeHost;
import com.facebook.rendercore.RenderUnit;
import java.util.List;
import java.util.Set;

/** This APIs declares that inputs required by the Visibility Extensions. */
public interface VisibilityExtensionInput {

  /** returns a list of items for which visibility events will be processed */
  List<VisibilityOutput> getVisibilityOutputs();

  /** Returns the set of ids of {@link RenderUnit}s which host {@link RenderTreeHost} */
  Set<Long> getRenderUnitIdsWhichHostRenderTrees();
}
