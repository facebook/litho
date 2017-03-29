// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import javax.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.litho.ComponentHost;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowCanvas;

/**
 * A collection of useful predicates over Android views for tests
 */
final class ViewPredicates {

  private ViewPredicates() {}

  /**
   * Returns a predicate that returns true if the applied on view's text is equal to the given
   * text.
   * substring.
   * @param predicate the predicate with which to test the text
   * @return the predicate
   */
  public static Predicate<View> hasTextMatchingPredicate(final Predicate<String> predicate) {
    return new Predicate<View>() {
      @Override
      public boolean apply(final View input) {
        if (predicate.apply(extractString(input))) {
          return true;
        }

        if (input instanceof ComponentHost) {
          return ComponentQueries.hasTextMatchingPredicate((ComponentHost) input, predicate);
        }

        return false;
