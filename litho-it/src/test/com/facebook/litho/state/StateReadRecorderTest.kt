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

package com.facebook.litho.state

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.Test

class StateReadRecorderTest {

  @Test
  fun canRecordStateReads() {
    val state1 = newState(1)
    val state2 = newState(2)
    val reads =
        StateReadRecorder.record(TREE_ID) {
          StateReadRecorder.read(state1)
          StateReadRecorder.read(state2)
        }
    assertThat(reads.asSet()).containsOnly(state1, state2)
  }

  @Test
  fun supportsReentrantStateRecordsFromSameTree() {
    val state1 = newState(1)
    val state2 = newState(2)
    val state3 = newState(3)
    val reads =
        StateReadRecorder.record(TREE_ID) {
          StateReadRecorder.read(state1)
          StateReadRecorder.record(TREE_ID) {
            StateReadRecorder.read(state2)
            StateReadRecorder.read(state3)
          }
        }
    assertThat(reads.asSet()).containsOnly(state1, state2, state3)
  }

  @Test
  fun supportsNestedStateRecordsFromDifferentTree() {
    val throwable = catchThrowable {
      StateReadRecorder.record(TREE_ID) { StateReadRecorder.record(TREE_ID + 1) {} }
    }
    assertThat(throwable).isNull()
  }

  @Test
  fun canRecordSequentiallyFromDifferentTrees() {
    val state1 = newState(1, treeId = 10)
    val state2 = newState(2, treeId = 20)
    val state3 = newState(3, treeId = 30)
    val read1 = StateReadRecorder.record(10) { StateReadRecorder.read(state1) }
    val read2 = StateReadRecorder.record(20) { StateReadRecorder.read(state2) }
    val read3 = StateReadRecorder.record(30) { StateReadRecorder.read(state3) }
    assertThat(read1.asSet()).containsOnly(state1)
    assertThat(read2.asSet()).containsOnly(state2)
    assertThat(read3.asSet()).containsOnly(state3)
  }

  @Test
  fun canReadStateWithoutRecorder() {
    val state1 = newState(1)
    assertThat(catchThrowable { StateReadRecorder.read(state1) }).isNull()
  }

  @Test
  fun throwsWhenReadStateFromDifferentTree() {
    val state1 = newState(1, treeId = 10)
    assertThatThrownBy { StateReadRecorder.record(TREE_ID) { StateReadRecorder.read(state1) } }
        .isInstanceOf(IllegalStateException::class.java)
  }

  private fun newState(index: Int, treeId: Int = TREE_ID): StateId {
    return StateId(treeId, "globalKey", index)
  }

  companion object {
    private const val TREE_ID = 1
  }
}
