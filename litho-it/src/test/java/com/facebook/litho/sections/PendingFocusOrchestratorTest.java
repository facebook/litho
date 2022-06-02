/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.sections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SmoothScrollAlignmentType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class PendingFocusOrchestratorTest {

  private PendingFocusOrchestrator mFocusOrchestrator;
  private FocusDispatcher mFocusDispatcher;

  @Before
  public void setup() {
    mFocusDispatcher = Mockito.mock(FocusDispatcher.class);
    mFocusOrchestrator = new PendingFocusOrchestrator(mFocusDispatcher);
  }

  @Test
  public void testInsertBeforePendingScrollAdjustsScrollPosition() {
    mFocusOrchestrator.registerPendingFocus(5, 0, null);

    mFocusOrchestrator.registerInsert(1, 2);

    mFocusOrchestrator.requestFocus();
    verify(mFocusDispatcher).requestFocusWithOffset(7, 0);
  }

  @Test
  public void testInsertOnPendingScrollPositionAdjustScrollPosition() {
    mFocusOrchestrator.registerPendingFocus(5, 0, null);

    mFocusOrchestrator.registerInsert(5, 2);

    mFocusOrchestrator.requestFocus();
    verify(mFocusDispatcher).requestFocusWithOffset(7, 0);
  }

  @Test
  public void testInsertAfterPendingScrollPositionDoesNotChangeScrollPosition() {
    mFocusOrchestrator.registerPendingFocus(5, 0, null);

    mFocusOrchestrator.registerInsert(6, 2);

    mFocusOrchestrator.requestFocus();

    verify(mFocusDispatcher).requestFocusWithOffset(5, 0);
  }

  @Test
  public void testMoveFromPendingScrollPositionUpdatesScrollPosition() {
    mFocusOrchestrator.registerPendingFocus(5, 0, null);

    mFocusOrchestrator.registerMove(5, 2);

    mFocusOrchestrator.requestFocus();

    verify(mFocusDispatcher).requestFocusWithOffset(2, 0);
  }

  @Test
  public void testMoveToPendingScrollPositionUpdatesScrollPosition() {
    mFocusOrchestrator.registerPendingFocus(5, 0, null);

    mFocusOrchestrator.registerMove(2, 5);

    mFocusOrchestrator.requestFocus();
    verify(mFocusDispatcher).requestFocusWithOffset(2, 0);
  }

  @Test
  public void testMoveNotOnPendingScrollPositionDoesNotUpdateScrollPosition() {
    mFocusOrchestrator.registerPendingFocus(5, 0, null);

    mFocusOrchestrator.registerMove(3, 2);

    mFocusOrchestrator.requestFocus();
    verify(mFocusDispatcher).requestFocusWithOffset(5, 0);
  }

  @Test
  public void testDeleteAfterPendingScrollPositionDoesNotUpdateScrollPosition() {
    mFocusOrchestrator.registerPendingFocus(5, 0, null);

    mFocusOrchestrator.registerDelete(6, 2);

    mFocusOrchestrator.requestFocus();
    verify(mFocusDispatcher).requestFocusWithOffset(5, 0);
  }

  @Test
  public void testDeleteBeforePendingScrollPositionUpdatesScrollPosition() {
    mFocusOrchestrator.registerPendingFocus(5, 0, null);

    mFocusOrchestrator.registerDelete(3, 2);

    mFocusOrchestrator.requestFocus();
    verify(mFocusDispatcher).requestFocusWithOffset(3, 0);
  }

  @Test
  public void testDeletePendingScrollPositionDeletesPendingScrollPosition() {
    mFocusOrchestrator.registerPendingFocus(5, 0, null);

    mFocusOrchestrator.registerDelete(4, 2);

    mFocusOrchestrator.requestFocus();
    verify(mFocusDispatcher, never()).requestFocusWithOffset(anyInt(), anyInt());
  }

  @Test
  public void testDispatchesPendingScrollToPositionCorrectly() {
    mFocusOrchestrator.registerPendingFocus(5, 100, SmoothScrollAlignmentType.SNAP_TO_ANY);

    mFocusOrchestrator.requestFocus();
    verify(mFocusDispatcher).requestSmoothFocus(5, 100, SmoothScrollAlignmentType.SNAP_TO_ANY);
  }

  @Test
  public void testDispatchesPendingScrollToIdCorrectly() {
    mFocusOrchestrator.registerPendingFocus("my-id", 100, SmoothScrollAlignmentType.SNAP_TO_ANY);

    mFocusOrchestrator.requestFocus();

    verify(mFocusDispatcher)
        .requestSmoothFocus("my-id", 100, SmoothScrollAlignmentType.SNAP_TO_ANY);
  }

  @Test
  public void testConsecutiveDispatches() {
    mFocusOrchestrator.registerPendingFocus("my-id", 100, SmoothScrollAlignmentType.SNAP_TO_ANY);
    mFocusOrchestrator.requestFocus();

    verify(mFocusDispatcher)
        .requestSmoothFocus("my-id", 100, SmoothScrollAlignmentType.SNAP_TO_ANY);

    mFocusOrchestrator.registerPendingFocus(5, 100, SmoothScrollAlignmentType.SNAP_TO_ANY);
    mFocusOrchestrator.requestFocus();

    verify(mFocusDispatcher).requestSmoothFocus(5, 100, SmoothScrollAlignmentType.SNAP_TO_ANY);
  }
}
