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
 * {@code FromPrepare} is used to pass objects from the {@link OnPrepare} lifecycle methods of
 * {@link MountSpec} to lifecycle methods called successively such as {@link OnMount} or {@link
 * OnBind} method, use {@code FromPrepare} with the same type and name to retrieve your previously
 * set
 *
 * <p>To use it, simply declare a parameter of type {@code com.facebook.litho.Output<>} within the
 * method annotated with {@link OnPrepare}. {@code com.facebook.litho.Output<>} accepts a generic
 * type which should be the type of the object you want to pass around. Then, in a successive
 * lifecycle method, use {@code FromPrepare} with the same type and name to retrieve your previously
 * set object.
 *
 * <p><em><b>This can be used in:</b></em>
 *
 * <ul>
 *   <li>{@link OnMeasure}
 *   <li>{@link OnMeasureBaseline}
 *   <li>{@link OnBoundsDefined}
 *   <li>{@link OnMount}
 *   <li>{@link OnBind}
 *   <li>{@link OnUnbind}
 *   <li>{@link OnUnmount}
 *   <li>{@link OnPopulateAccessibilityNode}
 *   <li>{@link OnPopulateExtraAccessibilityNode}
 *   <li>{@link GetExtraAccessibilityNodeAt}
 *   <li>{@link GetExtraAccessibilityNodesCount}
 * </ul>
 *
 * <p>Example:<br>
 *
 * <pre><code>{@literal @MountSpec}
 * public class MyComponentSpec {
 *
 *  {@literal @}OnCreateMountContent
 *   MyDrawable onCreateMountContent(Context context) {
 *     return new MyDrawable(c);
 *   }
 *
 *
 *  {@literal @}OnPrepare
 *   void onPrepare(
 *       ComponentContext c,
 *       Output&lt;MyFromPrepareObject&gt; fromPrepareObject) {
 *     MyFromPrepareObject myFromPrepareObject = new MyFromPrepareObject();
 *     fromPrepareObject.set(myFromPrepareObject);
 *   }
 *
 *  {@literal @}OnMount
 *   void onMount(
 *       ComponentContext c,
 *       MyDrawable myDrawable,
 *       {@literal @}FromPrepare MyFromPrepareObject fromPrepareObject) {
 *     fromPrepareObject.doSomething();
 *   }
 *
 * }
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FromPrepare {}
