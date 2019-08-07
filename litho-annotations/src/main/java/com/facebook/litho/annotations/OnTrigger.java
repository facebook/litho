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
 * Annotated function in the component will allow its parents to call it with an EventTrigger. See
 * <a href="https://fblitho.com/docs/trigger-events">trigger-events</a> for details.
 *
 * <p>For example
 *
 * <pre>
 * <code>
 *  {@literal @}LayoutSpec
 *   public class ComponentSpec {
 *
 *    {@literal @}OnTrigger(YourEvent.class)
 *     static Object yourEventClick(ComponentContext c,{@literal @}FromTrigger YourObject obj) {
 *       return new Object();
 *     }
 *   }
 * </code>
 * </pre>
 *
 * @see OnEvent
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnTrigger {
  Class<?> value();
}
