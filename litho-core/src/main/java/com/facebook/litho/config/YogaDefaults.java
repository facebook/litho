/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.config;

import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;

public class YogaDefaults {
  public static final YogaDirection DIRECTION = YogaDirection.INHERIT;
  public static final YogaFlexDirection FLEX_DIRECTION = YogaFlexDirection.ROW;
  public static final YogaJustify JUSTIFY_CONTENT = YogaJustify.FLEX_START;
  public static final YogaAlign ALIGN_CONTENT = YogaAlign.FLEX_START;
  public static final YogaAlign ALIGN_ITEM = YogaAlign.STRETCH;
  public static final YogaAlign ALIGN_SELF = YogaAlign.AUTO;
  public static final YogaPositionType POSITION_TYPE = YogaPositionType.RELATIVE;
  public static final YogaWrap FLEX_WRAP = YogaWrap.NO_WRAP;
}
