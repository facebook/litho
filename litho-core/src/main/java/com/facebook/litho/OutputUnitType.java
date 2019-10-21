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

package com.facebook.litho;

import static com.facebook.litho.OutputUnitType.BACKGROUND;
import static com.facebook.litho.OutputUnitType.BORDER;
import static com.facebook.litho.OutputUnitType.CONTENT;
import static com.facebook.litho.OutputUnitType.FOREGROUND;
import static com.facebook.litho.OutputUnitType.HOST;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({CONTENT, BACKGROUND, FOREGROUND, HOST, BORDER})
@Retention(RetentionPolicy.SOURCE)
public @interface OutputUnitType {
  int CONTENT = 0;
  int BACKGROUND = 1;
  int FOREGROUND = 2;
  int HOST = 3;
  int BORDER = 4;
}
