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
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Exception class used to add additional Litho metadata to a crash. */
@Nullsafe(Nullsafe.Mode.LOCAL)
class LithoMetadataExceptionWrapper extends RuntimeException {

  @Nullable EventHandler<ErrorEvent> lastHandler;

  private final ArrayList<Component> mComponentLayoutStack = new ArrayList<>();
  private final HashMap<String, String> mCustomMetadata = new HashMap<>();
  private final @Nullable ComponentContext mComponentContext;
  private final @Nullable ComponentTree mComponentTree;

  LithoMetadataExceptionWrapper(Throwable cause) {
    this(null, null, cause);
  }

  LithoMetadataExceptionWrapper(@Nullable ComponentContext componentContext, Throwable cause) {
    this(componentContext, null, cause);
  }

  LithoMetadataExceptionWrapper(@Nullable ComponentTree componentTree, Throwable cause) {
    this(null, componentTree, cause);
  }

  private LithoMetadataExceptionWrapper(
      @Nullable ComponentContext componentContext,
      @Nullable ComponentTree componentTree,
      Throwable cause) {
    super();
    initCause(cause);
    setStackTrace(new StackTraceElement[0]);
    mComponentContext = componentContext;
    mComponentTree = componentTree;
  }

  void addComponentForLayoutStack(Component c) {
    mComponentLayoutStack.add(c);
  }

  void addCustomMetadata(String key, String value) {
    mCustomMetadata.put(key, value);
  }

  @Override
  public String getMessage() {
    final StringBuilder msg = new StringBuilder("crash context:\n");
    if (!mComponentLayoutStack.isEmpty()) {
      msg.append("  layout_stack: ");
      for (int i = mComponentLayoutStack.size() - 1; i >= 0; i--) {
        msg.append(mComponentLayoutStack.get(i).getSimpleName());
        if (i != 0) {
          msg.append(" -> ");
        }
      }
      msg.append("\n");
    }

    if (mComponentContext != null) {
      if (mComponentContext.getLogTag() != null) {
        msg.append("  log_tag: ").append(mComponentContext.getLogTag()).append("\n");
      }
    }

    final ComponentTree componentTree =
        mComponentTree != null
            ? mComponentTree
            : (mComponentContext != null ? mComponentContext.getComponentTree() : null);
    if (componentTree != null) {
      msg.append("  tree_root: ").append(componentTree.getRoot().getSimpleName()).append("\n");
    }

    msg.append("  thread_name: ").append(Thread.currentThread().getName()).append("\n");

    for (Map.Entry<String, String> entry : mCustomMetadata.entrySet()) {
      msg.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }

    return msg.toString().trim();
  }
}
