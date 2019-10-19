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

package com.facebook.litho.intellij;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import org.junit.Assert;
import org.junit.Test;

public class LithoClassNamesTest {

  @Test
  public void names() {
    Assert.assertEquals(ClickEvent.class.getName(), LithoClassNames.CLICK_EVENT_CLASS_NAME);
    Assert.assertEquals(
        ComponentContext.class.getName(), LithoClassNames.COMPONENT_CONTEXT_CLASS_NAME);
    Assert.assertEquals(Section.class.getName(), LithoClassNames.SECTION_CLASS_NAME);
    Assert.assertEquals(SectionContext.class.getName(), LithoClassNames.SECTION_CONTEXT_CLASS_NAME);
  }

  @Test
  public void shortName() {
    Assert.assertEquals("Any", LithoClassNames.shortName("some.Any"));
    Assert.assertEquals("short", LithoClassNames.shortName("short"));
    Assert.assertEquals("", LithoClassNames.shortName(""));
    Assert.assertEquals("123", LithoClassNames.shortName(".123"));
  }
}
