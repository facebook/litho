// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;

import com.facebook.R;
import com.facebook.components.reference.Reference;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class ResolveAttributeTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(
        new ContextThemeWrapper(RuntimeEnvironment.application, R.style.TestTheme));
  }

  @Test
  public void testResolveDrawableAttribute() {
    InternalNode node = (InternalNode) Container.create(mContext)
        .backgroundAttr(R.attr.testAttrDrawable, 0)
        .build();

    Drawable d = mContext.getResources().getDrawable(R.drawable.test_bg);
    assertEquals(d, Reference.acquire(mContext, node.getBackground()));
  }

  @Test
  public void testResolveDimenAttribute() {
    InternalNode node = (InternalNode) Container.create(mContext)
        .widthAttr(R.attr.testAttrDimen, R.dimen.default_dimen)
        .build();
    node.calculateLayout();

    int dimen =
        mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertEquals(dimen, (int) node.getWidth());
  }

  @Test
  public void testDefaultDrawableAttribute() {
    InternalNode node = (InternalNode) Container.create(mContext)
        .backgroundAttr(R.attr.undefinedAttrDrawable, R.drawable.test_bg)
        .build();

    Drawable d = mContext.getResources().getDrawable(R.drawable.test_bg);
    assertEquals(d, Reference.acquire(mContext, node.getBackground()));
  }

  @Test
  public void testDefaultDimenAttribute() {
    InternalNode node = (InternalNode) Container.create(mContext)
        .widthAttr(R.attr.undefinedAttrDimen, R.dimen.test_dimen)
        .build();
    node.calculateLayout();

    int dimen =
        mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertEquals(dimen, (int) node.getWidth());
  }
}
