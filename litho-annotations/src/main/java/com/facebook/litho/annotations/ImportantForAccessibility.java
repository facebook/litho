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

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({
  ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
  ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES,
  ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO,
  ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
  ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS,
})
@Retention(RetentionPolicy.SOURCE)
public @interface ImportantForAccessibility {
  /** Automatically determine whether a view is important for accessibility. */
  int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0x00000000;

  /** The view is important for accessibility. */
  int IMPORTANT_FOR_ACCESSIBILITY_YES = 0x00000001;

  /** The view is not important for accessibility. */
  int IMPORTANT_FOR_ACCESSIBILITY_NO = 0x00000002;

  /** The view is not important for accessibility, nor are any of its descendant views. */
  int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 0x00000004;

  /** The view is important for accessibility, but none of its descendant views are. */
  int IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS = 0x00000008;
}
