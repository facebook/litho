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

package com.facebook.litho;

import androidx.annotation.Nullable;
import com.facebook.litho.yoga.LithoYogaFactory;
import com.facebook.yoga.YogaConfig;
import com.facebook.yoga.YogaNode;

/** A helper class that defines a configurable sizes for ComponentsPools. */
public class NodeConfig {

  public interface InternalYogaNodeFactory {
    @Nullable
    YogaNode create(YogaConfig config);
  }

  public interface InternalNodeFactory {
    InternalNode create(ComponentContext componentContext);

    InternalNode.NestedTreeHolder createNestedTreeHolder(
        ComponentContext c, @Nullable TreeProps props);
  }

  /**
   * Custom factory for Yoga nodes. Used to enable direct byte buffers to set Yoga style properties
   * (rather than JNI)
   */
  public static volatile @Nullable InternalYogaNodeFactory sYogaNodeFactory;

  /** Factory to create custom InternalNodes for Components. */
  public static volatile @Nullable InternalNodeFactory sInternalNodeFactory;

  private static final YogaConfig sYogaConfig = LithoYogaFactory.createYogaConfig();

  @Nullable
  static YogaNode createYogaNode() {
    final InternalYogaNodeFactory factory = sYogaNodeFactory;
    return factory != null
        ? factory.create(sYogaConfig)
        : LithoYogaFactory.createYogaNode(sYogaConfig);
  }

  /**
   * Toggles a Yoga setting on whether to print debug logs to adb.
   *
   * @param enable whether to print logs or not
   */
  public static synchronized void setPrintYogaDebugLogs(boolean enable) {
    sYogaConfig.setPrintTreeFlag(enable);
  }

  /** Allows access to the internal YogaConfig instance */
  public static YogaConfig getYogaConfig() {
    return sYogaConfig;
  }
}
