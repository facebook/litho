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

import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class DuplicateParentChildrenStateTest {

  private int mUnspecifiedSizeSpec;

  @Before
  public void setUp() throws Exception {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);
    mUnspecifiedSizeSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
  }

  @Test
  public void duplicateParentState_avoidedIfRedundant() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c) // (0 = root host)
                .duplicateParentState(true) // (1 = generated host)
                .clickHandler(c.newEventHandler(1))
                .child(
                    create(c)
                        .duplicateParentState(false) // (2 = simpleMountTester)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    create(c)
                        .duplicateParentState(true) // (3 = simpleMountTester)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    create(c) // (4 = generated host)
                        .clickHandler(c.newEventHandler(2)) // (5 = simpleMountTester)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    create(c) // (6 = generated host)
                        .clickHandler(c.newEventHandler(3)) // (7 = simpleMountTester)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(false)))
                .child(
                    create(c) // (8 = generated host)
                        .clickHandler(c.newEventHandler(3))
                        .backgroundColor(RED)
                        .foregroundColor(RED))
                .child(
                    create(c).backgroundColor(BLUE).foregroundColor(BLUE)) // (9 = generated host)
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

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(10);

    assertFalse(
        "Root output doesn't have duplicate state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(0)).getFlags()));

    assertTrue(
        "Clickable generated root host output has duplicate state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(1)).getFlags()));

    assertFalse(
        "Drawable doesn't duplicate host state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(2)).getFlags()));

    assertTrue(
        "Drawable does duplicate parent state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(3)).getFlags()));

    assertFalse(
        "Drawable host doesn't duplicate clickable parent state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(4)).getFlags()));

    assertTrue(
        "Drawable duplicates parent state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(5)).getFlags()));

    assertFalse(
        "Drawable host doesn't duplicate clickable parent state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(6)).getFlags()));

    assertFalse(
        "Drawable doesn't duplicate clickable parent state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(7)).getFlags()));

    assertFalse(
        "Clickable host doesn't duplicate parent state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(8)).getFlags()));

    assertFalse(
        "Host with bg doesn't duplicate parent state",
        isDuplicateParentState(getLayoutOutput(layoutState.getMountableOutputAt(9)).getFlags()));
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

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
