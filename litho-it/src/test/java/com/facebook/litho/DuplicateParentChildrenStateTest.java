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

package com.facebook.litho;

import static android.graphics.Color.BLUE;
import static android.graphics.Color.RED;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.LayoutOutput.isDuplicateParentState;
import static com.facebook.litho.LayoutState.calculate;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class DuplicateParentChildrenStateTest {

  private int mUnspecifiedSizeSpec;

  @Before
  public void setUp() throws Exception {
    mUnspecifiedSizeSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
  }

  @Test
  public void duplicateParentState_avoidedIfRedundant() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .duplicateParentState(true)
                .clickHandler(c.newEventHandler(1))
                .child(
                    create(c)
                        .duplicateParentState(false)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    create(c)
                        .duplicateParentState(true)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    create(c)
                        .clickHandler(c.newEventHandler(2))
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    create(c)
                        .clickHandler(c.newEventHandler(3))
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(false)))
                .child(
                    create(c)
                        .clickHandler(c.newEventHandler(3))
                        .backgroundColor(RED)
                        .foregroundColor(RED))
                .child(create(c).backgroundColor(BLUE).foregroundColor(BLUE))
                .build();
          }
        };

    LayoutState layoutState =
        calculate(
            new ComponentContext(getApplicationContext()),
            component,
            -1,
            mUnspecifiedSizeSpec,
            mUnspecifiedSizeSpec,
            LayoutState.CalculateLayoutSource.TEST);

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(12);

    assertTrue(
        "Clickable root output has duplicate state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(0)).getFlags()));

    assertFalse(
        "Parent doesn't duplicate host state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(1)).getFlags()));

    assertTrue(
        "Parent does duplicate host state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(2)).getFlags()));

    assertTrue(
        "Drawable duplicates clickable parent state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(4)).getFlags()));

    assertFalse(
        "Drawable doesn't duplicate clickable parent state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(6)).getFlags()));

    assertTrue(
        "Background should duplicate clickable node state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(8)).getFlags()));
    assertTrue(
        "Foreground should duplicate clickable node state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(9)).getFlags()));

    assertFalse(
        "Background should duplicate non-clickable node state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(10)).getFlags()));
    assertFalse(
        "Foreground should duplicate non-clickable node state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(11)).getFlags()));
  }

  @Test
  public void duplicateChildrenStates_passedToView() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Row.create(c)
                .child(
                    Row.create(c)
                        .duplicateChildrenStates(true)
                        .child(SimpleMountSpecTester.create(c).focusable(true))
                        .child(SimpleMountSpecTester.create(c).clickable(true)))
                .build();
          }
        };

    LithoView lv =
        ComponentTestHelper.mountComponent(
            new ComponentContext(getApplicationContext()), component);

    final Object secondMountedItem = lv.getMountDelegateTarget().getMountItemAt(1).getContent();
    assertTrue(secondMountedItem instanceof ComponentHost);
    assertTrue(((ComponentHost) secondMountedItem).addStatesFromChildren());
  }
}
