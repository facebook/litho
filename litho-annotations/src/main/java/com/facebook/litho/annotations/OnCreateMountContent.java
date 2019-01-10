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
 * {@link MountSpec}. The onCreateMountContent method can only take a
 * com.facebook.litho.ComponentContext as parameter. No props are allowed here.
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
