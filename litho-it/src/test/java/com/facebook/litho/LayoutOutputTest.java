/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static org.assertj.core.api.Assertions.assertThat;

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutOutputTest {

  private static final int LEVEL_TEST = 1;
  private static final int SEQ_TEST = 1;
  private static final int MAX_LEVEL_TEST = 255;
  private static final int MAX_SEQ_TEST = 65535;

  private static class TestComponent extends Component {

    @Override
    public boolean isEquivalentProps(Component other, boolean shouldCompareCommonProps) {
      return this == other;
    }
  }

  private TestComponent mTestComponent;

  @Before
  public void setup() {
    mTestComponent = new TestComponent();
  }

  @Test
  public void testGetMountBoundsNoHostTranslation() {
    Rect mountBounds = new Rect();
    getMountBounds(mountBounds, new Rect(10, 10, 10, 10), 0, 0);
    assertThat(mountBounds).isEqualTo(new Rect(10, 10, 10, 10));
  }

  @Test
  public void testGetMountBoundsWithHostTranslation() {
    Rect mountBounds = new Rect();
    getMountBounds(mountBounds, new Rect(10, 10, 10, 10), 5, 2);
    assertThat(mountBounds).isEqualTo(new Rect(5, 8, 5, 8));
  }

  private static Rect getMountBounds(Rect outRect, Rect bounds, int x, int y) {
    outRect.left = bounds.left - x;
    outRect.top = bounds.top - y;
    outRect.right = bounds.right - x;
    outRect.bottom = bounds.bottom - y;
    return outRect;
  }
}
