/*
 * Copyright 2014-present Facebook, Inc.
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

import static com.facebook.litho.testing.ReflectionHelper.setFinalStatic;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateRecyclingTest {

  private int mUnspecifiedSizeSpec;

  @Mock
  private RecyclePool<InternalNode> mInternalNodePool;
  private RecyclePool<InternalNode> mOriginalInternalNodePool;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    mUnspecifiedSizeSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

    final Field declaredField = ComponentsPools.class.getDeclaredField("sInternalNodePool");
    mOriginalInternalNodePool = (RecyclePool<InternalNode>) declaredField.get(null);
    setFinalStatic(ComponentsPools.class, "sInternalNodePool", mInternalNodePool);
  }

  @After
  public void tearDown() throws NoSuchFieldException, IllegalAccessException {
    setFinalStatic(ComponentsPools.class, "sInternalNodePool", mOriginalInternalNodePool);
  }

  @Ignore("t23857601")
  @Test
  public void testNodeRecycling() throws Exception {
    // We want to verify that we never recycle a node with a non-null parent, since that would
    // mean that the parent retains a dangling reference to a recycled node.
    Mockito.doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                final InternalNode node = (InternalNode) invocation.getArguments()[0];
                assertThat(node.getParent())
                    .overridingErrorMessage("Internal node parent must be null before releasing")
                    .isNull();
                return null;
              }
            })
        .when(mInternalNodePool)
        .release(Matchers.any());

    // Create a layout state and release it.
    final Component input =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c).child(Column.create(c)).build();
          }
        };

    final LayoutState layoutState =
        LayoutState.calculate(
            new ComponentContext(RuntimeEnvironment.application),
            input,
            -1,
            mUnspecifiedSizeSpec,
            mUnspecifiedSizeSpec,
            LayoutState.CalculateLayoutSource.TEST);
    layoutState.releaseRef();

    // Verify that the nodes did get recycled
    verify(mInternalNodePool, atLeast(2)).release(Matchers.any());
  }
}
