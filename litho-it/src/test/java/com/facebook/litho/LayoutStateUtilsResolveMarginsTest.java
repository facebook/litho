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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutStateUtilsResolveMarginsTest {

  private YogaNode node;

  @Before
  public void setup() {
    node = mock(YogaNode.class);
    when(node.getMargin(any(YogaEdge.class))).thenReturn(YogaValue.parse("undefined"));
  }

  @Test
  public void testPrecidenceRespected() {
    when(node.getMargin(YogaEdge.ALL)).thenReturn(YogaValue.parse("2"));
    Rect marginRect = LayoutStateUtils.resolveMargins(node, false);
    assertThat(marginRect).isEqualTo(new Rect(2, 2, 2, 2));

    when(node.getMargin(YogaEdge.HORIZONTAL)).thenReturn(YogaValue.parse("3"));
    when(node.getMargin(YogaEdge.VERTICAL)).thenReturn(YogaValue.parse("4"));

    marginRect = LayoutStateUtils.resolveMargins(node, false);
    assertThat(marginRect).isEqualTo(new Rect(3, 4, 3, 4));

    when(node.getMargin(YogaEdge.START)).thenReturn(YogaValue.parse("5"));
    when(node.getMargin(YogaEdge.END)).thenReturn(YogaValue.parse("6"));

    marginRect = LayoutStateUtils.resolveMargins(node, false);
    assertThat(marginRect).isEqualTo(new Rect(5, 4, 6, 4));

    when(node.getMargin(YogaEdge.LEFT)).thenReturn(YogaValue.parse("7"));
    when(node.getMargin(YogaEdge.RIGHT)).thenReturn(YogaValue.parse("8"));

    marginRect = LayoutStateUtils.resolveMargins(node, false);
    assertThat(marginRect).isEqualTo(new Rect(7, 4, 8, 4));
  }

  @Test
  public void testRightToLeftLayout() {
    when(node.getMargin(YogaEdge.START)).thenReturn(YogaValue.parse("5"));
    when(node.getMargin(YogaEdge.END)).thenReturn(YogaValue.parse("6"));

    Rect marginRect = LayoutStateUtils.resolveMargins(node, false);
    assertThat(marginRect).isEqualTo(new Rect(5, 0, 6, 0));

    marginRect = LayoutStateUtils.resolveMargins(node, true);
    assertThat(marginRect).isEqualTo(new Rect(6, 0, 5, 0));
  }
}
