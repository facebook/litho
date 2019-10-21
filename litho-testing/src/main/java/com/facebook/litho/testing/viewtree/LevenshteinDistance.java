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

package com.facebook.litho.testing.viewtree;

public class LevenshteinDistance {
  /**
   * Efficient version of Levenshtein Distance Algorithm. It saves CPU by returning early if the
   * distance goes over maxAllowedEditDistance. See examples in LevenshteinDistanceTest class.
   *
   * @param s String
   * @param t String
   * @param maxAllowedEditDistance int
   * @return min(LD(s, t), maxAllowedEditDistance + 1)
   */
  public static int getLevenshteinDistance(String s, String t, int maxAllowedEditDistance) {
    if (s == null || t == null) {
      throw new IllegalArgumentException("Strings must not be null");
    }
    int n = s.length(); // length of s
    int m = t.length(); // length of t

    if (n == 0) {
      return m;
    } else if (m == 0) {
      return n;
    }

    int p[] = new int[n + 1]; // 'previous' cost array, horizontally
    int d[] = new int[n + 1]; // cost array, horizontally
    int _d[]; // placeholder to assist in swapping p and d

    // indexes into strings s and t
    int i; // iterates through s
    int j; // iterates through t

    char t_j; // jth character of t

    int cost; // cost
    int min; // To keep track of min edit distance per iteration

    for (i = 0; i <= n; i++) {
      p[i] = i;
    }

    for (j = 1; j <= m; j++) {
      t_j = t.charAt(j - 1);
      d[0] = j;
      min = maxAllowedEditDistance + 1;

      for (i = 1; i <= n; i++) {
        cost = s.charAt(i - 1) == t_j ? 0 : 1;
        // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
        d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
        if (min > d[i]) {
          min = d[i];
        }
      }
      // If this is the last iteration, then minEditdistance which may be less than
      // actual edit distance for some substring of t, makes no sense. So,
      if (j == m) {
        min = d[n] > maxAllowedEditDistance ? maxAllowedEditDistance + 1 : d[n];
      }
      // Terminate, when the minEditdistance exceeds max allowed distance for speed up.
      if (min > maxAllowedEditDistance) {
        return min;
      }

      // copy current distance counts to 'previous row' distance counts
      _d = p;
      p = d;
      d = _d;
    }

    // our last action in the above loop was to switch d and p, so p now
    // actually has the most recent cost counts
    return p[n];
  }
}
