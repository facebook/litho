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
    final Activity activity = Robolectric.buildActivity(Activity.class).create().get();
    mRoot = new LinearLayout(activity);
    mChildLayout = new LinearLayout(activity);
    mChild1 = new View(activity);
    mGrandchild1 = new View(activity);
    mGrandchild2 = new View(activity);

    mChildLayout.addView(mGrandchild1);
    mChildLayout.addView(mGrandchild2);
    mRoot.addView(mChild1);
    mRoot.addView(mChildLayout);

    mTree = ViewTree.of(mRoot);
  }

  @Test
  public void testFindRoot() throws Exception {
    assertThat(mTree.findChild(Predicates.<View>equalTo(mRoot))).containsExactly(mRoot);
  }

  @Test
  public void testReturnNullIfCannotFind() throws Exception {
    assertThat(mTree.findChild(Predicates.<View>equalTo(null))).isNull();
  }

  @Test
  public void testFindChild() throws Exception {
    assertThat(mTree.findChild(Predicates.<View>equalTo(mChildLayout)))
        .containsExactly(mRoot, mChildLayout);
  }

  @Test
  public void testFindGrandchild() throws Exception {
    assertThat(mTree.findChild(Predicates.<View>equalTo(mGrandchild2)))
        .containsExactly(mRoot, mChildLayout, mGrandchild2);
  }

  @Test
  public void testRespectShouldGoIntoChildren() throws Exception {
    assertThat(mTree.findChild(
        Predicates.<View>equalTo(mGrandchild2),
        Predicates.not(Predicates.equalTo(mChildLayout))))
        .isNull();
  }

  @Test
  public void testGenerateString() {
    final String expected =
        getString(mRoot) + " (" + mRoot.hashCode() + ")\n" +
            "  " + getString(mChild1) + " (" + mChild1.hashCode() + ")\n" +
            "  " + getString(mChildLayout) + " (" + mChildLayout.hashCode() + ")\n" +
            "    " + getString(mGrandchild1) + " (" + mGrandchild1.hashCode() + ")\n" +
            "    " + getString(mGrandchild2) + " (" + mGrandchild2.hashCode() + ")";

    assertThat(mTree.makeString(new Function<View, String>() {

      @Override
      public String apply(@Nullable final View input) {
        return String.valueOf(input.hashCode());
      }
    })).isEqualTo(expected);
  }

  private String getString(final View view) {
