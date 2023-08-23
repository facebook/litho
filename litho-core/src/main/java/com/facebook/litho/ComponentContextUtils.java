/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer;

public class ComponentContextUtils {

  /**
   * Creates a new ComponentContext instance and sets the {@link ComponentTree} on the component.
   *
   * @param c context scoped to the parent component
   * @param componentTree component tree associated with the newly created context
   * @return a new ComponentContext instance
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  @Deprecated
  public static ComponentContext withComponentTree(
      ComponentContext c, ComponentTree componentTree) {
    final String contextLogTag = c.mLithoConfiguration.logTag;
    final ComponentsLogger contextLogger = c.mLithoConfiguration.logger;

    final ComponentTree.LithoConfiguration lithoConfiguration =
        (contextLogTag != null || contextLogger != null)
            ? mergeConfigurationWithNewLogTagAndLogger(
                componentTree.getLithoConfiguration(), contextLogTag, contextLogger)
            : componentTree.getLithoConfiguration();

    return withComponentTree(c, lithoConfiguration, componentTree);
  }

  static ComponentContext withComponentTree(
      ComponentContext c,
      ComponentTree.LithoConfiguration lithoConfiguration,
      ComponentTree componentTree) {
    final LithoTree lithoTree = LithoTree.Companion.create(componentTree);
    return withLithoTree(c, lithoConfiguration, lithoTree, componentTree.getLifecycleProvider());
  }

  static ComponentContext withLithoTree(
      ComponentContext c,
      ComponentTree.LithoConfiguration config,
      LithoTree lithoTree,
      @Nullable LithoLifecycleProvider lifecycleProvider) {
    return new ComponentContext(
        c.getAndroidContext(),
        c.getTreeProps(),
        config,
        lithoTree,
        c.mGlobalKey,
        lifecycleProvider,
        null,
        c.getParentTreeProps());
  }

  private static ComponentTree.LithoConfiguration mergeConfigurationWithNewLogTagAndLogger(
      ComponentTree.LithoConfiguration lithoConfiguration,
      @Nullable String logTag,
      @Nullable ComponentsLogger logger) {
    return new ComponentTree.LithoConfiguration(
        lithoConfiguration.mComponentsConfiguration,
        lithoConfiguration.areTransitionsEnabled,
        lithoConfiguration.isReconciliationEnabled,
        lithoConfiguration.isVisibilityProcessingEnabled,
        lithoConfiguration.preallocationPerMountContentEnabled,
        lithoConfiguration.mountContentPreallocationHandler,
        lithoConfiguration.incrementalMountEnabled,
        lithoConfiguration.errorEventHandler,
        logTag != null ? logTag : lithoConfiguration.logTag,
        logger != null ? logger : lithoConfiguration.logger,
        lithoConfiguration.renderUnitIdGenerator,
        lithoConfiguration.visibilityBoundsTransformer,
        lithoConfiguration.debugEventListener);
  }

  public static ComponentTree.LithoConfiguration buildDefaultLithoConfiguration(
      Context context,
      final @Nullable VisibilityBoundsTransformer transformer,
      @Nullable String logTag,
      @Nullable ComponentsLogger logger,
      int treeID) {
    ComponentsLogger loggerToUse =
        logger != null ? logger : ComponentsConfiguration.sComponentsLogger;

    String logTagToUse = logTag;
    if (logTag == null && logger == null && loggerToUse != null) {
      logTagToUse = "global-components-logger";
    }

    return new ComponentTree.LithoConfiguration(
        ComponentsConfiguration.getDefaultComponentsConfiguration(),
        AnimationsDebug.areTransitionsEnabled(context),
        ComponentsConfiguration.overrideReconciliation != null
            ? ComponentsConfiguration.overrideReconciliation
            : true,
        true,
        false,
        null,
        !ComponentsConfiguration.isIncrementalMountGloballyDisabled,
        DefaultErrorEventHandler.INSTANCE,
        logTagToUse,
        loggerToUse,
        treeID != ComponentTree.INVALID_ID
            ? new RenderUnitIdGenerator(treeID)
            : null, // TODO check if we can make this not nullable and always instantiate one
        transformer,
        null);
  }
}
