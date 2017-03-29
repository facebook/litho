// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.testing.viewtree;

public class LevenshteinDistance {
  /**
   * Efficient version of Levenshtein Distance Algorithm. It saves CPU by returning early if
   * the distance goes over maxAllowedEditDistance. See examples in LevenshteinDistanceTest class.
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

    int p[] = new int[n + 1]; //'previous' cost array, horizontally
    int d[] = new int[n + 1]; // cost array, horizontally
    int _d[]; //placeholder to assist in swapping p and d

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
