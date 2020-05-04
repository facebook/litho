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

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class TestItemTest {
  private TestItem mTestItem;

  @Before
  public void setup() {
    mTestItem = new TestItem();
  }

  @Test
  public void testPositionAndSizeSet() {
    mTestItem.setBounds(0, 1, 3, 4);

    assertThat(mTestItem.getBounds().left).isEqualTo(0);
    assertThat(mTestItem.getBounds().top).isEqualTo(1);
    assertThat(mTestItem.getBounds().right).isEqualTo(3);
    assertThat(mTestItem.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testRectBoundsSet() {
    final Rect bounds = new Rect(0, 1, 3, 4);
    mTestItem.setBounds(bounds);
    assertThat(mTestItem.getBounds().left).isEqualTo(0);
    assertThat(mTestItem.getBounds().top).isEqualTo(1);
    assertThat(mTestItem.getBounds().right).isEqualTo(3);
    assertThat(mTestItem.getBounds().bottom).isEqualTo(4);
  }
}
