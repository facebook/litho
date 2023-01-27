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

import android.util.SparseArray;
import androidx.collection.SparseArrayCompat;
import com.facebook.infer.annotation.FalseOnNull;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.TrueOnNull;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;

/** Provides util methods for common collection patterns. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class CollectionsUtils {

  @TrueOnNull
  public static <T> boolean isNullOrEmpty(@Nullable Collection<T> collection) {
    return collection == null || collection.isEmpty();
  }

  @FalseOnNull
  public static <T> boolean isNotNullOrEmpty(@Nullable Collection<T> collection) {
    return !isNullOrEmpty(collection);
  }

  @FalseOnNull
  public static <T> boolean isEmpty(@Nullable Collection<T> collection) {
    return collection != null && collection.isEmpty();
  }

  @TrueOnNull
  public static <K, V> boolean isNullOrEmpty(@Nullable Map<K, V> map) {
    return map == null || map.isEmpty();
  }

  @FalseOnNull
  public static <K, V> boolean isNotNullOrEmpty(@Nullable Map<K, V> map) {
    return !isNullOrEmpty(map);
  }

  @FalseOnNull
  public static <K, V> boolean isEmpty(@Nullable Map<K, V> map) {
    return map != null && map.isEmpty();
  }

  @TrueOnNull
  public static <T> boolean isNullOrEmpty(@Nullable SparseArrayCompat<T> sparseArrayCompat) {
    return sparseArrayCompat == null || sparseArrayCompat.isEmpty();
  }

  @FalseOnNull
  public static <T> boolean isNotNullOrEmpty(@Nullable SparseArrayCompat<T> sparseArrayCompat) {
    return !isNullOrEmpty(sparseArrayCompat);
  }

  @FalseOnNull
  public static <T> boolean isEmpty(@Nullable SparseArrayCompat<T> sparseArrayCompat) {
    return sparseArrayCompat != null && sparseArrayCompat.isEmpty();
  }

  @TrueOnNull
  public static <T> boolean isNullOrEmpty(@Nullable SparseArray<T> sparseArray) {
    return sparseArray == null || sparseArray.size() == 0;
  }

  @FalseOnNull
  public static <T> boolean isNotNullOrEmpty(@Nullable SparseArray<T> sparseArray) {
    return !isNullOrEmpty(sparseArray);
  }

  @FalseOnNull
  public static <T> boolean isEmpty(@Nullable SparseArray<T> sparseArray) {
    return sparseArray != null && sparseArray.size() == 0;
  }
}
