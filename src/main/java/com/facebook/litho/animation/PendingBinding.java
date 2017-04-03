// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.DataFlowBinding;
import com.facebook.litho.dataflow.GraphBinding;
import com.facebook.litho.dataflow.ValueNode;

/**
 * Represents the specification for a binding between {@link ValueNode}s that will be added at
 * runtime just as a {@link DataFlowBinding} is being activated.
 *
 * A PendingBinding is used instead of a regular binding (using {@link GraphBinding#addBinding})
 * when one of the ValueNodes in the binding doesn't exist yet. An example is binding to a View
 * property: at the time you want to declare the binding, the View may not even exist yet. This
 * gives an opportunity for the end developer to still declare a connection and then have the
 * connection actually get applied at a time when all relevant ValueNodes exist.
 */
class PendingBinding {

  /**
   * @return a new PendingBinding specifying a binding from a PendingNode to a resolved ValueNode.
   */
  public static PendingBinding create(
      PendingNode fromNodeSpec,
      ValueNode toNode,
      String name) {
    return new PendingBinding(fromNodeSpec, toNode, name, false);
  }

  /**
   * @return a new PendingBinding specifying a binding from a resolved ValueNode to a PendingNode.
   */
  public static PendingBinding create(
      ValueNode fromNode,
      PendingNode toPendingNode,
      String name) {
    return new PendingBinding(toPendingNode, fromNode, name, true);
  }

  /**
   * The PendingNode of this binding.
   */
  public final PendingNode pendingNode;

  /**
   * The resolved ValueNode of this binding.
   */
  public final ValueNode resolvedNode;

  /**
   * The name of the input into the 'to' node.
   */
  public final String inputName;

  /**
   * Whether the binding is from the resolved ValueNode to the PendingNode (if true), or
   * vice-versa (if false).
   */
  public final boolean isFromResolvedToPending;

  private PendingBinding(
      PendingNode pendingNode,
      ValueNode resolvedNode,
      String inputName,
      boolean isFromResolvedToPending) {
    this.pendingNode = pendingNode;
    this.resolvedNode = resolvedNode;
    this.inputName = inputName;
    this.isFromResolvedToPending = isFromResolvedToPending;
  }
}
