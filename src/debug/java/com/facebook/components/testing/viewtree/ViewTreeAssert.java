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
