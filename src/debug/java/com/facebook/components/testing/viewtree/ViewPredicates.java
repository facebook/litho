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

    final CharSequence text = ((TextView) view).getText();
    return text != null ? text.toString() : "";
  }

  public static Predicate<View> hasDrawable(final Drawable drawable) {
    return new Predicate<View>() {
      @Override
      public boolean apply(@Nullable final View input) {
        if (input instanceof ImageView) {
          final Drawable imageDrawable = ((ImageView) input).getDrawable();

          if (drawable instanceof BitmapDrawable && !(imageDrawable instanceof BitmapDrawable)) {
            return false;
          }
        }

        if (input instanceof ComponentHost) {
          return ComponentQueries.hasDrawable((ComponentHost) input, drawable);
        }

        final String drawnDrawableDescription = getDrawnDrawableDescription(drawable);
        return !drawnDrawableDescription.isEmpty() &&
            getDrawnViewDescription(input).contains(drawnDrawableDescription);
      }
    };
  }

  public static Predicate<View> hasVisibleDrawable(final Drawable drawable) {
    return Predicates.and(isVisible(), hasDrawable(drawable));
  }

  /** @return A Predicate which is true if the view is visible and has the given id. */
  public static Predicate<View> hasVisibleId(final int viewId) {
    return Predicates.and(isVisible(), hasId(viewId));
  }

  /**
   * Tries to extract the description of a drawn view from a canvas
   *
   * Since Robolectric can screw up {@link View#draw}, this uses reflection to call
   * {@link View#onDraw} and give you a canvas that has all the information drawn into it.
   * This is useful for asserting some view draws something specific to a canvas.
   *
   * @param view the view to draw
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static String getDrawnViewDescription(View view) {
    final Canvas canvas = new Canvas();
    view.draw(canvas);
    final ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
    if (!shadowCanvas.getDescription().isEmpty()) {
      return shadowCanvas.getDescription();
    }

    try {
      final Method onDraw = view.getClass().getMethod("onDraw", Canvas.class);
      onDraw.invoke(view, canvas);
      final ShadowCanvas shadowCanvas2 = Shadows.shadowOf(canvas);
      return shadowCanvas2.getDescription();
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static Predicate<View> hasId(final int id) {
    return new Predicate<View>() {
      @Override
      public boolean apply(final View input) {
        return input.getId() == id;
      }
    };
  }
