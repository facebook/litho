// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore.utils;

import static org.assertj.core.api.Assertions.assertThat;

import android.graphics.Typeface;
import android.util.SparseArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SynchronizedTypefaceHelperTest {

  @Test
  public void testSynchronizedTypefaceSparseArray() {
    SparseArray<Typeface> sparseArray = new SparseArray<>();
    sparseArray.put(1, Typeface.DEFAULT);
    SynchronizedTypefaceHelper.SynchronizedTypefaceSparseArray synchronizedSparseArray =
        new SynchronizedTypefaceHelper.SynchronizedTypefaceSparseArray(sparseArray);
    synchronizedSparseArray.put(2, Typeface.DEFAULT_BOLD);
    assertThat(synchronizedSparseArray.get(1)).isSameAs(Typeface.DEFAULT);
    assertThat(synchronizedSparseArray.get(2)).isSameAs(Typeface.DEFAULT_BOLD);
  }

  @Test
  public void testSynchronizedLongSparseArray() {
    SynchronizedTypefaceHelper.SynchronizedLongSparseArray synchronizedLongSparseArray =
        new SynchronizedTypefaceHelper.SynchronizedLongSparseArray(new Object(), 2);
    SparseArray<Typeface> sparseArray = new SparseArray<>();
    sparseArray.put(1, Typeface.DEFAULT);
    synchronizedLongSparseArray.put(2, sparseArray);
    SparseArray<Typeface> gotSparseArray = synchronizedLongSparseArray.get(2);
    assertThat(gotSparseArray)
        .isInstanceOf(SynchronizedTypefaceHelper.SynchronizedTypefaceSparseArray.class);
    assertThat(gotSparseArray.get(1)).isSameAs(Typeface.DEFAULT);
  }

  @Test
  public void testSynchronizedSparseArray() {
    SynchronizedTypefaceHelper.SynchronizedSparseArray synchronizedSparseArray =
        new SynchronizedTypefaceHelper.SynchronizedSparseArray(new Object(), 2);
    SparseArray<Typeface> sparseArray = new SparseArray<>();
    sparseArray.put(1, Typeface.DEFAULT);
    synchronizedSparseArray.put(2, sparseArray);
    SparseArray<Typeface> gotSparseArray = synchronizedSparseArray.get(2);
    assertThat(gotSparseArray)
        .isInstanceOf(SynchronizedTypefaceHelper.SynchronizedTypefaceSparseArray.class);
    assertThat(gotSparseArray.get(1)).isSameAs(Typeface.DEFAULT);
  }
}
