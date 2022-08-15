/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho;

import static com.facebook.litho.ComponentTree.INVALID_LAYOUT_VERSION;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;

/** Temporary LayoutStateContext to be used during Component.measure when not caching results. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class TemporaryLayoutStateContext extends LayoutStateContext {
  public TemporaryLayoutStateContext(
      TreeState treeState,
      ComponentContext componentContext,
      @Nullable ComponentTree componentTree) {
    super(
        new TempLayoutProcessInfo(),
        new TempIdGenerator(),
        componentContext,
        treeState,
        componentTree,
        null,
        null,
        INVALID_LAYOUT_VERSION);
  }

  private static class TempIdGenerator implements LayoutOutputIdGenerator {
    @Override
    public long calculateLayoutOutputId(String componentKey, @OutputUnitType int type) {
      return 0;
    }
  }

  private static class TempLayoutProcessInfo implements LayoutProcessInfo {
    @Override
    public boolean isCreateLayoutInProgress() {
      return true;
    }
  }
}
