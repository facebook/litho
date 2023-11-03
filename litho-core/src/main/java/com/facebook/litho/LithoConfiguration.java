// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho;

import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.RunnableHandler;
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer;

public final class LithoConfiguration {

  public ComponentsConfiguration mComponentsConfiguration;
  final boolean areTransitionsEnabled;
  public final boolean isReconciliationEnabled;
  final boolean isVisibilityProcessingEnabled;
  @Nullable final RunnableHandler mountContentPreallocationHandler;
  final boolean incrementalMountEnabled;
  final ErrorEventHandler errorEventHandler;
  final String logTag;
  @Nullable final ComponentsLogger logger;
  final RenderUnitIdGenerator renderUnitIdGenerator;
  @Nullable final VisibilityBoundsTransformer visibilityBoundsTransformer;
  @Nullable final ComponentTreeDebugEventListener debugEventListener;

  public final boolean preallocationPerMountContentEnabled;

  public LithoConfiguration(
      final ComponentsConfiguration config,
      boolean areTransitionsEnabled,
      boolean isReconciliationEnabled,
      boolean isVisibilityProcessingEnabled,
      boolean preallocationPerMountContentEnabled,
      @Nullable RunnableHandler mountContentPreallocationHandler,
      boolean incrementalMountEnabled,
      @Nullable ErrorEventHandler errorEventHandler,
      String logTag,
      @Nullable ComponentsLogger logger,
      RenderUnitIdGenerator renderUnitIdGenerator,
      @Nullable VisibilityBoundsTransformer visibilityBoundsTransformer,
      @Nullable ComponentTreeDebugEventListener debugEventListener) {
    this.mComponentsConfiguration = config;
    this.areTransitionsEnabled = areTransitionsEnabled;
    this.isReconciliationEnabled = isReconciliationEnabled;
    this.isVisibilityProcessingEnabled = isVisibilityProcessingEnabled;
    this.preallocationPerMountContentEnabled = preallocationPerMountContentEnabled;
    this.mountContentPreallocationHandler = mountContentPreallocationHandler;
    this.incrementalMountEnabled = incrementalMountEnabled;
    this.errorEventHandler =
        errorEventHandler == null ? DefaultErrorEventHandler.INSTANCE : errorEventHandler;
    this.logTag = logTag;
    this.logger = logger;
    this.renderUnitIdGenerator = renderUnitIdGenerator;
    this.visibilityBoundsTransformer = visibilityBoundsTransformer;
    this.debugEventListener = debugEventListener;
  }
}
