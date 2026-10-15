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

package com.facebook.litho.testing.viewtree

import kotlin.math.min

object LevenshteinDistance {
  /**
   * Efficient version of Levenshtein Distance Algorithm. It saves CPU by returning early if the
   * distance goes over maxAllowedEditDistance. See examples in LevenshteinDistanceTest class.
   *
   * @param s String
   * @param t String
   * @param maxAllowedEditDistance int
   * @return min(LD(s, t), maxAllowedEditDistance + 1)
   */
  @JvmStatic
  fun getLevenshteinDistance(s: String, t: String, maxAllowedEditDistance: Int): Int {
    val n = s.length // length of s
    val m = t.length // length of t

    if (n == 0) {
      return m
    } else if (m == 0) {
      return n
    }

    var p = IntArray(n + 1) // 'previous' cost array, horizontally
    var d = IntArray(n + 1) // cost array, horizontally

    for (i in 0..n) {
      p[i] = i
    }

    for (j in 1..m) {
      val tJ = t[j - 1] // jth character of t
      d[0] = j
      var minDist = maxAllowedEditDistance + 1 // To keep track of min edit distance per iteration

      for (i in 1..n) {
        val cost = if (s[i - 1] == tJ) 0 else 1
        // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
        d[i] = min(min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost)
        if (minDist > d[i]) {
          minDist = d[i]
        }
      }
      // If this is the last iteration, then minEditdistance which may be less than
      // actual edit distance for some substring of t, makes no sense. So,
      if (j == m) {
        minDist = if (d[n] > maxAllowedEditDistance) maxAllowedEditDistance + 1 else d[n]
      }
      // Terminate, when the minEditdistance exceeds max allowed distance for speed up.
      if (minDist > maxAllowedEditDistance) {
        return minDist
      }

      // copy current distance counts to 'previous row' distance counts
      val temp = p
      p = d
      d = temp
    }

    // our last action in the above loop was to switch d and p, so p now
    // actually has the most recent cost counts
    return p[n]
  }
}
