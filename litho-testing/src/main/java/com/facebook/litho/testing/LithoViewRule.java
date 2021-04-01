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

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentsPools;
import com.facebook.litho.LayoutState;
import com.facebook.litho.LithoLayoutResult;
import com.facebook.litho.LithoView;
import com.facebook.litho.TreeProps;
import com.facebook.litho.annotations.TreeProp;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This test utility allows clients to test assertion on the view hierarchy rendered by a Litho
 * components. The utility has methods to override the default {@link LithoView}, {@link
 * ComponentTree}, width, and height specs.
 *
 * <pre><code>{@literal @RunWith(LithoTestRunner.class)}
 * public class LithoSampleTest {
 *
 *   public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
 *
 *  {@literal @Test}
 *   public void test() {
 *     final ComponentContext c = mLithoViewRule.getContext();
 *     final Component component = MyComponent.create(c).build();
 *
 *     mLithoViewRule.setRoot(component)
 *       .attachToWindow()
 *       .setRoot(component)
 *       .measure()
 *       .layout();
 *
 *     LithoView lithoView = mLithoViewRule.getLithoView();
 *
 *     // Test your assertions on the litho view.
 *   }
 * }
 *
 * }</code></pre>
 */
public class LithoViewRule implements TestRule {

  public static final int DEFAULT_WIDTH_SPEC = makeMeasureSpec(1080, EXACTLY);
  public static final int DEFAULT_HEIGHT_SPEC = makeMeasureSpec(0, UNSPECIFIED);

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
          mContext = new ComponentContext(ApplicationProvider.getApplicationContext());
          mContext.setLayoutStateContextForTesting();
          base.evaluate();
        } finally {
          ComponentsPools.clearMountContentPools();
          mContext = null;
          mComponentTree = null;
          mLithoView = null;
          mWidthSpec = DEFAULT_WIDTH_SPEC;
          mHeightSpec = DEFAULT_HEIGHT_SPEC;
        }
      }
    };
  }

  /** Gets the current {@link ComponentContext}. */
  public ComponentContext getContext() {
    return mContext;
  }

  public LithoViewRule useContext(ComponentContext c) {
    mContext = c;
    return this;
  }

  /** Gets the current {@link LithoView}; creates a new instance if {@code null}. */
  public LithoView getLithoView() {
    if (mLithoView == null) {
      mLithoView = new LithoView(mContext);
    }

    if (mLithoView.getComponentTree() == null) {
      mLithoView.setComponentTree(getComponentTree());
    }

    return mLithoView;
  }

  /** Sets a new {@link LithoView} which should be used to render. */
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

  /** Gets the current {@link ComponentTree}; creates a new instance if {@code null}. */
  public ComponentTree getComponentTree() {
    if (mComponentTree == null) {
      mComponentTree = ComponentTree.create(mContext).build();
    }
    return mComponentTree;
  }

  /** Sets a new {@link ComponentTree} which should be used to render. */
  public LithoViewRule useComponentTree(ComponentTree componentTree) {
    mComponentTree = componentTree;
    getLithoView().setComponentTree(componentTree);
    return this;
  }

  /** Sets the new root {@link Component} to render. */
  public LithoViewRule setRoot(Component component) {
    getComponentTree().setRoot(component);
    return this;
  }

  /** Sets the new root {@link Component.Builder} to render. */
  public LithoViewRule setRoot(Component.Builder builder) {
    getComponentTree().setRoot(builder.build());
    return this;
  }

  /** Sets the new root {@link Component} to render asynchronously. */
  public LithoViewRule setRootAsync(Component component) {
    getComponentTree().setRootAsync(component);
    return this;
  }

  /** Sets the new root {@link Component.Builder} to render asynchronously. */
  public LithoViewRule setRootAsync(Component.Builder builder) {
    getComponentTree().setRootAsync(builder.build());
    return this;
  }

  /** Sets the new root {@link Component} with new size spec to render. */
  public LithoViewRule setRootAndSizeSpec(Component component, int widthSpec, int heightSpec) {
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    getComponentTree().setRootAndSizeSpec(component, mWidthSpec, mHeightSpec);
    return this;
  }

  /** Sets a new width and height which should be used to render. */
  public LithoViewRule setSizePx(int widthPx, int heightPx) {
    mWidthSpec = makeMeasureSpec(widthPx, EXACTLY);
    mHeightSpec = makeMeasureSpec(heightPx, EXACTLY);
    return this;
  }

  /** Sets a new width spec and height spec which should be used to render. */
  public LithoViewRule setSizeSpecs(int widthSpec, int heightSpec) {
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    return this;
  }

  /** Sets a new {@link TreeProp} for the next layout pass. */
  public LithoViewRule setTreeProp(Class<?> klass, Object instance) {
    TreeProps props = mContext.getTreeProps();
    if (props == null) {
      props = new TreeProps();
    }

    props.put(klass, instance);

    mContext.setTreeProps(props);

    return this;
  }

  /** Explicitly calls measure on the current root {@link LithoView} */
  public LithoViewRule measure() {
    getLithoView().measure(mWidthSpec, mHeightSpec);
    return this;
  }

  /** Explicitly calls layout on the current root {@link LithoView} */
  public LithoViewRule layout() {
    final LithoView lithoView = getLithoView();
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());
    return this;
  }

  /** Explicitly attaches current root {@link LithoView} */
  public LithoViewRule attachToWindow() {
    getLithoView().onAttachedToWindowForTest();
    return this;
  }

  /** Explicitly detaches current root {@link LithoView} */
  public LithoViewRule detachFromWindow() {
    getLithoView().onDetachedFromWindowForTest();
    return this;
  }

  /** Explicitly releases current root {@link LithoView} */
  public LithoViewRule release() {
    getLithoView().release();
    return this;
  }

  /** Gets the current width spec */
  public int getWidthSpec() {
    return mWidthSpec;
  }

  /** Gets the current height spec */
  public int getHeightSpec() {
    return mHeightSpec;
  }

  /**
   * Finds the first {@link View} with the specified tag in the rendered hierarchy, returning null
   * if is doesn't exist.
   */
  public @Nullable View findViewWithTagOrNull(Object tag) {
    return findViewWithTagTransversal(mLithoView, tag);
  }

  /**
   * Finds the first {@link View} with the specified tag in the rendered hierarchy, throwing if it
   * doesn't exist.
   */
  public View findViewWithTag(Object tag) {
    final View view = findViewWithTagOrNull(tag);
    if (view == null) {
      throw new RuntimeException("Did not find view with tag '" + tag + "'");
    }
    return view;
  }

  private @Nullable View findViewWithTagTransversal(View view, Object tag) {
    if (view.getTag() != null && view.getTag().equals(tag)) {
      return view;
    }
    if (view instanceof ViewGroup) {
      ViewGroup viewGroup = ((ViewGroup) view);
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        View child = findViewWithTagTransversal(viewGroup.getChildAt(i), tag);
        if (child != null) {
          return child;
        }
      }
    }
    return null;
  }

  protected @Nullable LayoutState getCommittedLayoutState() {
    return getComponentTree().getCommittedLayoutState();
  }

  public @Nullable LithoLayoutResult getCurrentRootNode() {
    return getCommittedLayoutState() != null ? getCommittedLayoutState().getLayoutRoot() : null;
  }
}
