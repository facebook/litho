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

import com.facebook.litho.ClickEvent;
import com.facebook.litho.sections.Section;
import com.facebook.litho.testing.sections.SectionsTestHelper;
import com.facebook.litho.testing.sections.SubSection;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Text;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link VerySimpleGroupSectionSpec} */
@RunWith(LithoTestRunner.class)
public class VerySimpleGroupSectionSpecTest {

  private SectionsTestHelper mTester;

  @Before
  public void setup() throws Exception {
    mTester = new SectionsTestHelper(getApplicationContext());
  }

  @Test
  public void testInitialChildren() throws Exception {

    Section s =
        mTester.prepare(
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

    Section s =
        mTester.prepare(
            VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    assertThat(mTester.getChildren(s).size()).isEqualTo(4);

    VerySimpleGroupSection.onUpdateStateSync(mTester.getScopedContext(s), 5);

    assertThat(mTester.getChildren(s).size()).isGreaterThan(4);
  }

  @Test
  public void testDataBound() throws Exception {
    VerySimpleGroupSection s =
        (VerySimpleGroupSection)
            mTester.prepare(
                VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    s.dataBound(mTester.getScopedContext(s));

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

  @Test
  public void testLogTag() {
    Section s =
        mTester.prepare(
            VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());
    assertThat(s.getLogTag()).isEqualTo(s.getClass().getSimpleName());
  }
}
