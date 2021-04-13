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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.it.R.attr.testAttrLargePadding;
import static com.facebook.litho.it.R.attr.testAttrLargeText;
import static com.facebook.litho.it.R.style.PaddingStyle;
import static com.facebook.litho.it.R.style.TestTheme;
import static com.facebook.litho.it.R.style.TextSizeStyle;
import static com.facebook.litho.testing.Whitebox.getInternalState;
import static com.facebook.yoga.YogaConstants.UNDEFINED;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.ContextThemeWrapper;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaConstants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
@org.junit.Ignore("t16280359")
public class ComponentStyleTest {
  private int mDimen;
  private int mLargeDimen;
  private ComponentContext mContext;

  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  @Before
  public void setup() {
    mContext = new ComponentContext(new ContextThemeWrapper(getApplicationContext(), TestTheme));
    mDimen = mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    mLargeDimen = mContext.getResources().getDimensionPixelSize(R.dimen.test_large_dimen);
  }

  @Test
  public void testStyleProp() {
    Component component = Text.create(mContext, 0, TextSizeStyle).text("text").build();
    assertThat((int) getInternalState(component, "textSize")).isEqualTo(mDimen);
  }

  @Test
  public void testOverrideStyleProp() {
    Component component =
        Text.create(mContext, 0, TextSizeStyle).text("text").textSizePx(2 * mDimen).build();
    assertThat((int) getInternalState(component, "textSize")).isEqualTo(2 * mDimen);
  }

  @Test
  public void testStyleLayout() {
    Component component = Text.create(mContext, 0, PaddingStyle).text("text").build();
    InternalNode node = (InternalNode) component.resolve(mContext);
    LithoLayoutResult result = node.calculateLayout(UNDEFINED, UNDEFINED);
    assertThat(result.getYogaNode().getPadding(LEFT)).isEqualTo(mDimen);
  }

  @Test
  public void testOverrideStyleLayout() {
    Component component =
        Text.create(mContext, 0, PaddingStyle).text("text").paddingPx(ALL, mDimen * 2).build();
    InternalNode node = (InternalNode) component.resolve(mContext);
    LithoLayoutResult result = node.calculateLayout(UNDEFINED, UNDEFINED);
    assertThat(result.getYogaNode().getPadding(LEFT)).isEqualTo(2 * mDimen);
  }

  @Test
  public void testAttributeStyleProp() {
    Component component = Text.create(mContext, testAttrLargeText, 0).text("text").build();
    assertThat((int) getInternalState(component, "textSize")).isEqualTo(mLargeDimen);
  }

  @Test
  public void testOverrideAttributeStyleProp() {
    Component component =
        Text.create(mContext, testAttrLargeText, 0).text("text").textSizePx(mDimen).build();
    assertThat((int) getInternalState(component, "textSize")).isEqualTo(mDimen);
  }

  @Test
  public void testAttributeStyleLayout() {
    Component component = Text.create(mContext, testAttrLargePadding, 0).text("text").build();
    InternalNode node = (InternalNode) component.resolve(mContext);
    LithoLayoutResult result = node.calculateLayout(UNDEFINED, UNDEFINED);
    assertThat(result.getYogaNode().getPadding(LEFT)).isEqualTo(mLargeDimen);
  }

  @Test
  public void testOverrideAttributeStyleLayout() {
    Component component =
        Text.create(mContext, testAttrLargePadding, 0)
            .text("text")
            .paddingPx(ALL, mDimen * 2)
            .build();
    InternalNode node = (InternalNode) component.resolve(mContext);
    LithoLayoutResult result = node.calculateLayout(UNDEFINED, UNDEFINED);
    assertThat(result.getYogaNode().getPadding(LEFT)).isEqualTo(2 * mDimen);
  }

  @Test
  public void testStyleResOverridenByAttrResForProp() {
    Component component =
        Text.create(mContext, testAttrLargeText, TextSizeStyle).text("text").build();
    assertThat((int) getInternalState(component, "textSize")).isEqualTo(mLargeDimen);
  }

  @Test
  public void testStyleResOverridenByAttrResForLayout() {
    Component component =
        Text.create(mContext, testAttrLargePadding, PaddingStyle).text("text").build();
    InternalNode node = (InternalNode) component.resolve(mContext);
    LithoLayoutResult result = node.calculateLayout(UNDEFINED, UNDEFINED);
    assertThat(result.getYogaNode().getPadding(LEFT)).isEqualTo(mLargeDimen);
  }
}
