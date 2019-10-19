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
 * A method annotation used in classes that are annotated with {@link MountSpec}.
 *
 * <p>Methods annotated with {@link OnBind} take an Android Context as the first parameter, the
 * Object that the MountSpec mounts as the second parameter, followed by any number of {@link
 * Prop}s.
 *
 * <p>The method should return void. This callback will be invoked every time the mounted object is
 * about to become active after being mounted. This method can be called multiple times after
 * onMount with the same mounted content.
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnBind {}
