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
import static com.facebook.litho.it.R.attr.testAttrDimen;
import static com.facebook.litho.it.R.attr.testAttrDrawable;
import static com.facebook.litho.it.R.attr.undefinedAttrDimen;
import static com.facebook.litho.it.R.attr.undefinedAttrDrawable;
import static com.facebook.litho.it.R.dimen.default_dimen;
import static com.facebook.litho.it.R.dimen.test_dimen;
import static com.facebook.litho.it.R.dimen.test_dimen_float;
import static com.facebook.litho.it.R.drawable.test_bg;
import static com.facebook.litho.it.R.style.TestTheme;
import static com.facebook.yoga.YogaEdge.LEFT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class ResolveAttributeTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(new ContextThemeWrapper(getApplicationContext(), TestTheme));
  }

  @Test
  public void testResolveDrawableAttribute() {
    Column column = Column.create(mContext).backgroundAttr(testAttrDrawable, 0).build();

    InternalNode node = createAndGetInternalNode(column);

    Drawable d = mContext.getResources().getDrawable(test_bg);
    Drawable drawable = node.getBackground();
    assertThat(shadowOf(drawable).getCreatedFromResId())
        .isEqualTo(shadowOf(d).getCreatedFromResId());
  }

  @Test
  public void testResolveDimenAttribute() {
    Column column = Column.create(mContext).widthAttr(testAttrDimen, default_dimen).build();

    InternalNode node = createAndGetInternalNode(column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertThat((int) node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testDefaultDrawableAttribute() {
    Column column = Column.create(mContext).backgroundAttr(undefinedAttrDrawable, test_bg).build();

    InternalNode node = createAndGetInternalNode(column);

    Drawable d = mContext.getResources().getDrawable(test_bg);
    Drawable drawable = node.getBackground();
    assertThat(shadowOf(drawable).getCreatedFromResId())
        .isEqualTo(shadowOf(d).getCreatedFromResId());
  }

  @Test
  public void testDefaultDimenAttribute() {
    Column column = Column.create(mContext).widthAttr(undefinedAttrDimen, test_dimen).build();

    InternalNode node = createAndGetInternalNode(column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertThat((int) node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenWidthAttribute() {
    Column column = Column.create(mContext).widthAttr(undefinedAttrDimen, test_dimen_float).build();

    InternalNode node = createAndGetInternalNode(column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenPaddingAttribute() {
    Column column =
        Column.create(mContext).paddingAttr(LEFT, undefinedAttrDimen, test_dimen_float).build();

    InternalNode node = createAndGetInternalNode(column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(node.getPaddingLeft()).isEqualTo(dimen);
  }

  private InternalNode createAndGetInternalNode(Component component) {
    final ComponentContext c = new ComponentContext(mContext);
    c.setLayoutStateContextForTesting();

    return Layout.create(c, component);
  }
}
