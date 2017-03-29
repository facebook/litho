// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.DataFlowGraph;
import com.facebook.litho.dataflow.GraphBinding;
import com.facebook.litho.dataflow.NumFramesNode;
import com.facebook.litho.dataflow.OutputOnlyNode;
import com.facebook.litho.dataflow.SimpleNode;
import com.facebook.litho.dataflow.UnitTestTimingSource;
import com.facebook.litho.dataflow.ValueNode;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

/**
 * Tests resolving a PendingNode to a ValueNode.
 */
@RunWith(ComponentsTestRunner.class)
public class PendingNodeTest {

  private UnitTestTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;

  private static class PendingNumFramesNode implements PendingNode<NumFramesNode> {
  }

  private static class TestResolver implements PendingNodeResolver {

    @Override
    public ValueNode resolve(PendingNode pendingNode) {
      if (pendingNode instanceof PendingNumFramesNode) {
        return new NumFramesNode();
      }
      throw new RuntimeException("Unknown node spec: " + pendingNode);
    }
  }

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new UnitTestTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testSimpleGraph() {
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode(123);

    GraphBinding graphBinding = GraphBinding.create(mDataFlowGraph);
    SimpleAnimationBinding animationBinding = new SimpleAnimationBinding(graphBinding);
    animationBinding.addPendingBinding(new PendingNumFramesNode(), middle);
    animationBinding.addBinding(middle, destination);

    animationBinding.resolvePendingNodes(new TestResolver());
    animationBinding.start();

    mTestTimingSource.step(1);

    assertEquals(1f, destination.getValue());

    mTestTimingSource.step(15);

    assertEquals(16f, destination.getValue());
  }
}
