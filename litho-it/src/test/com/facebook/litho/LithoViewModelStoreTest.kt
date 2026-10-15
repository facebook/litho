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

import android.os.Looper
import androidx.lifecycle.ViewModel
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(LithoTestRunner::class)
class LithoViewModelStoreTest {

  @Test
  fun `same key returns the same ViewModel`() {
    val store = LithoViewModelStore()

    val first =
        store.getOrCreate(TrackingViewModel::class, key = "model", factory = ::TrackingViewModel)
    val second =
        store.getOrCreate(TrackingViewModel::class, key = "model", factory = ::TrackingViewModel)

    assertThat(second).isSameAs(first)
    store.close()
  }

  @Test
  fun `different keys return different ViewModels`() {
    val store = LithoViewModelStore()

    val first =
        store.getOrCreate(TrackingViewModel::class, key = "first", factory = ::TrackingViewModel)
    val second =
        store.getOrCreate(TrackingViewModel::class, key = "second", factory = ::TrackingViewModel)

    assertThat(second).isNotSameAs(first)
    store.close()
  }

  @Test
  fun `different model classes use different default identities`() {
    val store = LithoViewModelStore()

    val first = store.getOrCreate(TrackingViewModel::class, factory = ::TrackingViewModel)
    val second = store.getOrCreate(OtherViewModel::class, factory = ::OtherViewModel)

    assertThat(second).isNotSameAs(first)
    store.close()
  }

  @Test
  fun `different stores isolate the same model identity`() {
    val firstStore = LithoViewModelStore()
    val secondStore = LithoViewModelStore()

    val first = firstStore.getOrCreate(TrackingViewModel::class, factory = ::TrackingViewModel)
    val second = secondStore.getOrCreate(TrackingViewModel::class, factory = ::TrackingViewModel)

    assertThat(second).isNotSameAs(first)
    firstStore.close()
    secondStore.close()
  }

  /**
   * ```text
   * Creator thread                  Waiting thread
   * --------------                  --------------
   * Install entry and start factory
   *                                 Request the same key and wait
   * Publish and signal
   *                                 Return the same ViewModel
   * ```
   */
  @Test
  fun `concurrent requests create one ViewModel`() {
    val store = LithoViewModelStore()
    val factoryStarted = CountDownLatch(1)
    val allowFactoryToFinish = CountDownLatch(1)
    val creations = AtomicInteger()
    val first =
        FutureTask<TrackingViewModel> {
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            creations.incrementAndGet()
            factoryStarted.countDown()
            check(allowFactoryToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            TrackingViewModel()
          }
        }
    val second =
        FutureTask<TrackingViewModel> {
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            creations.incrementAndGet()
            TrackingViewModel()
          }
        }
    val firstThread = Thread(first)
    val secondThread = Thread(second)

