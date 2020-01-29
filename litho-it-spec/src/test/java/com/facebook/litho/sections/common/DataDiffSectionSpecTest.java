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

package com.facebook.litho.sections.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class DataDiffSectionSpecTest {

  private ComponentContext mComponentContext;

  @Before
  public void setup() {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testDiffSectionCrashesIfOnRenderEventReturnsNull() {
    final RecyclerCollectionComponent component =
        RecyclerCollectionComponent.create(mComponentContext)
            .section(
                GroupSectionWithNullableRenderInfo.create(new SectionContext(mComponentContext))
                    /* "" should result with a `null` RenderInfo in GroupSectionWithNullableRenderInfoSpec */
                    .items(Lists.newArrayList("one", "two", "")))
            .build();

    try {
      ComponentTestHelper.mountComponent(mComponentContext, component);
      fail("Should crash when creating 'RenderInfo' for an empty string");
    } catch (RuntimeException e) {
      assertThat(e.getCause())
          .hasMessageEndingWith("'@OnEvent(RenderEvent.class)' is not allowed to return 'null'.");
    }
  }
}
