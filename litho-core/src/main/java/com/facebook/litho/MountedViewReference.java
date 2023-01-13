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

import android.view.View;
import androidx.annotation.Nullable;

/**
 * An interfaced used by ComponentContext to access the root view on which a hierarchy is mounted.
 * This interface needs to be passed in the ComponentContext when it's built.
 */
public interface MountedViewReference {

  @Nullable
  View getMountedView();
}
