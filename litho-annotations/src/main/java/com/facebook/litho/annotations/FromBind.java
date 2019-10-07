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
 * {@code FromBind} is used to pass objects from the {@link OnBind} lifecycle methods of {@link
 * MountSpec} to lifecycle methods called successively such as {@link OnUnbind} or {@link
 * OnUnmount}. method, use {@code FromBind} with the same type and name to retrieve your previously
 * set
 *
 * <p>To use it, simply declare a parameter of type {@code com.facebook.litho.Output<>} within the
 * method annotated with {@link OnBind}. {@code com.facebook.litho.Output<>} accepts a generic type
 * which should be the type of the object you want to pass around. Then, in a successive lifecycle
 * method, use {@code FromBind} with the same type and name to retrieve your previously set object.
 *
 * <p>Example:<br>
 *
 * <pre><code>{@literal @MountSpec}
 * public class MyComponentSpec {
 *
 *  {@literal @}OnCreateMountContent
 *   MyDrawable onCreateMountContent(Context c) {
 *     return new MyDrawable(c);
 *   }
 *
 *  {@literal @}OnMount
 *   void onMount(
 *       ComponentContext context,
 *       MyDrawable myDrawable,
 *       {@literal @}Prop MyProp prop) {
 *     myDrawable.setMyProp(prop);
 *   }
 *
 *  {@literal @}OnBind
 *   void onBind(
 *       ComponentContext context,
 *       MyDrawable myDrawable,
 *       Output&lt;MyFromBindObject&gt; fromBindObject) {
 *     MyFromBindObject myFromBindObject = new MyFromBindObject();
 *     fromBindObject.set(myFromBindObject);
 *   }
 *
 *  {@literal @}OnUnbind
 *   void onUnbind(
 *       ComponentContext context,
 *       MyDrawable myDrawable,
 *       {@literal @}FromBind MyFromBindObject fromBindObject) {
 *     fromBindObject.doSomething();
 *   }
 * }
 * </code></pre>
 */
@Retention(RetentionPolicy.SOURCE)
public @interface FromBind {}
