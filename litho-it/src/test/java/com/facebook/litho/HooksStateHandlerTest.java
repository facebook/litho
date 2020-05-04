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

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class HooksStateHandlerTest {

  @Test
  public void testCopyStateHandlerWithoutHooks() {
    final StateHandler first = new StateHandler();
    final StateHandler second = new StateHandler(first);

    assertThat(second.hasPendingUpdates()).isFalse();
    assertThat(second.isEmpty()).isTrue();
  }

  @Test
  public void testCopyStateHandlerWithHooks() {
    final Object bazState = new Object();
    final StateHandler first = new StateHandler();
    first.getHookState().put("foo", "test");
    first.getHookState().put("bar", 4);
    first.getHookState().put("baz", bazState);

    final StateHandler second = new StateHandler(first);

    assertThat(second.hasPendingUpdates()).isFalse();
    assertThat(second.isEmpty()).isFalse();
    assertThat(second.getHookState())
        .hasSize(3)
        .extracting("foo", "bar", "baz")
        .containsExactly("test", 4, bazState);
  }

  @Test
  public void testCopyStateHandlerWithPendingStateUpdate() {
    final Object bazState = new Object();
    final StateHandler first = new StateHandler();
    first.getHookState().put("foo", "test");
    first.getHookState().put("bar", 4);
    first.getHookState().put("baz", bazState);
    first.queueHookStateUpdate(
        new HookUpdater() {
          @Override
          public void apply(StateHandler stateHandler) {
            stateHandler.getHookState().put("newKey", "newValue");
            stateHandler.getHookState().put("bar", 5);
          }
        });

    assertThat(first.hasPendingUpdates()).isTrue();

    final StateHandler second = new StateHandler(first);

    assertThat(first.hasPendingUpdates()).isTrue();
    assertThat(second.hasPendingUpdates()).isFalse();

    assertThat(second.getHookState())
        .hasSize(4)
        .extracting("foo", "bar", "baz", "newKey")
        .containsExactly("test", 5, bazState, "newValue");

    first.commit(second);

    assertThat(first.hasPendingUpdates()).isFalse();
    assertThat(first.getHookState())
        .hasSize(4)
        .extracting("foo", "bar", "baz", "newKey")
        .containsExactly("test", 5, bazState, "newValue");
  }

  @Test
  public void testMultipleStateUpdates() {
    final Object bazState = new Object();
    final StateHandler first = new StateHandler();
    first.getHookState().put("foo", "test");
    first.getHookState().put("bar", 4);
    first.getHookState().put("baz", bazState);
    first.queueHookStateUpdate(
        new HookUpdater() {
          @Override
          public void apply(StateHandler stateHandler) {
            stateHandler.getHookState().put("newKey", "newValue");
            stateHandler.getHookState().put("bar", 5);
          }
        });

    first.queueHookStateUpdate(
        new HookUpdater() {
          @Override
          public void apply(StateHandler stateHandler) {
            stateHandler
                .getHookState()
                .put("bar", (int) stateHandler.getHookState().get("bar") + 1);
          }
        });

    assertThat(first.hasPendingUpdates()).isTrue();

    final StateHandler second = new StateHandler(first);

    assertThat(first.hasPendingUpdates()).isTrue();
    assertThat(second.hasPendingUpdates()).isFalse();

    assertThat(second.getHookState())
        .hasSize(4)
        .extracting("foo", "bar", "baz", "newKey")
        .containsExactly("test", 6, bazState, "newValue");

    first.commit(second);

    assertThat(first.hasPendingUpdates()).isFalse();
    assertThat(first.getHookState())
        .hasSize(4)
        .extracting("foo", "bar", "baz", "newKey")
        .containsExactly("test", 6, bazState, "newValue");
  }

  @Test
  public void testMultipleCommits() {
    final Object bazState = new Object();
    final StateHandler first = new StateHandler();
    first.getHookState().put("foo", "test");
    first.getHookState().put("bar", 4);
    first.getHookState().put("baz", bazState);
    first.queueHookStateUpdate(
        new HookUpdater() {
          @Override
          public void apply(StateHandler stateHandler) {
            stateHandler.getHookState().put("newKey", "newValue");
            stateHandler.getHookState().put("bar", 5);
          }
        });

    first.queueHookStateUpdate(
        new HookUpdater() {
          @Override
          public void apply(StateHandler stateHandler) {
            stateHandler
                .getHookState()
                .put("bar", (int) stateHandler.getHookState().get("bar") + 1);
          }
        });

    final StateHandler second = new StateHandler(first);

    first.queueHookStateUpdate(
        new HookUpdater() {
          @Override
          public void apply(StateHandler stateHandler) {
            stateHandler
                .getHookState()
                .put("bar", (int) stateHandler.getHookState().get("bar") + 1);
          }
        });

    final StateHandler third = new StateHandler(first);

    assertThat(second.getHookState())
        .hasSize(4)
        .extracting("foo", "bar", "baz", "newKey")
        .containsExactly("test", 6, bazState, "newValue");

    first.commit(second);

    assertThat(first.hasPendingUpdates()).isTrue();
    assertThat(first.getHookState())
        .hasSize(4)
        .extracting("foo", "bar", "baz", "newKey")
        .containsExactly("test", 6, bazState, "newValue");

    first.commit(third);

    assertThat(first.hasPendingUpdates()).isFalse();
    assertThat(first.getHookState())
        .hasSize(4)
        .extracting("foo", "bar", "baz", "newKey")
        .containsExactly("test", 7, bazState, "newValue");
  }
}
