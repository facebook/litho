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

package com.facebook.litho;

import com.facebook.infer.annotation.ThreadSafe;
import javax.annotation.Nullable;

/**
 * A class implementing this interface wll expose a method annotated with {@link
 * com.facebook.litho.annotations.OnTrigger} to accept an {@link
 * com.facebook.litho.annotations.Event} given an {@link EventTrigger}
 */
@ThreadSafe
public interface EventTriggerTarget {
  @Nullable
  Object acceptTriggerEvent(EventTrigger eventTrigger, Object eventState, Object[] params);
}
