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

import android.os.BadParcelableException
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LithoSaveableStateRegistryTest {

  @Test
  fun `snapshot restores a registered value`() {
    val registry = LithoSaveableStateRegistry()
    val value = AtomicInteger(3)
    registry.registerProvider("counter") { value.get() }

    val restored = LithoSaveableStateRegistry(registry.snapshot())

    assertThat(restored.restoredValue("counter")?.value).isEqualTo(3)
  }

  @Test
  fun `unregister discards a provider that its owner did not retain`() {
    val registry = LithoSaveableStateRegistry()
    val value = AtomicInteger(1)
    val registration = registry.registerProvider("counter") { value.get() }
    value.set(4)

    registration.close()

    assertThat(registry.restoredValue("counter")).isNull()
    assertThat(registry.snapshot().isEmpty).isTrue()
  }

  @Test
  fun `owner can retain active values before replacing its tree`() {
    val registry = LithoSaveableStateRegistry()
    val value = AtomicInteger(1)
    val registration = registry.registerProvider("counter") { value.get() }
    value.set(4)

    registry.retainActiveValues()
    registration.close()

    assertThat(registry.restoredValue("counter")?.value).isEqualTo(4)
  }

  /**
   * ```text
   * Retention thread                 Component cleanup
   * ----------------                 -----------------
   * Capture active provider
   * Run provider outside lock
   *                                  Unregister provider
   * Provider returns
   * Do not retain removed value
   * ```
   */
  @Test
  fun `retention does not restore a concurrently removed provider`() {
    val registry = LithoSaveableStateRegistry()
    val providerStarted = CountDownLatch(1)
    val allowProviderToFinish = CountDownLatch(1)
    val calls = AtomicInteger()
    val registration =
        registry.registerProvider("counter") {
          if (calls.incrementAndGet() > 1) {
            providerStarted.countDown()
            check(allowProviderToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
          }
          4
        }
    val retain = FutureTask { registry.retainActiveValues() }
    val retentionThread = Thread(retain)

    try {
      retentionThread.start()
      check(providerStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      registration.close()
      allowProviderToFinish.countDown()
      retain.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

      assertThat(registry.restoredValue("counter")).isNull()
      assertThat(registry.snapshot().isEmpty).isTrue()
    } finally {
      allowProviderToFinish.countDown()
      retentionThread.interrupt()
      registry.close()
    }
  }

  @Test
  fun `registering a provider consumes its restored value`() {
    val original = LithoSaveableStateRegistry()
    original.registerProvider("counter") { 2 }
    val restored = LithoSaveableStateRegistry(original.snapshot())

    restored.registerProvider("counter") { 5 }

    assertThat(restored.restoredValue("counter")).isNull()
    assertThat(
        LithoSaveableStateRegistry(restored.snapshot()).restoredValue("counter")?.value,
    )
        .isEqualTo(5)
  }

  @Test
  fun `snapshot does not share retained value bundles`() {
    val source = LithoSaveableStateRegistry()
    source.registerProvider("counter") { 2 }
    val registry = LithoSaveableStateRegistry(source.snapshot())

    val snapshot = registry.snapshot()
    snapshot.getBundle("counter")?.clear()

    assertThat(registry.restoredValue("counter")?.value).isEqualTo(2)
    source.close()
    registry.close()
  }

  @Test
  fun `malformed restored value is ignored`() {
    val restoredState = Bundle().apply { putInt("counter", 3) }
    val registry = LithoSaveableStateRegistry(restoredState)

    assertThat(registry.restoredValue("counter")).isNull()
    assertThat(registry.snapshot().containsKey("counter")).isFalse()
  }

  @Test
  fun `restored value that cannot be deserialized is ignored`() {
    val restoredState = parcelRoundTrip(
        Bundle().apply {
          putBundle(
              "counter",
              Bundle().apply { putParcelable("value", UnreadableParcelable()) },
          )
        },
    )
    val registry = LithoSaveableStateRegistry(
        restoredState,
        LithoSaveableStateRegistryTest::class.java.classLoader,
    )

    assertThat(registry.restoredValue("counter")).isNull()
    assertThat(registry.snapshot().containsKey("counter")).isFalse()
  }

  @Test
  fun `closed registration cannot remove a replacement provider`() {
    val registry = LithoSaveableStateRegistry()
    val first = registry.registerProvider("counter") { 1 }
    first.close()
    registry.registerProvider("counter") { 2 }

    first.close()

    val restored = LithoSaveableStateRegistry(registry.snapshot())
    assertThat(restored.restoredValue("counter")?.value).isEqualTo(2)
  }

  /**
   * ```text
   * Registration thread A           Registration thread B
   * ---------------------           ---------------------
   * Register provider for key
   *                                 Register provider for the same key
   * Keep the registration           Fail instead of replacing A
   * ```
   */
  @Test
  fun `concurrent registration keeps one provider per key`() {
    val registry = LithoSaveableStateRegistry()
    val start = CountDownLatch(1)
    val registered = AtomicInteger()
    val rejected = AtomicInteger()
    val tasks =
        List(2) { value ->
          FutureTask {
            check(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            try {
              registry.registerProvider("shared") { value }
              registered.incrementAndGet()
            } catch (_: IllegalStateException) {
              rejected.incrementAndGet()
            }
          }
        }
    val threads = tasks.map(::Thread)

    try {
      threads.forEach(Thread::start)
      start.countDown()
      tasks.forEach { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }

      assertThat(registered).hasValue(1)
      assertThat(rejected).hasValue(1)
    } finally {
      threads.forEach(Thread::interrupt)
      registry.close()
    }
  }

  /**
   * ```text
   * Registration thread             Teardown thread
   * -------------------             ---------------
   * Validate provider value
   *                                 Close registry
   * Attempt to commit provider
   * Reject the ended state scope
   * ```
   */
  @Test
  fun `close rejects a registration that has not committed`() {
    val registry = LithoSaveableStateRegistry()
    val providerStarted = CountDownLatch(1)
    val allowProviderToFinish = CountDownLatch(1)
    val registration = FutureTask {
      assertThatThrownBy {
            registry.registerProvider("counter") {
              providerStarted.countDown()
              check(allowProviderToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
              1
            }
          }
          .isInstanceOf(IllegalStateException::class.java)
          .hasMessageContaining("scope was cleared")
    }
    val registrationThread = Thread(registration)

    try {
      registrationThread.start()
      check(providerStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      registry.close()
      allowProviderToFinish.countDown()

      registration.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } finally {
      allowProviderToFinish.countDown()
      registrationThread.interrupt()
      registry.close()
    }
  }

  /**
   * ```text
   * Snapshot thread                 Registration thread
   * ---------------                 -------------------
   * Capture provider list
   * Run provider outside lock       Register another provider
   * Finish snapshot                 Complete without waiting for saver
   * ```
   */
  @Test
  fun `snapshot does not hold the registry lock while a provider runs`() {
    val registry = LithoSaveableStateRegistry()
    val providerStarted = CountDownLatch(1)
    val allowProviderToFinish = CountDownLatch(1)
    val calls = AtomicInteger()
    registry.registerProvider("counter") {
      if (calls.incrementAndGet() > 1) {
        providerStarted.countDown()
        check(allowProviderToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      }
      7
    }
    val snapshot = FutureTask { registry.snapshot() }
    val registerOther = FutureTask { registry.registerProvider("other") { 2 } }
    val snapshotThread = Thread(snapshot)
    val registrationThread = Thread(registerOther)

    try {
      snapshotThread.start()
      check(providerStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      registrationThread.start()

      val otherRegistration = registerOther.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      allowProviderToFinish.countDown()

      val restored = LithoSaveableStateRegistry(snapshot.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))

      assertThat(restored.restoredValue("counter")?.value).isEqualTo(7)
      assertThat(calls).hasValue(2)
      otherRegistration.close()
    } finally {
      allowProviderToFinish.countDown()
      snapshotThread.interrupt()
      registrationThread.interrupt()
      registry.close()
    }
  }

  @Test
  fun `unsupported value is rejected when its provider registers`() {
    val registry = LithoSaveableStateRegistry()

    assertThatThrownBy { registry.registerProvider("unsupported") { Any() } }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("unsupported")
        .hasMessageContaining(Any::class.java.name)
        .hasMessageContaining("Saver")
    assertThat(registry.snapshot().isEmpty).isTrue()
  }

  @Test
  fun `unsupported updated value reports its owning key during snapshot`() {
    val registry = LithoSaveableStateRegistry()
    var value: Any = 1
    registry.registerProvider("counter") { value }
    value = Any()

    assertThatThrownBy { registry.snapshot() }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("counter")
        .hasMessageContaining(Any::class.java.name)
  }

  @Test
  fun `close is idempotent and rejects later access`() {
    val registry = LithoSaveableStateRegistry()
    registry.registerProvider("counter") { 1 }

    registry.close()
    registry.close()

    assertThatThrownBy { registry.snapshot() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("scope was cleared")
    assertThatThrownBy { registry.registerProvider("other") { 2 } }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("scope was cleared")
  }

  private companion object {
    const val TIMEOUT_SECONDS: Long = 5

    fun parcelRoundTrip(bundle: Bundle): Bundle {
      val bytes =
          Parcel.obtain().let { parcel ->
            try {
              parcel.writeBundle(bundle)
              parcel.marshall()
            } finally {
              parcel.recycle()
            }
          }
      return Parcel.obtain().let { parcel ->
        try {
          parcel.unmarshall(bytes, 0, bytes.size)
          parcel.setDataPosition(0)
          checkNotNull(parcel.readBundle(LithoSaveableStateRegistryTest::class.java.classLoader))
        } finally {
          parcel.recycle()
        }
      }
    }
  }

  @android.annotation.SuppressLint("MigrateToUsingParcelizePlugin")
  private class UnreadableParcelable() : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
      parcel.writeInt(1)
    }

    override fun describeContents(): Int = 0

    private constructor(parcel: Parcel) : this() {
      parcel.readInt()
      throw BadParcelableException("Test value cannot be deserialized")
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<UnreadableParcelable> =
          object : Parcelable.Creator<UnreadableParcelable> {
            override fun createFromParcel(parcel: Parcel): UnreadableParcelable =
                UnreadableParcelable(parcel)

            override fun newArray(size: Int): Array<UnreadableParcelable?> = arrayOfNulls(size)
          }
    }
  }
}
