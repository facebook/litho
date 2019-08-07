/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import com.facebook.yoga.YogaConfig;
import com.facebook.yoga.YogaNode;
import javax.annotation.Nullable;

/** A helper class that defines a configurable sizes for ComponentsPools. */
public class NodeConfig {

  public interface YogaNodeFactory {
    @Nullable
    YogaNode create(YogaConfig config);
  }

  public interface InternalNodeFactory {
    InternalNode create(ComponentContext componentContext);
  }

  /**
   * Custom factory for Yoga nodes. Used to enable direct byte buffers to set Yoga style properties
   * (rather than JNI)
   */
  public static volatile @Nullable YogaNodeFactory sYogaNodeFactory = null;

  /** Factory to create custom InternalNodes for Components. */
  public static volatile @Nullable InternalNodeFactory sInternalNodeFactory = null;

  private static final YogaConfig sYogaConfig = new YogaConfig();
  private static final Object sYogaConfigLock = new Object();

  static {
    sYogaConfig.setUseWebDefaults(true);
  }

  @Nullable
  static YogaNode createYogaNode() {
    return sYogaNodeFactory != null
        ? sYogaNodeFactory.create(sYogaConfig)
        : YogaNode.create(sYogaConfig);
  }

  /**
   * Toggles a Yoga setting on whether to print debug logs to adb.
   *
   * @param enable whether to print logs or not
   */
  public static void setPrintYogaDebugLogs(boolean enable) {
    synchronized (sYogaConfigLock) {
      sYogaConfig.setPrintTreeFlag(enable);
    }
  }

  /** Allows access to the internal YogaConfig instance */
  public static YogaConfig getYogaConfig() {
    return sYogaConfig;
  }

}
