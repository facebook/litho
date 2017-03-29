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
