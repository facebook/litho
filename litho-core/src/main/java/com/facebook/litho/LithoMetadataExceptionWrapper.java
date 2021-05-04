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

import android.annotation.SuppressLint;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Assertions;
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Exception class used to add additional Litho metadata to a crash. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoMetadataExceptionWrapper extends RuntimeException {

  public static final String LITHO_CONTEXT = "Litho Context:";

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
  @SuppressLint({"BadMethodUse-java.lang.Class.getName", "ReflectionMethodUse"})
  public String getMessage() {
    final Throwable cause = getDeepestCause();
    final StringBuilder msg =
        new StringBuilder("Real Cause => ")
            .append(cause.getClass().getCanonicalName())
            .append(": ")
            .append(cause.getMessage())
            .append("\nLitho Context:\n");
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

    if (mComponentContext != null && mComponentContext.getLogTag() != null) {
      msg.append("  log_tag: ").append(mComponentContext.getLogTag()).append("\n");
    } else if (mComponentTree != null && mComponentTree.getLogTag() != null) {
      msg.append("  log_tag: ").append(mComponentTree.getLogTag()).append("\n");
    }

    final ComponentTree componentTree =
        mComponentTree != null
            ? mComponentTree
            : (mComponentContext != null ? mComponentContext.getComponentTree() : null);
    if (componentTree != null && componentTree.getRoot() != null) {
      msg.append("  tree_root: ").append(componentTree.getRoot().getSimpleName()).append("\n");
    }

    if (mComponentContext != null && mComponentContext.getComponentScope() != null) {
      msg.append("  component_scope: ")
          .append(mComponentContext.getComponentScope().getSimpleName())
          .append("\n");
    }

    msg.append("  thread_name: ").append(Thread.currentThread().getName()).append("\n");

    if (mComponentContext != null) {
      final DebugMetadata metadataFromTreeProps =
          mComponentContext.getTreeProp(DebugMetadata.class);
      if (metadataFromTreeProps != null) {
        appendMap(msg, metadataFromTreeProps.getMetadataMap());
      }
    }

    appendMap(msg, mCustomMetadata);

    return msg.toString().trim();
  }

  private Throwable getDeepestCause() {
    Throwable cause = Assertions.assertNotNull(getCause());
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause;
  }

  private static void appendMap(StringBuilder msg, Map<String, String> map) {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      msg.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }
  }
}
