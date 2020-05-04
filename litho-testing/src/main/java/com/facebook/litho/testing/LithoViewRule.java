// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.InternalNode;
import com.facebook.litho.LayoutState;
import com.facebook.litho.LithoView;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.RuntimeEnvironment;

public class LithoViewRule implements TestRule {

  private static final int DEFAULT_WIDTH_SPEC = makeMeasureSpec(1080, EXACTLY);
  private static final int DEFAULT_HEIGHT_SPEC = makeMeasureSpec(0, UNSPECIFIED);

  private ComponentContext mContext;
  private ComponentTree mComponentTree;
  private LithoView mLithoView;
  private int mWidthSpec = DEFAULT_WIDTH_SPEC;
  private int mHeightSpec = DEFAULT_HEIGHT_SPEC;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          mContext = new ComponentContext(RuntimeEnvironment.application);
          base.evaluate();
        } finally {
          mContext = null;
          mComponentTree = null;
          mLithoView = null;
          mWidthSpec = DEFAULT_WIDTH_SPEC;
          mHeightSpec = DEFAULT_HEIGHT_SPEC;
        }
      }
    };
  }

  public ComponentContext getContext() {
    return mContext;
  }

  public LithoView getLithoView() {
    if (mLithoView == null) {
      mLithoView = new LithoView(mContext);
      mLithoView.setComponentTree(getComponentTree());
    }
    return mLithoView;
  }

  public LithoViewRule useLithoView(LithoView lithoView) {
    mLithoView = lithoView;
    if (mLithoView.getComponentContext() != mContext) {
      throw new RuntimeException(
          "You must use the same ComponentContext for the LithoView as what is on the LithoViewRule @Rule!");
    }
    if (mComponentTree != null) {
      mLithoView.setComponentTree(mComponentTree);
    }
    return this;
  }

  public ComponentTree getComponentTree() {
    if (mComponentTree == null) {
      mComponentTree = ComponentTree.create(mContext).build();
    }
    return mComponentTree;
  }

  public LithoViewRule useComponentTree(ComponentTree componentTree) {
    mComponentTree = componentTree;
    getLithoView().setComponentTree(componentTree);
    return this;
  }

  public LithoViewRule setRoot(Component component) {
    getComponentTree().setRoot(component);
    return this;
  }

  public LithoViewRule setRootAsync(Component component) {
    getComponentTree().setRootAsync(component);
    return this;
  }

  public LithoViewRule setSizePx(int widthPx, int heightPx) {
    mWidthSpec = makeMeasureSpec(widthPx, EXACTLY);
    mHeightSpec = makeMeasureSpec(heightPx, EXACTLY);
    return this;
  }

  public LithoViewRule setSizeSpecs(int widthSpec, int heightSpec) {
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    return this;
  }

  public LithoViewRule measure() {
    getLithoView().measure(mWidthSpec, mHeightSpec);
    return this;
  }

  public LithoViewRule layout() {
    final LithoView lithoView = getLithoView();
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());
    return this;
  }

  public LithoViewRule attachToWindow() {
    getLithoView().onAttachedToWindowForTest();
    return this;
  }

  public LithoViewRule detachFromWindow() {
    getLithoView().onDetachedFromWindowForTest();
    return this;
  }

  protected @Nullable LayoutState getCommittedLayoutState() {
    return getComponentTree().getCommittedLayoutState();
  }

  protected @Nullable InternalNode getCurrentRootNode() {
    return getCommittedLayoutState() != null ? getCommittedLayoutState().getLayoutRoot() : null;
  }
}
