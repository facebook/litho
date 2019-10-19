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

package com.facebook.litho.widget;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsReporter;
import com.facebook.litho.DefaultComponentsReporter;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
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
        .executeOperations(
            any(ComponentContext.class), anyListOf(RecyclerBinderUpdateCallback.Operation.class));

    mReporter = mock(ComponentsReporter.Reporter.class);
    doAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) {
                return null;
              }
            })
        .when(mReporter)
        .emitMessage(eq(ComponentsReporter.LogLevel.ERROR), anyString(), anyString());
    ComponentsReporter.provide(mReporter);
  }

  @After
  public void tearDown() {
    ComponentsReporter.provide(new DefaultComponentsReporter());
  }

  @Test
  public void testApplyChangeset() {
    RecyclerBinderUpdateCallback callback =
        new RecyclerBinderUpdateCallback(null, mOldData, mComponentRenderer, mOperationExecutor);
    callback.onInserted(0, OLD_DATA_SIZE);
    callback.applyChangeset(mComponentContext);
    verify(mReporter, never())
        .emitMessage(any(ComponentsReporter.LogLevel.class), anyString(), anyString());

    final List<RecyclerBinderUpdateCallback.Operation> operations = callback.getOperations();
    assertThat(operations.size()).isEqualTo(1);

    final RecyclerBinderUpdateCallback.Operation firstOperation = operations.get(0);
    assertThat(firstOperation.getType()).isEqualTo(RecyclerBinderUpdateCallback.Operation.INSERT);
    assertThat(firstOperation.getIndex()).isEqualTo(0);
    assertThat(firstOperation.getComponentContainers().size()).isEqualTo(OLD_DATA_SIZE);

    assertThat(firstOperation.getDataContainers().size()).isEqualTo(OLD_DATA_SIZE);
    assertThat(firstOperation.getDataContainers().get(0).getPrevious()).isNull();
    for (int i = 0, size = firstOperation.getDataContainers().size(); i < size; i++) {
      assertThat(firstOperation.getDataContainers().get(i).getNext()).isEqualTo(mOldData.get(i));
    }
  }

  @Test
  public void testApplyChangesetWithMultiOperations() {
    List<Object> oldData = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      oldData.add("o" + i);
    }
    List<Object> newData = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      newData.add("n" + i);
    }
    RecyclerBinderUpdateCallback callback =
        new RecyclerBinderUpdateCallback(null, oldData, mComponentRenderer, mOperationExecutor);
    callback.onInserted(0, 12);
    callback.applyChangeset(mComponentContext);
    verify(mReporter, never())
        .emitMessage(any(ComponentsReporter.LogLevel.class), anyString(), anyString());

    final RecyclerBinderUpdateCallback callback2 =
        new RecyclerBinderUpdateCallback(oldData, newData, mComponentRenderer, mOperationExecutor);

    callback2.onInserted(0, 5);
    callback2.onChanged(6, 6, null);
    callback2.onInserted(17, 3);
    callback2.applyChangeset(mComponentContext);

    final List<RecyclerBinderUpdateCallback.Operation> operations = callback2.getOperations();
    assertThat(operations.size()).isEqualTo(3);

    final RecyclerBinderUpdateCallback.Operation firstOperation = operations.get(0);
    assertThat(firstOperation.getType()).isEqualTo(RecyclerBinderUpdateCallback.Operation.INSERT);
    assertThat(firstOperation.getIndex()).isEqualTo(0);
    assertThat(firstOperation.getComponentContainers().size()).isEqualTo(5);
    for (int i = 0, size = firstOperation.getDataContainers().size(); i < size; i++) {
      assertThat(firstOperation.getDataContainers().get(i).getNext()).isEqualTo(newData.get(i));
    }

    final RecyclerBinderUpdateCallback.Operation secondOperation = operations.get(1);
    assertThat(secondOperation.getType()).isEqualTo(RecyclerBinderUpdateCallback.Operation.UPDATE);
    assertThat(secondOperation.getIndex()).isEqualTo(6);
    assertThat(secondOperation.getComponentContainers().size()).isEqualTo(6);
    for (int i = 0, size = secondOperation.getDataContainers().size(); i < size; i++) {
      assertThat(secondOperation.getDataContainers().get(i).getNext())
          .isEqualTo(newData.get(i + 6));
      assertThat(secondOperation.getDataContainers().get(i).getPrevious())
          .isEqualTo(oldData.get(i + 1));
    }
  }

  @Test
  public void testApplyChangesetWithInValidOperations() {
    final RecyclerBinderUpdateCallback callback1 =
        new RecyclerBinderUpdateCallback(null, mOldData, mComponentRenderer, mOperationExecutor);
    callback1.onInserted(0, OLD_DATA_SIZE);
    callback1.applyChangeset(mComponentContext);
    verify(mReporter, never())
        .emitMessage(any(ComponentsReporter.LogLevel.class), anyString(), anyString());
    final RecyclerBinderUpdateCallback.Operation operation =
        (RecyclerBinderUpdateCallback.Operation) callback1.getOperations().get(0);
    assertOperationComponentContainer(operation, mOldData);

    assertThat(operation.getDataContainers().size()).isEqualTo(OLD_DATA_SIZE);
    for (int i = 0, size = operation.getDataContainers().size(); i < size; i++) {
      assertThat(operation.getDataContainers().get(i).getPrevious()).isNull();
      assertThat(operation.getDataContainers().get(i).getNext()).isEqualTo(mOldData.get(i));
    }

    final RecyclerBinderUpdateCallback callback2 =
        new RecyclerBinderUpdateCallback(
            mOldData, mNewData, mComponentRenderer, mOperationExecutor);

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
    verify(mReporter).emitMessage(eq(ComponentsReporter.LogLevel.ERROR), anyString(), anyString());

    final List<RecyclerBinderUpdateCallback.Operation> operations = callback2.getOperations();
    assertThat(operations.size()).isEqualTo(2);

    final RecyclerBinderUpdateCallback.Operation firstOperation = operations.get(0);
    assertThat(firstOperation.getType()).isEqualTo(RecyclerBinderUpdateCallback.Operation.DELETE);
    assertThat(firstOperation.getIndex()).isEqualTo(0);
    assertThat(firstOperation.getToIndex()).isEqualTo(OLD_DATA_SIZE);
    for (int i = 0, size = firstOperation.getDataContainers().size(); i < size; i++) {
      assertThat(firstOperation.getDataContainers().get(i).getPrevious())
          .isEqualTo(mOldData.get(i));
    }

    final RecyclerBinderUpdateCallback.Operation secondOperation = operations.get(1);
    assertThat(secondOperation.getType()).isEqualTo(RecyclerBinderUpdateCallback.Operation.INSERT);
    assertThat(secondOperation.getIndex()).isEqualTo(0);
    assertThat(secondOperation.getComponentContainers().size()).isEqualTo(NEW_DATA_SIZE);
    assertOperationComponentContainer(secondOperation, mNewData);
    assertThat(secondOperation.getDataContainers().size()).isEqualTo(NEW_DATA_SIZE);
    for (int i = 0, size = secondOperation.getDataContainers().size(); i < size; i++) {
      assertThat(secondOperation.getDataContainers().get(i).getNext()).isEqualTo(mNewData.get(i));
    }
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
