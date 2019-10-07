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
