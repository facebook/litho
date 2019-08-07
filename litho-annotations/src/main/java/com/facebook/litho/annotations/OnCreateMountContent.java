/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
 * The method annotated with this annotation will be called to instantiate the mount content for the
 * {@link MountSpec}, when the pool of objects corresponding to this mount content type is empty and
 * recycling is not possible. The mount content can be either a View or a Drawable.
 *
 * <p>You can think of methods annotated with {@code OnCreateMountContent} as the equivalent of the
 * RecyclerView.Adapter's createViewHolder method. If the pool for this mount content is not empty
 * and the framework can recycle mount content objects, this method will not be invoked.
 *
 * <p>The annotated method will return the created mount content, either a View or a Drawable type,
 * and receives the following arguments from the framework:
 *
 * <p><em>Required:</em><br>
 *
 * <ol>
 *   <li>ComponentContext
 * </ol>
 *
 * No other arguments, such as {@link Prop}, {@link State} or any inter-stage props are allowed in
 * this method.
 *
 * <p>The OnCreateMountContent method cannot provide inter-stage props.
 *
 * @see OnCreateMountContentPool
 * @see OnMount
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnCreateMountContent {

  /**
   * The type of class used for the mount content. During normal compilation, it should never be
   * necessary to specify this explicitly. However, projects that use source-only ABI generation may
   * need to if the mounting type cannot be inferred from the return type.
   */
  MountingType mountingType() default MountingType.INFER;
}
