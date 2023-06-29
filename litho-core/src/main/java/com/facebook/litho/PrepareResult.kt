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

package com.facebook.litho

import com.facebook.rendercore.Mountable
import com.facebook.rendercore.primitives.Primitive
import kotlin.jvm.JvmField

/**
 * The result of a [MountableComponent#prepare] or a [PrimitiveComponent#prepare] call. This will be
 * the Mountable/Primitive this component rendered to, potentially as well as other
 * non-Mountable/Primitive metadata that resulted from that call, such as transitions that should be
 * applied, as well as CommonProps for the component.
 */
class PrepareResult {

  @JvmField val primitive: Primitive?

  @JvmField val mountable: Mountable<*>?

  @JvmField val transitions: List<Transition>?

  @JvmField val useEffectEntries: List<Attachable>?

  @JvmField val commonProps: CommonProps?

  constructor(
      mountable: Mountable<*>?,
      transitions: List<Transition>?,
      useEffectEntries: List<Attachable>?,
      commonProps: CommonProps?
  ) {
    primitive = null
    this.mountable = mountable
    this.transitions = transitions
    this.useEffectEntries = useEffectEntries
    this.commonProps = commonProps
  }

  constructor(
      primitive: Primitive?,
      transitions: List<Transition>?,
      useEffectEntries: List<Attachable>?,
      commonProps: CommonProps?
  ) {
    this.primitive = primitive
    mountable = null
    this.transitions = transitions
    this.useEffectEntries = useEffectEntries
    this.commonProps = commonProps
  }
}
