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

import java.util.List;
import javax.annotation.Nullable;

/**
 * {@link DebugHierarchy} provides a light(er) weight way to track and access information about the
 * component parentage of a given {@link MountItem}. For a given {@link MountItem}, it provides
 * access to a linked list of {@link Class} objects representing the class of the {@link Component}
 * and each of it's hierarchy parents.
 */
public class DebugHierarchy {

  public static class Node {

    public final @Nullable Node parent;
    public final @Nullable Component component;
    public final @Nullable List<Component> components;
    public final @OutputUnitType int type;

    public Node(
        @Nullable Node parent,
        @Nullable Component component,
        @Nullable List<Component> components,
        @OutputUnitType int type) {
      this.parent = parent;
      this.component = component;
      this.components = components;
      this.type = type;
    }

    public Node mutateType(int type) {
      if (this.type == type) {
        return this;
      }

      return new Node(parent, component, components, type);
    }

    private void toHierarchyString(StringBuilder sb) {
      if (parent != null) {
        parent.toHierarchyString(sb);
      }
      if (components == null || components.isEmpty()) {
        sb.append("(no components)");
        sb.append(',');
        return;
      }
      for (int i = components.size() - 1; i >= 0; i--) {
        sb.append(components.get(i).getSimpleName());
        sb.append(',');
      }
    }

    public String toHierarchyString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      toHierarchyString(sb);
      if (sb.length() > 1) {
        sb.deleteCharAt(sb.length() - 1);
      }
      sb.append('}');
      return sb.toString();
    }
  }

  public static Node newNode(
      @Nullable Node parent, @Nullable Component component, @Nullable List<Component> components) {
    return new Node(parent, component, components, OutputUnitType.HOST);
  }

  private DebugHierarchy() {}

  public static int getMountItemCount(ComponentHost host) {
    return host.getMountItemCount();
  }

  public static Object getMountItemContent(ComponentHost host, int mountItemIndex) {
    return host.getMountItemAt(mountItemIndex).getContent();
  }

  public static @Nullable Node getMountItemHierarchy(ComponentHost host, int mountItemIndex) {
    return LayoutOutput.getLayoutOutput(host.getMountItemAt(mountItemIndex)).getHierarchy();
  }

  public static @Nullable String getOutputUnitTypeName(@OutputUnitType int type) {
    switch (type) {
      case OutputUnitType.CONTENT:
        return "CONTENT";
      case OutputUnitType.BACKGROUND:
        return "BACKGROUND";
      case OutputUnitType.FOREGROUND:
        return "FOREGROUND";
      case OutputUnitType.HOST:
        return "HOST";
      case OutputUnitType.BORDER:
        return "BORDER";
      default:
        return null;
    }
  }
}
