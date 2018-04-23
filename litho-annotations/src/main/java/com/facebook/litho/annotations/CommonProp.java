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
 * Annotates a parameter to a component's spec method indicating that it will be supplied as a
 * common prop for this component. A common prop is a prop that is available on all components.
 *
 * <p>Such a prop will be used by the framework automatically, but can also be used in specs to do
 * extra things. For example, a widget might want to set a different text color depending upon the
 * color of the background specified by the user.
 */
@Retention(RetentionPolicy.CLASS)
public @interface CommonProp {}
