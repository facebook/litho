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
import static com.facebook.litho.it.R.dimen.test_dimen;
import static com.facebook.litho.it.R.dimen.test_dimen_float;
import static com.facebook.litho.it.R.style.TestTheme;
import static com.facebook.yoga.YogaEdge.LEFT;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.ContextThemeWrapper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class ResolveResTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(new ContextThemeWrapper(getApplicationContext(), TestTheme));
  }

  @Test
  public void testDefaultDimenWidthRes() {
    Column column = Column.create(mContext).widthRes(test_dimen).build();

    InternalNode node = createAndGetInternalNode(column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(test_dimen);
    assertThat(node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testDefaultDimenPaddingRes() {
    Column column = Column.create(mContext).paddingRes(LEFT, test_dimen).build();

    InternalNode node = createAndGetInternalNode(column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(test_dimen);
    assertThat(node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenWidthRes() {
    Column column = Column.create(mContext).widthRes(test_dimen_float).build();

    InternalNode node = createAndGetInternalNode(column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenPaddingRes() {
    Column column = Column.create(mContext).paddingRes(LEFT, test_dimen_float).build();

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
