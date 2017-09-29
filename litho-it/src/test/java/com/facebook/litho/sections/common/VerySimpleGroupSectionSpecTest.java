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
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.sections.SectionComponentTestHelper;
import com.facebook.litho.testing.sections.SubSection;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link VerySimpleGroupSectionSpec} */
@RunWith(ComponentsTestRunner.class)
public class VerySimpleGroupSectionSpecTest {

  private SectionComponentTestHelper mTester;

  @Before
  public void setup() throws Exception {
    mTester = new SectionComponentTestHelper(RuntimeEnvironment.application);
  }

  @Test
  public void testInitialChildrenWithLightWeightInfra() throws Exception {

    Section s = mTester.prepare(
        VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    List<SubSection> subSections = mTester.getChildren(s);

    assertThat(subSections)
        .isEqualTo(
            ImmutableList.of(
                SubSection.of(
                    SingleComponentSection.create(mTester.getContext())
                        .key("key0")
                        .component(Text.create(mTester.getContext()).text("Lol hi 0"))
                        .build()),
                SubSection.of(SingleComponentSection.class),
                SubSection.of(SingleComponentSection.class),
                SubSection.of(SingleComponentSection.class)));
  }

  @Test
  public void testStateUpdateWithLightWeightInfra() throws Exception {

    Section s = mTester.prepare(
        VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    VerySimpleGroupSection.onUpdateState(mTester.getScopedContext(s), 5);

    assertThat(mTester.getChildren(s).size()).isNotEqualTo(4);
  }
}
