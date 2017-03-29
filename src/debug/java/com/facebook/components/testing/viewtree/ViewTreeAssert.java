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
