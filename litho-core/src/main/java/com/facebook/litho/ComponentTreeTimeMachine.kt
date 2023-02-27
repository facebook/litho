// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho

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
