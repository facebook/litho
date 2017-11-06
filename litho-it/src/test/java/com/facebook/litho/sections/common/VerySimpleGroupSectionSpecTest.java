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

import com.facebook.litho.ClickEvent;
import com.facebook.litho.sections.Section;
import com.facebook.litho.testing.sections.SectionsTestHelper;
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

  private SectionsTestHelper mTester;

  @Before
  public void setup() throws Exception {
    mTester = new SectionsTestHelper(RuntimeEnvironment.application);
  }

  @Test
  public void testInitialChildren() throws Exception {

    Section s = mTester.prepare(
        VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    List<SubSection> subSections = mTester.getChildren(s);

    assertThat(subSections)
        .containsExactly(
                SubSection.of(
                    SingleComponentSection.create(mTester.getContext())
                        .key("key0")
                        .component(Text.create(mTester.getContext()).text("Lol hi 0"))
                        .build()),
                SubSection.of(SingleComponentSection.class),
                SubSection.of(SingleComponentSection.class),
                SubSection.of(SingleComponentSection.class));
  }

  @Test
  public void testStateUpdate() throws Exception {

    Section s = mTester.prepare(
        VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    assertThat(mTester.getChildren(s).size()).isEqualTo(4);

    VerySimpleGroupSection.onUpdateState(mTester.getScopedContext(s), 5);

    assertThat(mTester.getChildren(s).size()).isGreaterThan(4);
  }

  @Test
  public void testDataBound() throws Exception {
    VerySimpleGroupSection s =
        (VerySimpleGroupSection) mTester.prepare(
            VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    s.dataBound(mTester.getScopedContext(s), s);

    VerySimpleGroupSection.VerySimpleGroupSectionStateContainer stateContainer =
        mTester.getStateContainer(s);

    assertThat(stateContainer.extra).isEqualTo(-4);
  }

  @Test
  public void testClickHandler() throws Exception {
    Section s =
        mTester.prepare(
            VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    SectionsTestHelper.dispatchEvent(
        s, VerySimpleGroupSection.onImageClick(mTester.getScopedContext(s)), new ClickEvent());

    VerySimpleGroupSection.VerySimpleGroupSectionStateContainer stateContainer =
        mTester.getStateContainer(s);

    assertThat(stateContainer.extra).isEqualTo(3);
  }
}
