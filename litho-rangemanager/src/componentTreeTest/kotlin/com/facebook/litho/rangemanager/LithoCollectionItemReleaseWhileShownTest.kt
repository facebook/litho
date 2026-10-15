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

package com.facebook.litho.rangemanager

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.LithoView
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.SizeConstraints
import com.meta.rangemanager.AsynchronousKeyBasedRangeWorker
import com.meta.rangemanager.CollectionItemProvider
import com.meta.rangemanager.KeyBasedRange
import com.meta.rangemanager.KeyBasedRangeWorker
import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.PreparationHealthEvent
import com.meta.rangemanager.PreparationHealthObserver
import com.meta.rangemanager.PreparationSource
import com.meta.rangemanager.PrepareScope
import com.meta.rangemanager.UnprepareReason
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * A [LithoCollectionItem] its owner releases while a view shows its tree: an asynchronous worker
 * can run a release chosen before the row scrolled back and bound again.
 *
 * The item is released through a real worker on a background thread, the way the asynchronous
 * worker releases it, and the main looper then runs what the release posted. The view is attached
 * to an activity's window unless a test detaches it. Items keep a shown tree (`keepTreeWhileShown`)
 * unless a test says otherwise.
 */
@RunWith(LithoTestRunner::class)
class LithoCollectionItemReleaseWhileShownTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val window = FrameLayout(context)
  private val view = LithoView(context)
  private val builds = AtomicInteger(0)
  private val scope = PrepareScope()
  private val item = item()
  private val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)

  @Before
  fun setUp() {
    Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(window)
    window.addView(view)
    check(view.isAttachedToWindow)
  }

  @Test
  fun `a release while shown leaves the tree on the view`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val shownTree = checkNotNull(view.componentTree)

    releaseOnWorkerThread()

    assertThat(view.componentTree).isSameAs(shownTree)
    assertThat(shownTree.isReleased).isFalse()
  }

  @Test
  fun `a tree kept after its release is released on unbind`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val shownTree = checkNotNull(view.componentTree)
    releaseOnWorkerThread()

    item.unbind()

    assertThat(shownTree.isReleased).isTrue()
    assertThat(view.componentTree).isNull()
  }

  @Test
  fun `rebinding after a release while shown reuses the tree without building`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val shownTree = checkNotNull(view.componentTree)
    releaseOnWorkerThread()
    val buildsBefore = builds.get()

    item.bind(view)

    assertThat(view.componentTree).isSameAs(shownTree)
    assertThat(shownTree.isReleased).isFalse()
    assertThat(builds.get()).isEqualTo(buildsBefore)
  }

  @Test
  fun `a release after the view moved on releases the tree`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val firstTree = checkNotNull(view.componentTree)
    item().bind(view)

    releaseOnWorkerThread()

    assertThat(firstTree.isReleased).isTrue()
  }

  @Test
  fun `a release while unbound releases the tree`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val shownTree = checkNotNull(view.componentTree)
    item.unbind()

    releaseOnWorkerThread()

    assertThat(shownTree.isReleased).isTrue()
  }

  /** The item has left its owner, so nothing else would release this tree. */
  @Test
  fun `a tree built by a bind after the release is released on unbind`() {
    worker.schedule(Unit, Keys(KEY))
    releaseOnWorkerThread()
    item.bind(view)
    val boundTree = checkNotNull(view.componentTree)

    item.unbind()

    assertThat(boundTree.isReleased).isTrue()
  }

  @Test
  fun `an item prepared again after a release while shown keeps its tree`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val shownTree = checkNotNull(view.componentTree)
    releaseOnWorkerThread()
    worker.schedule(Unit, Keys(KEY))

    item.unbind()
    window.removeView(view)
    idleMain()

    assertThat(shownTree.isReleased).isFalse()
  }

  /** Its owner holds the item again, so another item taking the view must not release its tree. */
  @Test
  fun `an item prepared again after a release while shown keeps its tree when another item takes the view`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val shownTree = checkNotNull(view.componentTree)
    releaseOnWorkerThread()
    worker.schedule(Unit, Keys(KEY))

    item().bind(view)
    item.bind(view)

    assertThat(shownTree.isReleased).isFalse()
    assertThat(view.componentTree).isSameAs(shownTree)
  }

  /** A consumer that rebinds a view and unbinds the item it showed later, as Stories can. */
  @Test
  fun `a late unbind after another item took the view leaves the new tree on the view`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val next = item()
    next.bind(view)
    val nextTree = checkNotNull(view.componentTree)

    item.unbind()

    assertThat(view.componentTree).isSameAs(nextTree)
  }

  /** The same, after the first item's tree was kept by a release while shown. */
  @Test
  fun `a late unbind of a released item after another item took the view leaves the new tree on the view`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    releaseOnWorkerThread()
    val next = item()
    next.bind(view)
    val nextTree = checkNotNull(view.componentTree)

    item.unbind()

    assertThat(view.componentTree).isSameAs(nextTree)
  }

  @Test
  fun `without keepTreeWhileShown, a release while shown releases the tree`() {
    val unkeptScope = PrepareScope()
    val unkept = item(keepTreeWhileShown = false)
    val unkeptWorker = KeyBasedRangeWorker(providerOf(unkept, unkeptScope), unkeptScope)
    unkeptWorker.schedule(Unit, Keys(KEY))
    unkept.bind(view)
    val shownTree = checkNotNull(view.componentTree)

    val thread = Thread { unkeptWorker.schedule(Unit, Keys()) }
    thread.start()
    thread.join()
    idleMain()

    assertThat(shownTree.isReleased).isTrue()
    assertThat(view.componentTree).isNull()
  }

  /** As on master: unbind takes no lock, so it never waits on the main thread for a prepare. */
  @Test
  fun `without keepTreeWhileShown, unbind does not wait for a prepare on another thread`() {
    val blocking = BlockingPrepare(keepTreeWhileShown = false)
    blocking.item.bind(view)

    assertThat(blocking.mainThreadMsWhilePreparing { blocking.item.unbind() })
        .isLessThan(MAIN_THREAD_WAIT_MS)
  }

  /** A released item's unbind decides what to release without waiting for its prepare. */
  @Test
  fun `unbind does not wait for a prepare on another thread`() {
    val blocking = BlockingPrepare(keepTreeWhileShown = true)
    blocking.item.bind(view)
    blocking.releaseOnWorkerThread()

    assertThat(blocking.mainThreadMsWhilePreparing { blocking.item.unbind() })
        .isLessThan(MAIN_THREAD_WAIT_MS)
  }

  /**
   * An item whose preparation blocks inside its data provider, on a worker thread, after the first
   * prepare, so a test can time main-thread calls made while that preparation holds the item.
   */
  private inner class BlockingPrepare(keepTreeWhileShown: Boolean) {
    private val inPrepare = CountDownLatch(1)
    private val finishPrepare = CountDownLatch(1)
    @Volatile private var blocks = false
    private val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = {
          if (blocks) {
            inPrepare.countDown()
            finishPrepare.await(AWAIT_SECONDS, TimeUnit.SECONDS)
          }
          1
        },
        component = { _ -> Row() },
        sizeConstraints = SizeConstraints.exact(100, 100),
        traceTag = "test",
        keepTreeWhileShown = keepTreeWhileShown,
    )
    private val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)

    init {
      worker.schedule(Unit, Keys(KEY))
    }

    fun releaseOnWorkerThread() {
      val thread = Thread { worker.schedule(Unit, Keys()) }
      thread.start()
      thread.join()
      idleMain()
    }

    /** Runs [mainThreadCall] while a prepare on another thread holds the item; returns its ms. */
    fun mainThreadMsWhilePreparing(mainThreadCall: () -> Unit): Long {
      blocks = true
      val preparing = Thread { worker.schedule(Unit, Keys(KEY)) }
      preparing.start()
      try {
        check(inPrepare.await(AWAIT_SECONDS, TimeUnit.SECONDS))
        // Lets a blocked call return, so a failure is a slow call rather than a hang.
        val returned = CountDownLatch(1)
        val watchdog = Thread {
          if (!returned.await(MAIN_THREAD_WAIT_MS, TimeUnit.MILLISECONDS)) {
            finishPrepare.countDown()
          }
        }
        watchdog.start()
        val startNs = System.nanoTime()
        mainThreadCall()
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        returned.countDown()
        watchdog.join()
        return elapsedMs
      } finally {
        finishPrepare.countDown()
        preparing.join(AWAIT_SECONDS * 1000)
        idleMain()
      }
    }
  }

  /** As on master: a release with no tree drops the component, so the next prepare builds it. */
  @Test
  fun `without keepTreeWhileShown, a release after a failed tree build drops the component`() {
    var failTreeBuild = true
    val failing = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ ->
          builds.incrementAndGet()
          Row()
        },
        sizeConstraints = SizeConstraints.exact(100, 100),
        traceTag = "test",
        treeConfigurator = {
          if (failTreeBuild) {
            failTreeBuild = false
            throw IllegalStateException("tree build failed")
          }
        },
    )
    val failingScope = PrepareScope()
    val failingProvider = providerOf(failing, failingScope)
    val failingWorker = KeyBasedRangeWorker(failingProvider, failingScope)
    runCatching { failingWorker.schedule(Unit, Keys(KEY)) }
    check(builds.get() == 1)

    failingProvider.invalidate(KEY)
    failingWorker.schedule(Unit, Keys(KEY))

    assertThat(builds.get()).isEqualTo(2)
  }

  /** The option's contract: the consumer unbinds, and until it does the view keeps its tree. */
  @Test
  fun `a view left bound keeps its tree until it is unbound, wherever the view is`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val shownTree = checkNotNull(view.componentTree)
    releaseOnWorkerThread()

    window.removeView(view)
    idleMain()
    assertThat(shownTree.isReleased).isFalse()

    item.unbind()
    assertThat(shownTree.isReleased).isTrue()
  }

  /**
   * The release holds the item while unbind runs, so unbind's own check can't take it. The release
   * then sees the view gone and releases the tree, or checks again on the main thread.
   */
  @Test
  fun `a release that races unbind still releases the tree`() {
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val shownTree = checkNotNull(view.componentTree)
    val releasing = Thread { worker.schedule(Unit, Keys()) }

    // The release saves the tree's state through the scope, so holding the scope stops it there,
    // inside the item's lock, until unbind has run.
    synchronized(scope) {
      releasing.start()
      awaitBlocked(releasing)
      item.unbind()
    }
    releasing.join(AWAIT_SECONDS * 1000)
    idleMain()

    assertThat(shownTree.isReleased).isTrue()
  }

  @Test
  fun `with the asynchronous worker, a release while shown leaves the tree on the view`() {
    val prepared = CountDownLatch(1)
    val unprepared = CountDownLatch(1)
    val asyncScope = PrepareScope()
    val asyncItem = item()
    val asyncWorker = AsynchronousKeyBasedRangeWorker(
        itemProvider = providerOf(asyncItem, asyncScope),
        scope = asyncScope,
        observer = LatchingObserver(prepared, unprepared),
    )
    try {
      asyncWorker.schedule(Unit, Keys(KEY))
      assertThat(prepared.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      idleMain()
      asyncItem.bind(view)
      val shownTree = checkNotNull(view.componentTree)

      asyncWorker.schedule(Unit, Keys())
      assertThat(unprepared.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      idleMain()

      assertThat(view.componentTree).isSameAs(shownTree)
      assertThat(shownTree.isReleased).isFalse()
      asyncItem.unbind()
      assertThat(shownTree.isReleased).isTrue()
    } finally {
      asyncWorker.destroy()
    }
  }

  private fun item(
      keepTreeWhileShown: Boolean = true,
  ): LithoCollectionItem<Int> = LithoCollectionItem(
      context = context,
      dataProvider = { 1 },
      component = { _ ->
        builds.incrementAndGet()
        Row()
      },
      sizeConstraints = SizeConstraints.exact(100, 100),
      traceTag = "test",
      keepTreeWhileShown = keepTreeWhileShown,
  )

  /** Releases [KEY] off the main thread, then runs what the release posted to the main thread. */
  private fun releaseOnWorkerThread() {
    val thread = Thread { worker.schedule(Unit, Keys()) }
    thread.start()
    thread.join()
    idleMain()
  }

  private fun awaitBlocked(thread: Thread) {
    val deadline = System.nanoTime() + AWAIT_SECONDS * 1_000_000_000
    while (thread.state != Thread.State.BLOCKED) {
      check(System.nanoTime() < deadline) { "Thread did not block in time" }
      Thread.sleep(1)
    }
  }

  private fun idleMain() {
    shadowOf(Looper.getMainLooper()).idle()
  }

  private fun providerOf(item: PreparableCollectionItem, scope: PrepareScope) =
      object : CollectionItemProvider<String>(scope = scope) {
        override fun create(key: String): PreparableCollectionItem = item

        override fun contains(key: String): Boolean = true
      }

  private class Keys(override val keys: List<String>) : KeyBasedRange<String> {
    constructor(vararg keys: String) : this(keys.toList())
  }

  private class LatchingObserver(
      private val prepared: CountDownLatch,
      private val unprepared: CountDownLatch,
  ) : PreparationHealthObserver {
    override fun onItemRegistered(key: Any) = Unit

    override fun onItemPrepared(key: Any, source: PreparationSource) = prepared.countDown()

    override fun onItemBound(key: Any, wasPrepared: Boolean) = Unit

    override fun onItemUnprepared(key: Any, reason: UnprepareReason) = unprepared.countDown()

    override fun onFirstScroll() = Unit

    override fun reportEvent(key: Any, event: PreparationHealthEvent) = Unit
  }

  private companion object {
    const val KEY = "row"
    const val AWAIT_SECONDS = 5L
    const val MAIN_THREAD_WAIT_MS = 300L
  }
}
