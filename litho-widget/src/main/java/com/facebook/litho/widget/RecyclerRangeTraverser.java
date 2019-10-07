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

package com.facebook.litho.widget;

/** An interface for generating traversing order for a range. */
public interface RecyclerRangeTraverser {
  RecyclerRangeTraverser FORWARD_TRAVERSER =
      new RecyclerRangeTraverser() {
        @Override
        public void traverse(
            int rangeStart, int rangeEnd, int firstVisible, int lastVisible, Processor processor) {
          for (int i = rangeStart; i < rangeEnd; i++) {
            if (!processor.process(i)) {
              return;
            }
          }
        }
      };

  RecyclerRangeTraverser BACKWARD_TRAVERSER =
      new RecyclerRangeTraverser() {
        @Override
        public void traverse(
            int rangeStart, int rangeEnd, int firstVisible, int lastVisible, Processor processor) {
          for (int i = rangeEnd - 1; i >= rangeStart; i--) {
            if (!processor.process(i)) {
              return;
            }
          }
        }
      };

  /**
   * A more optimized range traverser that expands from the center for the visible range so that
   * items adjacent to the visible ones are traversed first.
   */
  RecyclerRangeTraverser BIDIRECTIONAL_TRAVERSER =
      new RecyclerRangeTraverser() {
        @Override
        public void traverse(
            int rangeStart, int rangeEnd, int firstVisible, int lastVisible, Processor processor) {

          if (rangeEnd <= rangeStart) {
            return;
          }

          final boolean firstVisibleValid = rangeStart <= firstVisible && firstVisible < rangeEnd;
          final boolean lastVisibleValid = rangeStart <= lastVisible && lastVisible < rangeEnd;

          final int center;
          if (!firstVisibleValid && !lastVisibleValid) {
            center = (rangeEnd + rangeStart - 1) / 2;
          } else if (!firstVisibleValid) {
            center = lastVisible;
          } else if (!lastVisibleValid) {
            center = firstVisible;
          } else {
            center = (firstVisible + lastVisible) / 2;
          }

          if (!processor.process(center)) {
            return;
          }

          int distance = 1;
          while (true) {
            final int head = center - distance;
            final int tail = center + distance;
            final boolean headInRange = head >= rangeStart;
            final boolean tailInRange = tail < rangeEnd;

            if (!headInRange && !tailInRange) {
              return;
            }

            if (headInRange) {
              if (!processor.process(head)) {
                return;
              }
            }

            if (tailInRange) {
              if (!processor.process(tail)) {
                return;
              }
            }

            distance += 1;
          }
        }
      };

  /**
   * Traverse the given range.
   *
   * @param rangeStart Start of the range, inclusive
   * @param rangeEnd End of the range, exclusive
   * @param firstVisible Index of the first visible item
   * @param lastVisible Index of the last visible item
   * @param handler Handler that will perform job for each index in the range and determines if we
   *     need to stop
   */
  void traverse(
      int rangeStart, int rangeEnd, int firstVisible, int lastVisible, Processor processor);

  interface Processor {
    /** @return Whether or not to continue */
    boolean process(int index);
  }
}
