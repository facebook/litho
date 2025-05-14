/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore.text;

import com.facebook.infer.annotation.Nullsafe;

/** Enumeration of text truncation style values. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public enum TruncationStyle {

  /**
   * Truncate the text such that the truncation will always appear on the last line of text.
   * Truncation will never appear on its own line or next to emojis or periods. eg: "This is a very
   * long line of text that will be truncated... See More" rather than "This is a very long line of
   * text that will be truncated at the end ... See More"
   */
  FORCE_INLINE_TRUNCATION,

  /**
   * Truncate the text such that the truncation uses the maximum space available. This allows
   * truncation to appear on its own line or next to emojis or periods. This is the default
   * behavior.
   */
  USE_MAX_LINES,
}
