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

package com.facebook.litho.widget;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsReporter;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link RecyclerBinderUpdateCallback} */
@RunWith(ComponentsTestRunner.class)
public class RecyclerBinderUpdateCallbackTest {

  private static final int OLD_DATA_SIZE = 12;
  private static final int NEW_DATA_SIZE = 12;
  private static final String OBJECT_KEY = "objectKey";

  private List<Object> mOldData;
  private List<Object> mNewData;

  private ComponentContext mComponentContext;
  private RecyclerBinderUpdateCallback.ComponentRenderer mComponentRenderer;
  private RecyclerBinderUpdateCallback.OperationExecutor mOperationExecutor;
  private ComponentsReporter.Reporter mReporter;

  @Before
  public void setup() {
    mOldData = new ArrayList<>();
    for (int i = 0; i < OLD_DATA_SIZE; i++) {
      mOldData.add(new Object());
    }
    mNewData = new ArrayList<>();
    for (int i = 0; i < NEW_DATA_SIZE; i++) {
      mNewData.add(new Object());
    }

    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
    mComponentRenderer = new TestObjectRenderer(mComponentContext);
    mOperationExecutor = mock(RecyclerBinderUpdateCallback.OperationExecutor.class);
    doAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) {
                return null;
              }
            })
        .when(mOperationExecutor)
        .executeOperations(anyObject(), anyListOf(RecyclerBinderUpdateCallback.Operation.class));

    mReporter = mock(ComponentsReporter.Reporter.class);
    doAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) {
                return null;
              }
            })
        .when(mReporter)
        .emitMessage(eq(ComponentsReporter.LogLevel.ERROR), anyString());
    ComponentsReporter.provide(mReporter);
  }

  @Test
  public void testApplyChangeset() {
    RecyclerBinderUpdateCallback callback =
        RecyclerBinderUpdateCallback.acquire(
            0, mOldData, mComponentRenderer, mOperationExecutor, 0);
    callback.onInserted(0, OLD_DATA_SIZE);
    callback.applyChangeset(mComponentContext);
    verify(mReporter, never()).emitMessage(anyObject(), anyString());

    final List<RecyclerBinderUpdateCallback.Operation> operations = callback.getOperations();
    assertThat(operations.size()).isEqualTo(1);

    final RecyclerBinderUpdateCallback.Operation firstOperation = operations.get(0);
    assertThat(firstOperation.getType()).isEqualTo(RecyclerBinderUpdateCallback.Operation.INSERT);
    assertThat(firstOperation.getIndex()).isEqualTo(0);
    assertThat(firstOperation.getComponentContainers().size()).isEqualTo(OLD_DATA_SIZE);
  }

  @Test
  public void testApplyChangesetWithInValidOperations() {
    final RecyclerBinderUpdateCallback callback1 =
        RecyclerBinderUpdateCallback.acquire(
            0, mOldData, mComponentRenderer, mOperationExecutor, 0);
    callback1.onInserted(0, OLD_DATA_SIZE);
    callback1.applyChangeset(mComponentContext);
    verify(mReporter, never()).emitMessage(anyObject(), anyString());
    final RecyclerBinderUpdateCallback.Operation operation =
        (RecyclerBinderUpdateCallback.Operation) callback1.getOperations().get(0);
    assertOperationComponentContainer(operation, mOldData);

    final RecyclerBinderUpdateCallback callback2 =
        RecyclerBinderUpdateCallback.acquire(
            OLD_DATA_SIZE, mNewData, mComponentRenderer, mOperationExecutor, 0);

    // Apply invalid operations
    callback2.onChanged(7, 5, null);
    callback2.onChanged(5, 1, null);
    callback2.onMoved(4, 5);
    callback2.onChanged(5, 1, null);
    callback2.onMoved(6, 4);
    callback2.onChanged(2, 3, null);
    callback2.onMoved(1, 10);
    callback2.onChanged(10, 1, null);
    callback2.onRemoved(0, 1);
    callback2.applyChangeset(mComponentContext);
    verify(mReporter).emitMessage(eq(ComponentsReporter.LogLevel.ERROR), anyString());

    final List<RecyclerBinderUpdateCallback.Operation> operations = callback2.getOperations();
    assertThat(operations.size()).isEqualTo(2);

    final RecyclerBinderUpdateCallback.Operation firstOperation = operations.get(0);
    assertThat(firstOperation.getType()).isEqualTo(RecyclerBinderUpdateCallback.Operation.DELETE);
    assertThat(firstOperation.getIndex()).isEqualTo(0);
    assertThat(firstOperation.getToIndex()).isEqualTo(OLD_DATA_SIZE);

    final RecyclerBinderUpdateCallback.Operation secondOperation = operations.get(1);
    assertThat(secondOperation.getType()).isEqualTo(RecyclerBinderUpdateCallback.Operation.INSERT);
    assertThat(secondOperation.getIndex()).isEqualTo(0);
    assertThat(secondOperation.getComponentContainers().size()).isEqualTo(NEW_DATA_SIZE);
    assertOperationComponentContainer(secondOperation, mNewData);
  }

  private static void assertOperationComponentContainer(
      RecyclerBinderUpdateCallback.Operation operation, List<Object> data) {
    final List<RecyclerBinderUpdateCallback.ComponentContainer> containers =
        operation.getComponentContainers();
    for (int i = 0, size = data.size(); i < size; i++) {
      ComponentRenderInfo componentRenderInfo =
          (ComponentRenderInfo) containers.get(i).getRenderInfo();
      assertThat(componentRenderInfo.getCustomAttribute(OBJECT_KEY)).isEqualTo(data.get(i));
    }
  }

  private static class TestObjectRenderer
      implements RecyclerBinderUpdateCallback.ComponentRenderer<Object> {
    private final ComponentContext mComponentContext;

    TestObjectRenderer(ComponentContext componentContext) {
      mComponentContext = componentContext;
    }

    @Override
    public RenderInfo render(Object o, int idx) {
      return ComponentRenderInfo.create()
          .customAttribute(OBJECT_KEY, o)
          .component(EmptyComponent.create(mComponentContext))
          .build();
    }
  }
}
