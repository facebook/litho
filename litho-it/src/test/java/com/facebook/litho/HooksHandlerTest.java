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

/** Test for {@link HooksHandler}. */
@RunWith(LithoTestRunner.class)
public class HooksHandlerTest {

  @Test
  public void copyHandler_copyingHandlerWithHooks_copiesAllHookValues() {
    final Object fooState = new Object();
    final Object barState = new Object();
    final HooksHandler first = new HooksHandler();
    final Hooks hooks1 = first.getOrCreate("foo");
    hooks1.add("test");
    hooks1.add(4);
    hooks1.add(fooState);

    final Hooks hooks2 = first.getOrCreate("bar");
    hooks2.add(7);
    hooks2.add(barState);
    hooks2.add("hello");
    final HooksHandler second = new HooksHandler(first);

    assertThat(second.getHooksContainer()).hasSize(2);
    assertThat(second.getHooksContainer().get("foo").getMemoizedValues())
        .containsExactly("test", 4, fooState);
    assertThat(second.getHooksContainer().get("bar").getMemoizedValues())
        .containsExactly(7, barState, "hello");
  }

  @Test
  public void commit_committingHandlerWithNewValues_appliesNewValues() {
    final Object fooState = new Object();
    final Object barState = new Object();
    final HooksHandler first = new HooksHandler();
    final Hooks hooks1 = first.getOrCreate("foo");
    hooks1.add("test");
    hooks1.add(4);
    hooks1.add(fooState);

    final Hooks hooks2 = first.getOrCreate("bar");
    hooks2.add(7);
    hooks2.add(barState);
    hooks2.add("hello");
    final HooksHandler second = new HooksHandler(first);

    final Object bazState = new Object();
    final Hooks hooks3 = second.getOrCreate("baz");
    hooks3.add("newValue");
    hooks3.add(bazState);
    hooks3.add(13);

    first.commit(second);

    assertThat(first.getHooksContainer()).hasSize(3);
    assertThat(first.getHooksContainer().get("foo").getMemoizedValues())
        .containsExactly("test", 4, fooState);
    assertThat(first.getHooksContainer().get("bar").getMemoizedValues())
        .containsExactly(7, barState, "hello");
    assertThat(first.getHooksContainer().get("baz").getMemoizedValues())
        .containsExactly("newValue", bazState, 13);
  }

  @Test
  public void
      commit_copyingHandlerWithPendingStateUpdateAndCommittingIt_copiesAllOldHookValuesAndAppliesStateUpdate() {
    final Object fooState = new Object();
    final Object barState = new Object();
    final HooksHandler first = new HooksHandler();
    final Hooks hooks1 = first.getOrCreate("foo");
    hooks1.add("test");
    hooks1.add(4);
    hooks1.add(fooState);

    final Hooks hooks2 = first.getOrCreate("bar");
    hooks2.add(7);
    hooks2.add(barState);
    hooks2.add("hello");

    first.queueHookStateUpdate(
        new HookUpdater<HooksHandler>() {
          @Override
          public void apply(HooksHandler hooksHandler) {
            hooksHandler.getOrCreate("baz").add("newValue");
            hooksHandler.getOrCreate("foo").set(1, 5);
          }
        });

    assertThat(first.hasPendingUpdates()).isTrue();

    final HooksHandler second = new HooksHandler(first);

    assertThat(first.hasPendingUpdates()).isTrue();
    assertThat(second.hasPendingUpdates()).isFalse();

    assertThat(second.getHooksContainer()).hasSize(3);
    assertThat(second.getHooksContainer().get("foo").getMemoizedValues())
        .describedAs("4 is updated to 5 after State update")
        .containsExactly("test", 5, fooState);
    assertThat(second.getHooksContainer().get("bar").getMemoizedValues())
        .containsExactly(7, barState, "hello");
    assertThat(second.getHooksContainer().get("baz").getMemoizedValues())
        .describedAs("newValue is added after State update")
        .containsExactly("newValue");

    first.commit(second);

    assertThat(first.hasPendingUpdates()).isFalse();
    assertThat(first.getHooksContainer()).hasSize(3);
    assertThat(first.getHooksContainer().get("foo").getMemoizedValues())
        .describedAs("4 is updated to 5 after commit")
        .containsExactly("test", 5, fooState);
    assertThat(first.getHooksContainer().get("bar").getMemoizedValues())
        .containsExactly(7, barState, "hello");
    assertThat(first.getHooksContainer().get("baz").getMemoizedValues())
        .describedAs("newValue is added after commit")
        .containsExactly("newValue");
  }

