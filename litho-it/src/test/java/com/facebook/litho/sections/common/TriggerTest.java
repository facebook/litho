/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.common;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.sections.TestTriggerChildSection;
import com.facebook.litho.testing.sections.TestTriggerParentSection;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class TriggerTest {

  private SectionContext mSectionContext;
  private SectionTree mSectionTree;
  private TestTarget mTestTarget;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    mSectionContext = new SectionContext(RuntimeEnvironment.application);
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
