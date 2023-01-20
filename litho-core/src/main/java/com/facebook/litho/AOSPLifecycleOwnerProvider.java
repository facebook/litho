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

import androidx.lifecycle.LifecycleOwner;
import javax.annotation.Nullable;

/**
 * This abstraction allows us to retrieve a {@link LifecycleOwner}. This class is meant to be used
 * to identify the closest {@link LifecycleOwner} and associate it to the {@link ComponentTree}.
 *
 * <p>If there is no knowledge about the current {@link LifecycleOwner} it will return {@code null}
 */
public interface AOSPLifecycleOwnerProvider {

  @Nullable
  LifecycleOwner getLifecycleOwner();
}
