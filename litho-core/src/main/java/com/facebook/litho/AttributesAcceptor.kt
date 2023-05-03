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

/**
 * Defines an abstraction that knows how to store attributes.
 *
 * This is used so that the clients can only set attributes and not read them.
 *
 * Reading attributes is not allowed for the clients, as only the testing APIs should use it.
 */
interface AttributesAcceptor {
  fun <T> setDebugAttributeKey(attributeKey: AttributeKey<T>, value: T)
}