    try {
      firstThread.start()
      check(factoryStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      secondThread.start()

      allowFactoryToFinish.countDown()

      assertThat(second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
          .isSameAs(first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      assertThat(creations).hasValue(1)
    } finally {
      allowFactoryToFinish.countDown()
      firstThread.interrupt()
      secondThread.interrupt()
      store.close()
    }
  }

  /**
   * ```text
   * Background creator             Main-thread requester
   * ------------------             ---------------------
   * Start the factory
   *                                Request the same key
   *                                Wait for the creator
   * Publish and signal
   *                                Return the same model
   * ```
   */
  @Test
  fun `main thread request joins background creation`() {
    val store = LithoViewModelStore()
    val factoryStarted = CountDownLatch(1)
    val allowFactoryToFinish = CountDownLatch(1)
    val creations = AtomicInteger()
    val creator =
        FutureTask<TrackingViewModel> {
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            creations.incrementAndGet()
            factoryStarted.countDown()
            check(allowFactoryToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            TrackingViewModel()
          }
        }
    val creatorThread = Thread(creator)
    val requestingThread = Thread.currentThread()
    val releaseCreator = FutureTask {
      waitUntilWaiting(requestingThread)
      allowFactoryToFinish.countDown()
    }
    val releaserThread = Thread(releaseCreator)

    try {
      creatorThread.start()
      check(factoryStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      releaserThread.start()

      val requested =
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            error("The waiting request must not run its factory")
          }

      releaseCreator.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      assertThat(requested).isSameAs(creator.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      assertThat(creations).hasValue(1)
    } finally {
      allowFactoryToFinish.countDown()
      creatorThread.interrupt()
      releaserThread.interrupt()
      store.close()
    }
  }

  @Test
  fun `failed creation can be retried`() {
    val store = LithoViewModelStore()

    assertThatThrownBy {
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            error("creation failed")
          }
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("creation failed")

    assertThat(
        store.getOrCreate(
            TrackingViewModel::class,
            key = "model",
            factory = ::TrackingViewModel,
        ),
    )
        .isNotNull
    store.close()
  }

  @Test
  fun `factory cannot request another ViewModel`() {
    val store = LithoViewModelStore()

    assertThatThrownBy {
          store.getOrCreate(TrackingViewModel::class, key = "first") {
            store.getOrCreate(
                TrackingViewModel::class,
                key = "second",
                factory = ::TrackingViewModel,
            )
          }
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("must not request another ViewModel")

    store.close()
  }

  /**
   * ```text
   * Creator thread                  Waiting thread
   * --------------                  --------------
   * Install entry and start factory
   *                                 Request the same key and wait
   * Publish failure and signal
   *                                 Throw with the creator failure as the cause
   * ```
   */
  @Test
  fun `waiting request wraps failure from creating thread`() {
    val store = LithoViewModelStore()
    val factoryStarted = CountDownLatch(1)
    val allowFactoryToFail = CountDownLatch(1)
    val waiterStarted = CountDownLatch(1)
    val creator =
        FutureTask<TrackingViewModel> {
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            factoryStarted.countDown()
            check(allowFactoryToFail.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            error("creation failed")
          }
        }
    val waiter =
        FutureTask<TrackingViewModel> {
          waiterStarted.countDown()
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            error("The waiting request must not run its factory")
          }
        }
    val creatorThread = Thread(creator)
    val waiterThread = Thread(waiter)

    try {
      creatorThread.start()
      check(factoryStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      waiterThread.start()
      check(waiterStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      waitUntilWaiting(waiterThread)

      allowFactoryToFail.countDown()

      assertThatThrownBy { creator.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
          .hasCauseInstanceOf(IllegalStateException::class.java)
      assertThatThrownBy { waiter.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
          .hasCauseInstanceOf(IllegalStateException::class.java)
          .hasRootCauseMessage("creation failed")
    } finally {
      allowFactoryToFail.countDown()
      creatorThread.interrupt()
      waiterThread.interrupt()
      store.close()
    }
  }

  @Test
  fun `close clears ViewModels once and rejects later access`() {
    val store = LithoViewModelStore()
    val model =
        store.getOrCreate(
            TrackingViewModel::class,
            key = "model",
            factory = ::TrackingViewModel,
        )

    store.close()
    store.close()

    assertThat(model.clearCount).hasValue(1)
    assertThatThrownBy {
          store.getOrCreate(
              TrackingViewModel::class,
              key = "other",
              factory = ::TrackingViewModel,
          )
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("scope was cleared")
  }

  @Test
  fun `active calculation lease defers clearing until it closes`() {
    val store = LithoViewModelStore()
    val lease = checkNotNull(store.tryAcquireCalculationLease())
    val model = store.getOrCreate(TrackingViewModel::class, factory = ::TrackingViewModel)

    store.close()
    shadowOf(Looper.getMainLooper()).idle()

    assertThat(model.clearCount).hasValue(0)
    assertThat(store.tryAcquireCalculationLease()).isNull()

    lease.close()
    shadowOf(Looper.getMainLooper()).idle()

    assertThat(model.clearCount).hasValue(1)
  }

  @Test
  fun `last active calculation lease clears the store`() {
    val store = LithoViewModelStore()
    val firstLease = checkNotNull(store.tryAcquireCalculationLease())
    val secondLease = checkNotNull(store.tryAcquireCalculationLease())
    val model = store.getOrCreate(TrackingViewModel::class, factory = ::TrackingViewModel)

    store.close()
    firstLease.close()
    shadowOf(Looper.getMainLooper()).idle()
    assertThat(model.clearCount).hasValue(0)

    secondLease.close()
    shadowOf(Looper.getMainLooper()).idle()
    assertThat(model.clearCount).hasValue(1)
  }

  @Test
  fun `access without an active calculation lease is rejected after close is requested`() {
    val store = LithoViewModelStore()
    val leaseAcquired = CountDownLatch(1)
    val releaseLease = CountDownLatch(1)
    val worker = Thread {
      checkNotNull(store.tryAcquireCalculationLease()).use {
        leaseAcquired.countDown()
        check(releaseLease.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      }
    }

    try {
      worker.start()
      check(leaseAcquired.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      store.close()

      assertThatThrownBy {
            store.getOrCreate(TrackingViewModel::class, factory = ::TrackingViewModel)
          }
          .isInstanceOf(IllegalStateException::class.java)
          .hasMessageContaining("scope was cleared")
    } finally {
      releaseLease.countDown()
      worker.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
      store.close()
    }
  }

  /**
   * ```text
   * Resolve thread                 Owner thread
   * --------------                 ------------
   * Acquire a calculation lease
   * Start the ViewModel factory
   *                                Request store closure
   * Finish and use the ViewModel
   * Release the lease
   * Clear the ViewModel on the main thread
   * ```
   */
  @Test
  fun `active calculation lease lets creation finish before clearing`() {
    val store = LithoViewModelStore()
    val factoryStarted = CountDownLatch(1)
    val allowFactoryToFinish = CountDownLatch(1)
    val created = AtomicReference<TrackingViewModel>()
    val result =
        FutureTask<TrackingViewModel> {
          checkNotNull(store.tryAcquireCalculationLease()).use {
            store.getOrCreate(TrackingViewModel::class, key = "model") {
              TrackingViewModel().also { model ->
                created.set(model)
                factoryStarted.countDown()
                check(allowFactoryToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
              }
            }
          }
        }
    val worker = Thread(result)

    try {
      worker.start()
      check(factoryStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

      store.close()
      allowFactoryToFinish.countDown()

      assertThat(result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isSameAs(created.get())
      shadowOf(Looper.getMainLooper()).idle()
      assertThat(created.get().clearCount).hasValue(1)
    } finally {
      allowFactoryToFinish.countDown()
      worker.interrupt()
      store.close()
    }
  }

  /**
   * ```text
   * Worker thread                   Main thread
   * -------------                   -----------
   * Close the Litho store
   * Schedule AndroidX cleanup
   *                                 Clear the ViewModelStore and call onCleared()
   * ```
   */
  @Test
  fun `close from a background thread clears ViewModels on the main thread`() {
    val store = LithoViewModelStore()
    val model =
        store.getOrCreate(
            ThreadCapturingViewModel::class,
            key = "model",
            factory = ::ThreadCapturingViewModel,
        )
    val close = FutureTask { store.close() }
    val worker = Thread(close)

    try {
      worker.start()
      close.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      shadowOf(Looper.getMainLooper()).idle()

      assertThat(model.clearedOnMainThread).hasValue(true)
    } finally {
      worker.interrupt()
      store.close()
    }
  }

  /**
   * ```text
   * Creator thread                  Owner thread
   * --------------                  ------------
   * Start the factory
   *                                 Close the store and mark the entry closed
   * Finish the factory
   * Schedule cleanup for the unpublished ViewModel on the main thread
   * ```
   */
  @Test
  fun `close racing with creation clears the created ViewModel`() {
    val store = LithoViewModelStore()
    val factoryStarted = CountDownLatch(1)
    val allowFactoryToFinish = CountDownLatch(1)
    val created = AtomicReference<TrackingViewModel>()
    val result =
        FutureTask<TrackingViewModel> {
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            TrackingViewModel().also { model ->
              created.set(model)
              factoryStarted.countDown()
              check(allowFactoryToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            }
          }
        }
    val worker = Thread(result)

    try {
      worker.start()
      check(factoryStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

      store.close()
      allowFactoryToFinish.countDown()

      assertThatThrownBy { result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
          .hasCauseInstanceOf(IllegalStateException::class.java)
      shadowOf(Looper.getMainLooper()).idle()
      assertThat(created.get().clearCount).hasValue(1)
    } finally {
      allowFactoryToFinish.countDown()
      worker.interrupt()
      store.close()
    }
  }

  /**
   * Verifies cleanup while one request is creating a ViewModel and another is waiting for it.
   *
   * ```text
   * Creator thread        Waiting thread        Owner thread
   * --------------        --------------        ------------
   * Start the factory
   *                       Request the same key
   *                       Wait for completion
   *                                             Close the store
   *                       Wake with failure
   * Finish the factory
   * Clear the ViewModel on the main thread
   * ```
   */
  @Test
  fun `close wakes a request waiting for creation`() {
    val store = LithoViewModelStore()
    val factoryStarted = CountDownLatch(1)
    val allowFactoryToFinish = CountDownLatch(1)
    val waiterStarted = CountDownLatch(1)
    val created = AtomicReference<TrackingViewModel>()
    val creator =
        FutureTask<TrackingViewModel> {
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            TrackingViewModel().also { model ->
              created.set(model)
              factoryStarted.countDown()
              check(allowFactoryToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            }
          }
        }
    val waiter =
        FutureTask<TrackingViewModel> {
          waiterStarted.countDown()
          store.getOrCreate(TrackingViewModel::class, key = "model") {
            error("The waiting request must not run its factory")
          }
        }
    val creatorThread = Thread(creator)
    val waiterThread = Thread(waiter)

    try {
      creatorThread.start()
      check(factoryStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      waiterThread.start()
      check(waiterStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      waitUntilWaiting(waiterThread)

      store.close()

      assertThatThrownBy { waiter.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
          .hasCauseInstanceOf(IllegalStateException::class.java)

      allowFactoryToFinish.countDown()
      assertThatThrownBy { creator.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
          .hasCauseInstanceOf(IllegalStateException::class.java)
      shadowOf(Looper.getMainLooper()).idle()
      assertThat(created.get().clearCount).hasValue(1)
    } finally {
      allowFactoryToFinish.countDown()
      creatorThread.interrupt()
      waiterThread.interrupt()
      store.close()
    }
  }

  private class TrackingViewModel : ViewModel() {
    val clearCount = AtomicInteger()

    override fun onCleared() {
      clearCount.incrementAndGet()
    }
  }

  private class OtherViewModel : ViewModel()

  private class ThreadCapturingViewModel : ViewModel() {
    val clearedOnMainThread = AtomicReference<Boolean>()

    override fun onCleared() {
      clearedOnMainThread.set(Looper.myLooper() == Looper.getMainLooper())
    }
  }

  private companion object {
    const val TIMEOUT_SECONDS = 5L

    private fun waitUntilWaiting(thread: Thread) {
      val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
      while (thread.state != Thread.State.WAITING && System.nanoTime() < deadline) {
        Thread.yield()
      }
      assertThat(thread.state).isEqualTo(Thread.State.WAITING)
    }
  }
}
