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

package com.facebook.litho;

import com.facebook.proguard.annotations.DoNotStrip;
import java.util.Collections;
import java.util.List;

/** A UI element that contains text. */
@DoNotStrip
public interface TextContent {

  /** An empty instance of {@link TextContent}. */
  TextContent EMPTY =
      new TextContent() {
        @Override
        public List<CharSequence> getTextItems() {
          return Collections.EMPTY_LIST;
        }
      };

  /**
   * @return the list of text items that are rendered by this UI element. The list returned should
   *     not be modified and may be unmodifiable.
   */
  @DoNotStrip
  List<CharSequence> getTextItems();
}
