/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
