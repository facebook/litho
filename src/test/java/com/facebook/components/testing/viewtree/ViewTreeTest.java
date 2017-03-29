// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import javax.annotation.Nullable;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ViewTree}
 */
@RunWith(ComponentsTestRunner.class)
public class ViewTreeTest {

  private ViewGroup mRoot;
  private ViewGroup mChildLayout;
  private View mChild1;
  private View mGrandchild1;
  private View mGrandchild2;
  private ViewTree mTree;

  @Before
  public void setUp() {
