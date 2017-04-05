// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.assertj;

import java.util.Deque;
import java.util.Locale;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;

import com.facebook.litho.ComponentView;
import com.facebook.litho.ComponentViewTestHelper;
import com.facebook.litho.TestItem;
import com.facebook.litho.testing.viewtree.ViewTree;
import com.facebook.litho.testing.viewtree.ViewTreeAssert;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

/**
 * Assertion methods for {@link ComponentView}s.
 *
 * <p> To create an instance of this class, invoke
 * <code>{@link ComponentViewAssert#assertThat(ComponentView)}</code>.
 */
public class ComponentViewAssert extends AbstractAssert<ComponentViewAssert, ComponentView> {

  public static ComponentViewAssert assertThat(ComponentView actual) {
    return new ComponentViewAssert(actual);
  }

  ComponentViewAssert(ComponentView actual) {
    super(actual, ComponentViewAssert.class);
  }

  public ComponentViewAssert containsTestKey(String testKey) {
    return containsTestKey(testKey, once());
  }

  public ComponentViewAssert containsTestKey(String testKey, OccurrenceCount count) {
    final Deque<TestItem> testItems = ComponentViewTestHelper.findTestItems(actual, testKey);
    Assertions.assertThat(testItems)
        .hasSize(count.times)
        .overridingErrorMessage(
            "Expected to find test key <%s> in ComponentView <%s> %s, but %s.",
            testKey,
            actual,
            count,
            testItems.isEmpty() ?
                "couldn't find it" :
                String.format(Locale.ROOT, "saw it %d times instead", testItems.size()))
        .isNotNull();

    return this;
  }

  public ComponentViewAssert doesNotContainTestKey(String testKey) {
    final TestItem testItem = ComponentViewTestHelper.findTestItem(actual, testKey);
    final Rect bounds = testItem == null ? null : testItem.getBounds();

    Assertions.assertThat(testItem)
        .overridingErrorMessage(
            "Expected not to find test key <%s> in ComponentView <%s>, but it was present at " +
                "bounds %s.",
            testKey,
            actual,
            bounds)
        .isNull();

    return this;
  }

  private ViewTreeAssert assertThatViewTree() {
    return ViewTreeAssert.assertThat(ViewTree.of(actual));
  }

  /**
   * Assert that any view in the given Component has the provided content
   * description.
   */
  public ComponentViewAssert hasContentDescription(String contentDescription) {
    assertThatViewTree().hasContentDescription(contentDescription);

    return this;
  }

  /**
   * Assert that the given component contains the drawable identified by the provided drawable
   * resource id.
   */
  public ComponentViewAssert hasVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatViewTree().hasVisibleDrawable(drawableRes);

    return this;
  }

  /**
   * Assert that the given component contains the drawable provided.
   */
  public ComponentViewAssert hasVisibleDrawable(Drawable drawable) {
    assertThatViewTree().hasVisibleDrawable(drawable);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleDrawable(Drawable)}
   */
  public ComponentViewAssert doesNotHaveVisibleDrawable(Drawable drawable) {
    assertThatViewTree().doesNotHaveVisibleDrawable(drawable);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleDrawable(int)}
   */
  public ComponentViewAssert doesNotHaveVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatViewTree().doesNotHaveVisibleDrawable(drawableRes);

    return this;
  }

  /**
   * Assert that the given component has the exact text provided.
   */
  public ComponentViewAssert hasVisibleText(String text) {
    assertThatViewTree().hasVisibleText(text);

    return this;
  }

  /**
   * Assert that the view tag is present for the given index.
   * @param tagId Index of the view tag.
   * @param tagValue View tag value.
   */
  public ComponentViewAssert hasViewTag(int tagId, Object tagValue) {
    assertThatViewTree().hasViewTag(tagId, tagValue);

    return this;
  }

  public static OccurrenceCount times(int i) {
    return new OccurrenceCount(i, i + " times");
  }

  public static OccurrenceCount once() {
    return new OccurrenceCount(1, "once");
  }

  public static class OccurrenceCount {
    final int times;
    final String shortName;

    OccurrenceCount(int times, String shortName) {
      this.times = times;
      this.shortName = shortName;
    }

    public String toString() {
      return shortName;
    }
  }
}
