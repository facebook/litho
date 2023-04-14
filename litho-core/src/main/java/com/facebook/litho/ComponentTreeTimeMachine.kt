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

package com.facebook.litho

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import java.util.UUID

interface ComponentTreeTimeMachine {

  val originalRootName: String

  fun storeRevision(
      root: Component,
      treeState: TreeState,
      treeProps: TreeProps?,
      source: Int,
      attribution: String?
  )

  fun getCurrentRevision(): Revision

  fun restoreRevision(id: String)

  fun getRevisions(): List<Revision>

  @DataClassGenerate
  data class Revision
  internal constructor(
      val id: String,
      val version: Int,
      val root: Component,
      val treeState: TreeState,
      val treeProps: TreeProps?,
      val timestamp: Long,
      val source: Int,
      val attribution: String?
  )
}

internal class DebugComponentTreeTimeMachine(private val componentTree: ComponentTree) :
    ComponentTreeTimeMachine {

  /**
   * Whenever we restore one revision, the [ComponentTree] will attempt to commit an equivalent
   * revision to the restored one.
   *
   * Therefore we use a controlling mechanism to avoid storing a revision that was restored.
   */
  private var skipNextRevision: Boolean = false

  override val originalRootName: String
    get() =
        revisions.entries.firstOrNull()?.value?.root?.simpleName
            ?: error("There should be at least one revision")

  private val revisions = LinkedHashMap<String, ComponentTreeTimeMachine.Revision>()
  private var selectedRevisionId: String? = null

  override fun storeRevision(
      root: Component,
      treeState: TreeState,
      treeProps: TreeProps?,
      source: Int,
      attribution: String?
  ) {
    if (!skipNextRevision) {
      val revision =
          ComponentTreeTimeMachine.Revision(
              id = UUID.randomUUID().toString(),
              version = revisions.size,
              root = root,
              treeState = treeState,
              treeProps = treeProps,
              timestamp = System.currentTimeMillis(),
              source = source,
              attribution = attribution)

      revisions[revision.id] = revision
      selectedRevisionId = revision.id
    }

    skipNextRevision = false
  }

  override fun restoreRevision(id: String) {
    val revision = revisions[id] ?: error("There should be a revision with id $id")
    selectedRevisionId = id
    skipNextRevision = true
    componentTree.applyRevision(revision)
  }

  override fun getRevisions(): List<ComponentTreeTimeMachine.Revision> = revisions.values.toList()

  override fun getCurrentRevision(): ComponentTreeTimeMachine.Revision {
    return revisions[selectedRevisionId]
        ?: error("There should be a revision with id $selectedRevisionId")
  }
}