  @Test
  public void
      commit_copyingHandlerWithMultiplePendingStateUpdatesAndCommittingIt_copiesAllOldHookValuesAndAppliesAllStateUpdates() {
    final Object fooState = new Object();
    final Object barState = new Object();
    final HooksHandler first = new HooksHandler();
    final Hooks hooks1 = first.getOrCreate("foo");
    hooks1.add("test");
    hooks1.add(4);
    hooks1.add(fooState);

    final Hooks hooks2 = first.getOrCreate("bar");
    hooks2.add(7);
    hooks2.add(barState);
    hooks2.add("hello");

    first.queueHookStateUpdate(
        new HookUpdater<HooksHandler>() {
          @Override
          public void apply(HooksHandler hooksHandler) {
            hooksHandler.getOrCreate("baz").add("newValue");
            hooksHandler.getOrCreate("foo").set(1, 5);
          }
        });

    first.queueHookStateUpdate(
        new HookUpdater<HooksHandler>() {
          @Override
          public void apply(HooksHandler hooksHandler) {
            final Hooks hooks = hooksHandler.getOrCreate("foo");
            hooks.set(1, (int) hooks.get(1) + 1);
          }
        });

    final HooksHandler second = new HooksHandler(first);

    assertThat(first.hasPendingUpdates()).isTrue();
    assertThat(second.hasPendingUpdates()).isFalse();

    assertThat(second.getHooksContainer()).hasSize(3);
    assertThat(second.getHooksContainer().get("foo").getMemoizedValues())
        .describedAs("4 is updated to 6 after two State updates")
        .containsExactly("test", 6, fooState);
    assertThat(second.getHooksContainer().get("bar").getMemoizedValues())
        .containsExactly(7, barState, "hello");
    assertThat(second.getHooksContainer().get("baz").getMemoizedValues())
        .describedAs("newValue is added after State update")
        .containsExactly("newValue");

    first.commit(second);

    assertThat(first.hasPendingUpdates()).isFalse();
    assertThat(first.getHooksContainer()).hasSize(3);
    assertThat(first.getHooksContainer().get("foo").getMemoizedValues())
        .describedAs("4 is updated to 6 after commit")
        .containsExactly("test", 6, fooState);
    assertThat(first.getHooksContainer().get("bar").getMemoizedValues())
        .containsExactly(7, barState, "hello");
    assertThat(first.getHooksContainer().get("baz").getMemoizedValues())
        .describedAs("newValue is added after commit")
        .containsExactly("newValue");
  }

  @Test
  public void
      commit_committingHandlerFirstWithPartOfStateUpdatesAndSecondTimeWithAllStateUpdates_atFirstAppliesOnlyStateUpdatesFromFirstCommitAndKeepOthersPendingAndThenAppliesAllStateUpdates() {
    final Object fooState = new Object();
    final Object barState = new Object();
    final HooksHandler first = new HooksHandler();
    final Hooks hooks1 = first.getOrCreate("foo");
    hooks1.add("test");
    hooks1.add(4);
    hooks1.add(fooState);

    final Hooks hooks2 = first.getOrCreate("bar");
    hooks2.add(7);
    hooks2.add(barState);
    hooks2.add("hello");

    first.queueHookStateUpdate(
        new HookUpdater<HooksHandler>() {
          @Override
          public void apply(HooksHandler hooksHandler) {
            hooksHandler.getOrCreate("baz").add("newValue");
            hooksHandler.getOrCreate("foo").set(1, 5);
          }
        });

    final HooksHandler second = new HooksHandler(first);

    first.queueHookStateUpdate(
        new HookUpdater<HooksHandler>() {
          @Override
          public void apply(HooksHandler hooksHandler) {
            final Hooks hooks = hooksHandler.getOrCreate("foo");
            hooks.set(1, (int) hooks.get(1) + 1);
          }
        });

    final HooksHandler third = new HooksHandler(first);

    assertThat(second.getHooksContainer()).hasSize(3);
    assertThat(second.getHooksContainer().get("foo").getMemoizedValues())
        .describedAs("4 is updated to 5 after State update")
        .containsExactly("test", 5, fooState);
    assertThat(second.getHooksContainer().get("bar").getMemoizedValues())
        .containsExactly(7, barState, "hello");
    assertThat(second.getHooksContainer().get("baz").getMemoizedValues())
        .describedAs("newValue is added after State update")
        .containsExactly("newValue");

    first.commit(second);

    assertThat(first.hasPendingUpdates())
        .describedAs("Update that was enqueued after `second` was created should still be pending")
        .isTrue();
    assertThat(first.getHooksContainer()).hasSize(3);
    assertThat(first.getHooksContainer().get("foo").getMemoizedValues())
        .describedAs("4 is updated to 5 after commit from `second` hooksHandler")
        .containsExactly("test", 5, fooState);
    assertThat(first.getHooksContainer().get("bar").getMemoizedValues())
        .containsExactly(7, barState, "hello");
    assertThat(first.getHooksContainer().get("baz").getMemoizedValues())
        .describedAs("newValue is added after commit from `second` hooksHandler")
        .containsExactly("newValue");

    first.commit(third);

    assertThat(first.hasPendingUpdates()).describedAs("All updates should be committed").isFalse();
    assertThat(first.getHooksContainer()).hasSize(3);
    assertThat(first.getHooksContainer().get("foo").getMemoizedValues())
        .describedAs("5 is updated to 6 after commit from `third` hooksHandler")
        .containsExactly("test", 6, fooState);
    assertThat(first.getHooksContainer().get("bar").getMemoizedValues())
        .containsExactly(7, barState, "hello");
    assertThat(first.getHooksContainer().get("baz").getMemoizedValues())
        .containsExactly("newValue");
  }
}
