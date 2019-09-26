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
 * An annotation for a Spec method that generates tree props. These tree props will be passed
 * silently to all of the Component's children.
 *
 * <p>Tree props are stored in a map keyed on their individual class object, meaning there will only
 * be one entry for tree props of any given type. PLEASE DO NOT USE COMMON JAVA CLASSES, for
 * example, String, Integer etc; creates a wrapper class instead.
 *
 * <p>Example usage: <code>
 *
 * {@literal @}LayoutSpec
 * public class MySpec {
 *
 *   {@literal @}OnCreateTreeProp
 *   SomeTreePropClass onCreateSomeTreeProp(
 *     ComponentsContext c,
 *     {@literal @}Prop SomeProp prop) {
 *    return new SomeTreePropClass(prop.getSomeProperty());
 *   }
 * }
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnCreateTreeProp {}
