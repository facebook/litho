// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import javax.annotation.Nullable;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.litho.testing.viewtree.ViewExtractors.GET_TEXT_FUNCTION;
import static com.facebook.litho.testing.viewtree.ViewPredicates.hasTextMatchingPredicate;
import static com.facebook.litho.testing.viewtree.ViewPredicates.hasVisibleId;
import static com.facebook.litho.testing.viewtree.ViewPredicates.isVisible;

/**
 * Assertions which require checking an entire view tree
 *
 * NOTE: Assertions looking for visible attributes are limited to checking the visibility of the
 * nodes, but do not check actual layout. So a visible view might have 0 pixels available for it
 * in actual app code and still pass the checks done here
 */
public final class ViewTreeAssert extends AbstractAssert<ViewTreeAssert, ViewTree> {

  private ViewTreeAssert(final ViewTree actual) {
    super(actual, ViewTreeAssert.class);
  }

  public static ViewTreeAssert assertThat(final ViewTree actual) {
    return new ViewTreeAssert(actual);
  }

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, has the
   * requested piece of text as its text
   *
   * @param text the text to search for
   * @return the assertions object
   */
  public ViewTreeAssert hasVisibleText(final String text) {
    final ImmutableList<View> path = getPathToVisibleText(text);

    Assertions.assertThat(path)
        .overridingErrorMessage(path == null ? getHasVisibleTextErrorMessage(text) : "")
        .isNotNull();

    return this;
  }

  private String getHasVisibleTextErrorMessage(final String text) {
    String errorMsg = String.format(
        "Cannot find text \"%s\" in view hierarchy:%n%s. ",
        text,
        actual.makeString(GET_TEXT_FUNCTION));

    final ImmutableList<View> similarPath = getPathToVisibleSimilarText(text);
    if (similarPath != null) {
      errorMsg += String.format(
          "\nHowever, a near-match was found: \"%s\"",
          GET_TEXT_FUNCTION.apply(similarPath.get(similarPath.size() - 1)));
    } else {
      errorMsg += "\nNo near-match was found.";
    }
    return errorMsg;
  }

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, has the
   * requested piece of text as its text and has a tag set on that TextView with the given tag id
   * and tag value.
   *
   * @param text the text to search for
   * @param tagId the tag to look for on the TextView containing the searched text
   * @param tagValue the expected value of the tag associated with tagId
   * @return the assertions object
   */
  public ViewTreeAssert hasVisibleTextWithTag(final String text, final int tagId, final Object tagValue) {
    final ImmutableList<View> path = getPathToVisibleTextWithTag(text, tagId, tagValue);

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Cannot find text \"%s\" with tagId \"%d\" and value:%s in view hierarchy:%n%s",
            text,
            tagId,
            tagValue.toString(),
            actual.makeString(GET_TEXT_FUNCTION))
        .isNotNull();

