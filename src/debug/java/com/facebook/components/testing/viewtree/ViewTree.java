// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import javax.annotation.Nullable;

import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

/**
 * This is a helper class to allow asserting on view trees and recursively
 * verify predicates on its nodes within the narrow abilities that
 * Robolectric affords us.
 */
public final class ViewTree {

  private final View mView;

  public static ViewTree of(View view) {
    return new ViewTree(view);
  }

  private ViewTree(View view) {
    mView = view;
  }

  /**
   * @return the view group used to generate this tree
   */
  public View getRoot() {
    return mView;
  }

  /**
   * Find a view in the hierarchy for which the given predicate is true
   *
   * @param predicate the predicate to find a view upholding
   * @return null if no such view is found, or a list showing the path in the hierarchy to the
   * view for which the predicate holds
   */
  @Nullable
  public ImmutableList<View> findChild(Predicate<View> predicate) {
    return findChild(mView, predicate, Predicates.<ViewGroup>alwaysTrue());
  }

  /**
   * Find a view in the hierarchy for which the given predicate is true, while only check children
   * of nodes as directed by the additional shouldCheckChildren predicate
   *
   * @param predicate the predicate to find a view upholding
   * @param shouldCheckChildren a predicate to decide whether to
   * @return null if no such view is found, or a list showing the path in the hierarchy to the
   * view for which the predicate holds
   */
  @Nullable
  public ImmutableList<View> findChild(
      Predicate<View> predicate,
      Predicate<? super ViewGroup> shouldCheckChildren) {
    return findChild(mView, predicate, shouldCheckChildren);
  }

  /**
   * Generates a string describing the views tree using the views' toString methods and an extra
   * information function.
   *
   * The output is a string, with each view of the tree in its own line, indented according to its
   * depth in the tree, and then the extra information supplied by teh function.
   *
   * This can be used, for example, to print all views and their respective text and is useful
   * for when assertions fail.
   *
   * @param extraTextFunction the function returning extra information to print per view, or null
   *   if not extra information should be printed
   * @return a string describing the tree
   */
  public String makeString(Function<View, String> extraTextFunction) {
    return makeString(extraTextFunction, mView, 0);
  }

  private String makeString(Function<View, String> extraTextFunction, View view, int depth) {
    final StringBuilder builder = new StringBuilder();
    if (depth > 0) {
      builder.append('\n');
    }
    for (int i = 0; i < depth; i++) {
      builder.append("  ");
    }
    builder.append(getViewString(view));
    String extra = extraTextFunction != null ? extraTextFunction.apply(view) : null;
    if (extra != null) {
      builder.append(" (");
      builder.append(extra);
      builder.append(")");
    }
    if (view instanceof ViewGroup) {
      ViewGroup viewGroup = (ViewGroup) view;
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        View child = viewGroup.getChildAt(i);
        builder.append(makeString(extraTextFunction, child, depth + 1));
      }
    }

    return builder.toString();
  }

  private String getViewString(View view) {
    String string = view.toString();
    return removePrefix(removePrefix(string, "android.widget."), "android.view.");
  }

  private static String removePrefix(String string, String prefix) {
    return string.startsWith(prefix) ? string.substring(prefix.length()) : string;
  }

