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

package com.facebook.litho.widget;

import androidx.annotation.Nullable;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.EventHandler;
import com.facebook.litho.RenderCompleteEvent;

/** {@link RenderInfo} that can render components. */
public class ComponentRenderInfo extends BaseRenderInfo {

  public static final String LAYOUT_DIFFING_ENABLED = "layout_diffing_enabled";
  public static final String RECONCILIATION_ENABLED = "is_reconciliation_enabled";

  private final Component mComponent;
  @Nullable private final EventHandler<RenderCompleteEvent> mRenderCompleteEventHandler;
  @Nullable private final ComponentsLogger mComponentsLogger;
  @Nullable private final String mLogTag;

  public static Builder create() {
    return new Builder();
  }

  private ComponentRenderInfo(Builder builder) {
    super(builder);

    if (builder.mComponent == null) {
      throw new IllegalStateException("Component must be provided.");
    }

    mComponent = builder.mComponent;
    mRenderCompleteEventHandler = builder.mRenderCompleteEventEventHandler;
    mComponentsLogger = builder.mComponentsLogger;
    mLogTag = builder.mLogTag;
  }

  /** Create empty {@link ComponentRenderInfo}. */
  public static RenderInfo createEmpty() {
    return create().component(new EmptyComponent()).build();
  }

  @Override
  public Component getComponent() {
    return mComponent;
  }

  @Override
  @Nullable
  public EventHandler<RenderCompleteEvent> getRenderCompleteEventHandler() {
    return mRenderCompleteEventHandler;
  }

  @Override
  @Nullable
  public ComponentsLogger getComponentsLogger() {
    return mComponentsLogger;
  }

  @Nullable
  @Override
  public String getLogTag() {
    return mLogTag;
  }

  @Override
  public boolean rendersComponent() {
    return true;
  }

  @Override
  public String getName() {
    return mComponent.getSimpleName();
  }

  public static class Builder extends BaseRenderInfo.Builder<Builder> {
    private Component mComponent;
    private EventHandler<RenderCompleteEvent> mRenderCompleteEventEventHandler;
    @Nullable private ComponentsLogger mComponentsLogger;
    @Nullable private String mLogTag;

    /** Specify {@link Component} that will be rendered as an item of the list. */
    public Builder component(Component component) {
      this.mComponent = component;
      return this;
    }

    public Builder renderCompleteHandler(
        EventHandler<RenderCompleteEvent> renderCompleteEventHandler) {
      this.mRenderCompleteEventEventHandler = renderCompleteEventHandler;
      return this;
    }

    public Builder component(Component.Builder builder) {
      return component(builder.build());
    }

    public Builder componentsLogger(@Nullable ComponentsLogger componentsLogger) {
      this.mComponentsLogger = componentsLogger;
      return this;
    }

    public Builder logTag(@Nullable String logTag) {
      this.mLogTag = logTag;
      return this;
    }

    public ComponentRenderInfo build() {
      return new ComponentRenderInfo(this);
    }
  }

  private static class EmptyComponent extends Component {

    protected EmptyComponent() {
      super("EmptyComponent");
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return Column.create(c).build();
    }

    @Override
    public boolean isEquivalentTo(Component other) {
      return EmptyComponent.this == other
          || (other != null && EmptyComponent.this.getClass() == other.getClass());
    }
  }
}
