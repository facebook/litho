/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.DisplayListPrefetcher.AverageDLPrefetchDuration.INITIAL_SIZE;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.DisplayListPrefetcher.AverageDLPrefetchDuration;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link DisplayListPrefetcher}
 */
public class DisplayListPrefetcherTest {

  AverageDLPrefetchDuration mAverageDLPrefetchDuration;

  @Before
  public void setup() {
    mAverageDLPrefetchDuration = new AverageDLPrefetchDuration();
  }

  @Test
  public void testAverageDLPrefetchDurationEmpty() {
    assertThat(-1L).isEqualTo(mAverageDLPrefetchDuration.get("Text"));
  }

  @Test
  public void testAverageDLPrefetchDurationAddItem() {
    mAverageDLPrefetchDuration.put("Image", 100L);
    assertThat(-1L).isEqualTo(mAverageDLPrefetchDuration.get("Text"));
    assertThat(100L).isEqualTo(mAverageDLPrefetchDuration.get("Image"));
  }

  @Test
  public void testAverageDLPrefetchDurationUpdateItems() {
    mAverageDLPrefetchDuration.put("Image", 100L);
    mAverageDLPrefetchDuration.put("Image", 200L);
    mAverageDLPrefetchDuration.put("Image", 250L);
    assertThat(250L).isEqualTo(mAverageDLPrefetchDuration.get("Image"));
  }

  @Test
  public void testAverageDLPrefetchDurationAddItemsExceedInitialSize() {
    int initialSize = INITIAL_SIZE;
    for (int i = 0; i < initialSize; i++) {
      mAverageDLPrefetchDuration.put("Image" + i, 100L + i * 10);
    }
    mAverageDLPrefetchDuration.put("Image" + initialSize, 111L);
    assertThat(111L).isEqualTo(mAverageDLPrefetchDuration.get("Image" + initialSize));
  }
}
