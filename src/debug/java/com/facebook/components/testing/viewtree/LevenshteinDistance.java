// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

public class LevenshteinDistance {
  /**
   * Efficient version of Levenshtein Distance Algorithm. It saves CPU by returning early if
   * the distance goes over maxAllowedEditDistance. See examples in LevenshteinDistanceTest class.
