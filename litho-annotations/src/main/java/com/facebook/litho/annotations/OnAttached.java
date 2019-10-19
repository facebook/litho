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

package com.facebook.litho.annotations;

/**
 * A method annotated with this annotation is called when the component is attached to the
 * ComponentTree.
 *
 * <p>In a component spec, you can acquire resources in {@literal @}OnAttached method, and release
 * them in {@literal @}OnDetached method.
 *
 * <p>For example: <code>
 *
 * {@literal @}OnAttached
 * void onAttached(
 *     ComponentContext c,
 *     {@literal @}Prop final DataSource dataSource,
 *     {@literal @}State final SourceListener sourceListener) {
 *   dataSource.subscribe(sourceListener);
 * }
 * </code>
 */
public @interface OnAttached {}
