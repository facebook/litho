/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.sections;

import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.DiffSectionSpec;

@DiffSectionSpec
public class TestTriggerChildSectionSpec {

  @OnTrigger(TestTriggerEvent.class)
  protected static String onTestTrigger(SectionContext c, @FromTrigger String prefix) {
    return prefix + c.getSectionScope().getGlobalKey();
  }
}
