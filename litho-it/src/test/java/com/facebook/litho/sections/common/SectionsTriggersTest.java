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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.sections.TestTriggerChildSection;
import com.facebook.litho.testing.sections.TestTriggerParentSection;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(LithoTestRunner.class)
public class SectionsTriggersTest {

  private SectionContext mSectionContext;
  private SectionTree mSectionTree;
  private TestTarget mTestTarget;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    mSectionContext = new SectionContext(getApplicationContext());
    mTestTarget = new TestTarget();
    mSectionTree = SectionTree.create(mSectionContext, mTestTarget).build();
  }

  @Test
  public void testTriggerEvent() {
    final String childKey = "childKey";
    final Section section =
        TestTriggerParentSection.create(mSectionContext).childKey(childKey).build();

    mSectionContext = SectionContext.withSectionTree(mSectionContext, mSectionTree);
    mSectionContext = SectionContext.withScope(mSectionContext, section);
    section.setScopedContext(mSectionContext);

    final String parentKey = TestTriggerParentSection.class.getSimpleName();
    section.setGlobalKey(parentKey);

    mSectionTree.setRoot(section);

    final String prefix = "TestTrigger";
    final String actual = TestTriggerChildSection.onTestTrigger(mSectionContext, childKey, prefix);

    assertThat(actual).isEqualTo(prefix + parentKey + childKey);
  }
}
