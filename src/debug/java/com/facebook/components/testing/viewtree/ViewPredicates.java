// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.testing.viewtree;

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

import com.facebook.components.ComponentHost;

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
      }
    };
  }

  /**
   * Returns a predicate that returns true if the applied on view's text is equal to the given
   * text.
   * substring.
   * @param text the text to check
   * @return the predicate
   */
  public static Predicate<View> hasText(final String text) {
    return hasTextMatchingPredicate(Predicates.equalTo(text));
  }

  public static Predicate<View> hasTag(final int tagId, final Object tagValue) {
    return new Predicate<View>() {
      @Override
      public boolean apply(final View input) {
        final Object tag = input.getTag(tagId);
        return tag != null && tag.equals(tagValue);
      }
    };
  }

  public static Predicate<View> hasContentDescription(final String contentDescription) {
    return new Predicate<View>() {
      @Override
      public boolean apply(final View input) {
        if (input instanceof ComponentHost) {
          final List<CharSequence> contentDescriptions =
              ((ComponentHost) input).getContentDescriptions();
          return contentDescriptions.contains(contentDescription);
        }

        return contentDescription.equals(input.getContentDescription());
      }
    };
  }

  public static Predicate<View> hasVisibleText(final String text) {
    return Predicates.and(isVisible(), hasText(text));
  }

  public static Predicate<View> hasVisibleTextWithTag(
      final String text,
      final int tagId,
      final Object tagValue) {
    return Predicates.and(hasVisibleText(text), hasTag(tagId, tagValue));
  }

  public static Predicate<View> matchesText(final String text) {
    final Pattern pattern = Pattern.compile(text);

    return new Predicate<View>() {
      @Override
      public boolean apply(final View input) {
        if (pattern.matcher(extractString(input)).find()) {
          return true;
        }

        if (input instanceof ComponentHost) {
          return ComponentQueries.matchesPattern((ComponentHost) input, pattern);
        }

        return false;
      }
    };
  }

  public static Predicate<View> hasVisibleMatchingText(final String text) {
    return Predicates.and(isVisible(), matchesText(text));
  }

  public static Predicate<View> isVisible() {
    return new Predicate<View>() {
      @Override
      public boolean apply(final View input) {
        return input.getVisibility() == View.VISIBLE;
      }
    };
  }

  @SuppressWarnings("unchecked")
  public static Predicate<View> isClass(final Class<? extends View> clazz) {
    return (Predicate<View>) (Predicate<?>) Predicates.instanceOf(clazz);
  }

  /**
   * Tries to extract the description of a drawn drawable from a canvas
   */
  static String getDrawnDrawableDescription(final Drawable drawable) {
    final Canvas canvas = new Canvas();
    drawable.draw(canvas);
    final ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
    return shadowCanvas.getDescription();
  }

  private static String extractString(final View view) {
    if (!(view instanceof TextView)) {
      return "";
    }