    return this;
  }

  /**
   * Tests if any view has visible text identified by the resource id
   *
   * @param resourceId resource id of the text
   * @return the assertions object
   */
  public ViewTreeAssert hasVisibleText(final int resourceId) {
    return hasVisibleText(
        RuntimeEnvironment
            .application
            .getResources()
            .getString(resourceId));
  }

  /**
   * Tests that all views in the hierarchy under the root, for which the path is visible, do not
   * have text equal to the given string
   *
   * @param text the text to search for
   * @return the assertions object
   */
  public ViewTreeAssert doesNotHaveVisibleText(final String text) {
    final ImmutableList<View> path = getPathToVisibleText(text);

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found text \"%s\" in view hierarchy for path: %s",
            text,
            makeString(path))
        .isNull();

    return this;
  }

  /**
   * Tests if any view hierarchy under the root has the given view tag and value.
   * @param tagId the id to look for
   * @param tagValue the value that the id should have
   * @return the assertions object
   */
  public ViewTreeAssert hasViewTag(final int tagId, final Object tagValue) {
    final ImmutableList<View> path = getPathToViewTag(tagId, tagValue);

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Cannot find tag id \"%d\" with tag value \"%s\" in view hierarchy:%n%s",
            tagId,
            tagValue,
            actual.makeString(ViewExtractors.generateGetViewTagFunction(tagId)))
        .isNotNull();

    return this;
  }

  /**
   * Tests if any view hierarchy under the root has the given contentDescription.
   * @param contentDescription the contentDescription to search for
   * @return the assertions object
   */
  public ViewTreeAssert hasContentDescription(final String contentDescription) {
    final ImmutableList<View> path = getPathToContentDescription(contentDescription);

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Cannot find content description \"%s\" in view hierarchy:%n%s",
            contentDescription,
            actual.makeString(ViewExtractors.GET_CONTENT_DESCRIPTION_FUNCTION))
        .isNotNull();

    return this;
  }

  /**
   * Tests that all views in the hierarchy under the root, for which the path is visible, do not
   * have text equal to the string matching the given resource id
   *
   * @param resourceId resource id of the text
   * @return the assertions object
   */
  public ViewTreeAssert doesNotHaveVisibleText(final int resourceId) {
    return doesNotHaveVisibleText(
        RuntimeEnvironment
            .application
            .getResources()
            .getString(resourceId));
  }

  /**
   * Tests if any view hierarchy under the root has the given contentDescription.
   * @param resourceId the resId of the contentDescription to search for
   * @return the assertions object
   */
  public ViewTreeAssert hasContentDescription(final int resourceId) {
    return hasContentDescription(
        RuntimeEnvironment
            .application
            .getResources()
            .getString(resourceId));
  }

  private ImmutableList<View> getPathToVisibleSimilarText(final String text) {
    return actual.findChild(
        Predicates.and(
            isVisible(),
            hasTextMatchingPredicate(new Predicate<String>() {
          @Override
          public boolean apply(@Nullable final String input) {
            final int maxEditDistance = Math.max(3, text.length() / 4);
            return LevenshteinDistance.getLevenshteinDistance(text, input, maxEditDistance)
                <= maxEditDistance;
          }
        })),
        ViewPredicates.isVisible());
  }

  private ImmutableList<View> getPathToVisibleText(final String text) {
    return actual.findChild(
        ViewPredicates.hasVisibleText(text),
        ViewPredicates.isVisible());
  }

  private ImmutableList<View> getPathToVisibleTextWithTag(final String text, final int tagId, final Object tagValue) {
    return actual.findChild(
        ViewPredicates.hasVisibleTextWithTag(text, tagId, tagValue),
        ViewPredicates.isVisible());
  }

  private ImmutableList<View> getPathToViewTag(final int tagId, final Object tagValue) {
    return actual.findChild(ViewPredicates.hasTag(tagId, tagValue));
  }

  private ImmutableList<View> getPathToContentDescription(final String contentDescription) {
    return actual.findChild(ViewPredicates.hasContentDescription(contentDescription));
  }

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, has text that
   * matches the given regular expression
   *
   * @param pattern the regular expression to match against
   * @return the assertions object
   */
  public ViewTreeAssert hasVisibleTextMatching(final String pattern) {
    final ImmutableList<View> path = getPathToVisibleMatchingText(pattern);

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Cannot find text matching \"%s\" in view hierarchy:%n%s",
            pattern,
            actual.makeString(GET_TEXT_FUNCTION))
        .isNotNull();

    return this;
  }

  /**
   * Tests that all views in the hierarchy under the root, for which the path is visible, do not
   * have text that matches against the given regular expression
   *
   * @param pattern the regular expression to match against
   * @return the assertions object(
   */
  public ViewTreeAssert doesNotHaveVisibleTextMatching(final String pattern) {
    final ImmutableList<View> path = getPathToVisibleMatchingText(pattern);

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found pattern \"%s\" in view hierarchy for path: %s",
            pattern,
            makeString(path))
        .isNull();

    return this;
  }

  /**
   * Tests that all views in the hierarchy under the root, for which the path is visible, do not
   * have any text appearing on them
   *
   * @return the assertions object
   */
  public ViewTreeAssert doesNotHaveVisibleText() {
    final ImmutableList<View> path = getPathToVisibleMatchingText(".+");

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found text \"%s\" in view hierarchy for path: %s",
            getTextProof(path),
            makeString(path))
        .isNull();

    return this;
  }

  private String getTextProof(@Nullable final ImmutableList<View> path) {
    if (path == null) {
      return "";
    }

    final View last = path.get(path.size() - 1);
    return ((TextView) last).getText().toString();
  }

  private ImmutableList<View> getPathToVisibleMatchingText(final String pattern) {
    return actual.findChild(
        ViewPredicates.hasVisibleMatchingText(pattern),
        ViewPredicates.isVisible());
  }

  private String makeString(final Iterable<View> path) {
    return path != null ? Joiner.on(" -> ").join(path) : "";
  }

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, is displaying
   * the requested drawable by the given resource id.
   *
   * For this assertion to work, Robolectric must be immediately available and be able to load the
   * drawable corresponding to this resource id.
   *
   * @param resourceId the resource id of the drawable to look for
   * @return the assertions object
   */
  public ViewTreeAssert hasVisibleDrawable(final int resourceId) {
    hasVisibleDrawable(
        RuntimeEnvironment
            .application
            .getResources()
            .getDrawable(resourceId)
    );
    return this;
  }

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, is displaying
   * the requested drawable
   *
   * @param drawable the drawable to look for
   * @return the assertions object
   */
  public ViewTreeAssert hasVisibleDrawable(final Drawable drawable) {
    final ImmutableList<View> path = getPathToVisibleWithDrawable(drawable);

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Did not find drawable %s in view hierarchy:%n%s",
            drawable,
            actual.makeString(ViewExtractors.GET_DRAWABLE_FUNCTION))
        .isNotNull();

    return this;
  }

  /**
   * Tests all views in the hierarchy under the root, for which the path is visible, do not have
   * the requested drawable by the given resource id.
   * For this assertion to work, Robolectric must be immediately available and be able to load the
   * drawable corresponding to this resource id.
   *
   * @param resourceId the resource id of the drawable to look for
   * @return the assertions object
   */
  public ViewTreeAssert doesNotHaveVisibleDrawable(final int resourceId) {
    doesNotHaveVisibleDrawable(
        RuntimeEnvironment
            .application
            .getResources()
            .getDrawable(resourceId)
    );
    return this;
  }

  /**
   * Tests all views in the hierarchy under the root, for which the path is visible, are not
   * displaying the requested drawable
   *
   * @param drawable the drawable to look for
   * @return the assertions object
   */
  public ViewTreeAssert doesNotHaveVisibleDrawable(final Drawable drawable) {
    final ImmutableList<View> path = getPathToVisibleWithDrawable(drawable);

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found drawable %s in view hierarchy:%n%s",
            drawable,
            actual.makeString(ViewExtractors.GET_DRAWABLE_FUNCTION))
        .isNull();

    return this;
  }

  /** Whether there is a visible view in the hierarchy with the given id. */
  public ViewTreeAssert hasVisibleViewWithId(final int viewId) {
    final ImmutableList<View> path = getPathToVisibleWithId(viewId);

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Did not find visible view with id \"%s=%d\":%n%s",
            ViewTreeUtil.getResourceName(viewId),
            viewId,
            actual.makeString(ViewExtractors.GET_VIEW_ID_FUNCTION))
        .isNotNull();

    return this;
  }

  /** Whether there is not a visible view in the hierarchy with the given id. */
