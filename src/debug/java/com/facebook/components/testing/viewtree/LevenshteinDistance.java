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
