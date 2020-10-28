/*
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

package com.facebook.rendercore.testing;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderResult;
import com.facebook.rendercore.RenderState;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeHost;
import com.facebook.rendercore.RenderTreeHostView;
import com.facebook.rendercore.RootHost;
import com.facebook.rendercore.RootHostView;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.RuntimeEnvironment;

/**
 * This test utility allows clients to test assertion on the rendered view hierarchy; for example by
 * using {@link ViewAssertions}. The utility has methods to override the default {@link RootHost},
 * {@link RenderTreeHost}, width, and height specs.
 */
public class RenderCoreTestRule implements TestRule {

  public static final int DEFAULT_WIDTH_SPEC = makeMeasureSpec(1080, EXACTLY);
  public static final int DEFAULT_HEIGHT_SPEC = makeMeasureSpec(0, UNSPECIFIED);

  private static final RenderState.Delegate DELEGATE =
      new RenderState.Delegate() {
        @Override
        public void commit(
            int layoutVersion,
            RenderTree current,
            RenderTree next,
            Object currentState,
            Object nextState) {}

        @Override
        public void commitToUI(RenderTree tree, Object o) {}
      };

  private Context context;
  private @Nullable RootHost rootHost;
  private @Nullable RenderTreeHost renderTreeHost;
  private @Nullable Node rootNode;
  private int widthSpec = DEFAULT_WIDTH_SPEC;
  private int heightSpec = DEFAULT_HEIGHT_SPEC;
  private RenderCoreExtension<?, ?>[] extensions;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          context = RuntimeEnvironment.application;
          base.evaluate();
        } finally {
          widthSpec = DEFAULT_WIDTH_SPEC;
          heightSpec = DEFAULT_HEIGHT_SPEC;
          rootHost = null;
          renderTreeHost = null;
          rootNode = null;
          extensions = null;
        }
      }
    };
  }

  /** Gets the current Android context. */
  public Context getContext() {
    return context;
  }

  /** Gets the current {@link RootHost}; the default is a {@link RootHostView}. */
  public RootHost getRootHost() {
    if (rootHost == null) {
      rootHost = new RootHostView(context);
    }

    return rootHost;
  }

  /** Gets the current {@link RenderTreeHost}; the default is a {@link RenderTreeHostView}. */
  public RenderTreeHost getRenderTreeHost() {
    if (renderTreeHost == null) {
      renderTreeHost = new RenderTreeHostView(context);
    }
    return renderTreeHost;
  }

  /** Gets the current root {@link Node}. */
  public @Nullable Node getRootNode() {
    return rootNode;
  }

  /** Gets the current width spec. */
  public int getWidthSpec() {
    return widthSpec;
  }

  /** Gets the current height spec. */
  public int getHeightSpec() {
    return heightSpec;
  }

  /** Sets a new root {@link Node} which should be rendered. */
  public RenderCoreTestRule useRootNode(Node rootNode) {
    this.rootNode = rootNode;
    return this;
  }

  /**
   * Sets a new {@link RootHost} which should render the next render result using {@link #render()}.
   */
  public RenderCoreTestRule useRootHost(RootHost rootHost) {
    this.rootHost = rootHost;
    checkRootHost();
    context = ((View) rootHost).getContext();
    return this;
  }

  /**
   * Sets a new {@link RenderTreeHost} which should render the next {@link RenderTree} using the
   * {@link #renderWithRenderTreeHost()} method.
   */
  public RenderCoreTestRule useRenderTreeHost(RenderTreeHost renderTreeHost) {
    this.renderTreeHost = renderTreeHost;
    checkRenderTreeHost();
    context = ((View) renderTreeHost).getContext();
    return this;
  }

  /**
   * Sets a new list of {@link RenderCoreExtension} which should be used for rendering the next
   * {@link RenderTree} using the {@link #render()} method.
   */
  public RenderCoreTestRule useExtensions(RenderCoreExtension[] extensions) {
    this.extensions = extensions;
    return this;
  }

  /** Sets the width and height that should be used to measure the {@link RootHost}. */
  public RenderCoreTestRule setSizePx(int widthPx, int heightPx) {
    widthSpec = makeMeasureSpec(widthPx, EXACTLY);
    heightSpec = makeMeasureSpec(heightPx, EXACTLY);
    return this;
  }

  /** Sets the width spec and height spec that should be used to measure the {@link RootHost}. */
  public RenderCoreTestRule setSizeSpecs(int widthSpec, int heightSpec) {
    this.widthSpec = widthSpec;
    this.heightSpec = heightSpec;
    return this;
  }

  /**
   * Renders the current root {@link Node} into the current {@link RootHost}. Test assertions on the
   * output by accessing the {@link RootHost} using {@link #getRootHost()}.
   */
  public RenderCoreTestRule render() {
    checkRootHost();
    final View rootHostView = (View) getRootHost();

    final RenderState renderState =
        new RenderState(rootHostView.getContext(), DELEGATE, null, extensions);
    renderState.setTree(new SimpleLazyTree(getRootNode()));
    getRootHost().setRenderState(renderState);

    rootHostView.measure(getWidthSpec(), getHeightSpec());
    rootHostView.layout(0, 0, rootHostView.getMeasuredWidth(), rootHostView.getMeasuredHeight());

    return this;
  }

  /**
   * Renders the current root {@link Node} into the current {@link RenderTreeHost}. Test assertions
   * on the output by accessing the {@link RenderTreeHost} using {@link #getRenderTreeHost()}.
   */
  public RenderCoreTestRule renderWithRenderTreeHost() {
    checkRenderTreeHost();
    final View rootHost = (View) getRenderTreeHost();

    final RenderResult renderResult =
        RenderResult.resolve(
            rootHost.getContext(),
            new SimpleLazyTree(getRootNode()),
            null,
            null,
            null,
            -1,
            getWidthSpec(),
            getHeightSpec());

    getRenderTreeHost().setRenderTree(renderResult.getRenderTree());

    rootHost.measure(getWidthSpec(), getHeightSpec());
    rootHost.layout(0, 0, rootHost.getMeasuredWidth(), rootHost.getMeasuredHeight());

    return this;
  }

  private void checkRootHost() {
    if (!(getRootHost() instanceof Host)) {
      throw new IllegalArgumentException("The RootHost must be a Host.");
    }
  }

  private void checkRenderTreeHost() {
    if (!(getRenderTreeHost() instanceof Host)) {
      throw new IllegalArgumentException("The RenderTreeHost must be a Host.");
    }
  }

  public static class SimpleLazyTree implements RenderState.LazyTree {

    private final Node root;

    public SimpleLazyTree(Node root) {
      this.root = root;
    }

    @Override
    public Pair<Node, Object> resolve() {
      return new Pair<>(root, null);
    }
  }
}
