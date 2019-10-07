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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <strong>EXPERIMENTAL</strong>. This is a part of new experimental API. There is no guarantee that
 * this API will work or that it will remain the same.
 *
 * <p>A method annotation used in {@link MountSpec} classes to indicate methods that apply current
 * value of dynamic prop {@link Prop} to the mounted content.
 *
 * <p>For every dynamic {@link Prop}, ComponentSpec should provide exactly one {@link
 * OnBindDynamicValue} method.
 *
 * <p>Every {@link OnBindDynamicValue} takes two parameters: 1st - mounted content object; 2nd -
 * current value of the dynamic {@link Prop}, which needs to be applied to the mount content.
 *
 * <p>The method should return void.
 *
 * <p>This callback will be invoked when a Component is being bound and after that, every time when
 * value of dynamic {@link Prop} changes until the Component is unbound.
 *
 * @see Prop#dynamic()
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnBindDynamicValue {}
