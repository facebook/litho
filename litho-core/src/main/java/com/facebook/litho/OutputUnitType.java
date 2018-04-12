/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import static com.facebook.litho.OutputUnitType.BACKGROUND;
import static com.facebook.litho.OutputUnitType.BORDER;
import static com.facebook.litho.OutputUnitType.CONTENT;
import static com.facebook.litho.OutputUnitType.FOREGROUND;
import static com.facebook.litho.OutputUnitType.HOST;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({CONTENT, BACKGROUND, FOREGROUND, HOST, BORDER})
@Retention(RetentionPolicy.SOURCE)
@interface OutputUnitType {
  int CONTENT = 0;
  int BACKGROUND = 1;
  int FOREGROUND = 2;
  int HOST = 3;
  int BORDER = 4;
}
