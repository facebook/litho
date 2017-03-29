// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.testing.viewtree;

import java.util.List;
import java.util.regex.Pattern;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.components.ComponentHost;
import com.facebook.components.MatrixDrawable;

import com.google.common.base.Predicate;

/**
 * Utility APIs to query the state of components.
 */
class ComponentQueries {

  /**
   * Checks whether the given {@link ComponentHost} has the given text as it's text. It does not
   * look at the host's children.
   * @param host the component host
