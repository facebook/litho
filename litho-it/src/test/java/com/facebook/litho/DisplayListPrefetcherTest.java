/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import org.junit.Before;
import org.junit.Test;
import com.facebook.litho.DisplayListPrefetcher.AverageDLPrefetchDuration;

import static junit.framework.Assert.assertEquals;

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
    assertEquals(mAverageDLPrefetchDuration.get("Text"), -1);
  }

  @Test
  public void testAverageDLPrefetchDurationAddItem() {
    mAverageDLPrefetchDuration.put("Image", 100L);
    assertEquals(mAverageDLPrefetchDuration.get("Text"), -1);
    assertEquals(mAverageDLPrefetchDuration.get("Image"), 100L);
  }

  @Test
  public void testAverageDLPrefetchDurationUpdateItems() {
    mAverageDLPrefetchDuration.put("Image", 100L);
    mAverageDLPrefetchDuration.put("Image", 200L);
    mAverageDLPrefetchDuration.put("Image", 250L);
    assertEquals(mAverageDLPrefetchDuration.get("Image"), 250L);
  }

  @Test
  public void testAverageDLPrefetchDurationAddItemsExceedInitialSize() {
    int initialSize = AverageDLPrefetchDuration.INITIAL_SIZE;
    for (int i = 0; i < initialSize; i++) {
      mAverageDLPrefetchDuration.put("Image"+i, 100L + i * 10);
    }
    mAverageDLPrefetchDuration.put("Image"+initialSize, 111L);
    assertEquals(mAverageDLPrefetchDuration.get("Image"+initialSize), 111L);
  }
}
