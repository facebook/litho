// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.ValueNode;

/**
 * Represents the specification for a {@link ValueNode} which doesn't exist yet, for example for a
 * View property on a View that hasn't been mounted yet.
 *
 * This class is used in conjunction with {@link SimpleAnimationBinding#addBindingSpec}
 * and {@link PendingNodeResolver} to turn a spec into its corresponding {@link ValueNode} at
 * runtime and link it to another node.
 */
public interface PendingNode<T extends ValueNode> {
}
