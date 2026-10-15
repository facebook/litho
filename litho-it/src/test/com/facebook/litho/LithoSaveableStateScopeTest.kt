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

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

@RunWith(LithoTestRunner::class)
class LithoSaveableStateScopeTest {

  @Test
  fun `restores the registry snapshot through a recreated Android owner`() {
    val firstAndroidOwner = TestSavedStateOwner()
    val firstScope = createScope(firstAndroidOwner)
    val value = AtomicInteger(3)
    firstScope.registry.registerProvider("counter") { value.get() }
    value.set(8)

    val savedState = firstAndroidOwner.save()
    firstScope.close()
    firstAndroidOwner.destroy()
    val restoredAndroidOwner = TestSavedStateOwner(savedState)
    val restoredScope = createScope(restoredAndroidOwner)

    assertThat(restoredScope).isNotSameAs(firstScope)
    assertThat(restoredScope.registry.restoredValue("counter")?.value).isEqualTo(8)
  }

  @Test
  fun `same Android owner and key reuse the scope`() {
    val androidOwner = TestSavedStateOwner()

    val firstScope = createScope(androidOwner)
    val secondScope = createScope(androidOwner)

    assertThat(secondScope).isSameAs(firstScope)

    firstScope.close()
  }

  @Test
  fun `different keys create independent scopes`() {
    val androidOwner = TestSavedStateOwner()

    val firstScope = createScope(androidOwner, "first")
    val secondScope = createScope(androidOwner, "second")

    assertThat(secondScope.registry).isNotSameAs(firstScope.registry)

    firstScope.close()
    secondScope.close()
  }

  @Test
  fun `same owner and key reject a different class loader`() {
    val androidOwner = TestSavedStateOwner()
    val scope = createScope(androidOwner)

    assertThatThrownBy {
          getOrCreateLithoSaveableStateScope(
              androidOwner,
              OWNER_KEY,
              object : ClassLoader() {},
          )
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("same host Context class loader")

    scope.close()
  }

  @Test
  fun `withSaveableStateScope supplies the scope registry to the tree`() {
    val androidOwner = TestSavedStateOwner()
    val scope = createScope(androidOwner)
    val tree =
        ComponentTree.create(ComponentContext(RuntimeEnvironment.getApplication()))
            .withSaveableStateScope(scope)
            .build()

    assertThat(tree.acquireLithoSaveableStateRegistry()).isSameAs(scope.registry)

    tree.release()
    scope.close()
  }

  @Test
  fun `closing the scope prevents attaching it to another tree`() {
    val androidOwner = TestSavedStateOwner()
    val scope = createScope(androidOwner)
    val tree =
        ComponentTree.create(ComponentContext(RuntimeEnvironment.getApplication()))
            .withSaveableStateScope(scope)
            .build()
    val registry = checkNotNull(tree.acquireLithoSaveableStateRegistry())

    tree.release()
    scope.close()

    assertThatThrownBy { registry.snapshot() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("after its Litho scope was cleared")
    assertThatThrownBy {
          ComponentTree.create(ComponentContext(RuntimeEnvironment.getApplication()))
              .withSaveableStateScope(scope)
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("closed LithoSaveableStateScope")
  }

  @Test
  fun `recreating a closed scope starts with fresh state`() {
    val firstAndroidOwner = TestSavedStateOwner()
    val originalScope = createScope(firstAndroidOwner)
    originalScope.registry.registerProvider("counter") { 8 }
    val savedState = firstAndroidOwner.save()
    originalScope.close()
    firstAndroidOwner.destroy()
    val restoredAndroidOwner = TestSavedStateOwner(savedState)
    val restoredScope = createScope(restoredAndroidOwner)

    assertThat(restoredScope.registry.restoredValue("counter")?.value).isEqualTo(8)

    restoredScope.close()
    val replacementScope = createScope(restoredAndroidOwner)

    assertThat(replacementScope).isNotSameAs(restoredScope)
    assertThat(replacementScope.registry.restoredValue("counter")).isNull()
    replacementScope.close()
  }

  @Test
  fun `scope key cannot be empty`() {
    val androidOwner = TestSavedStateOwner()

    assertThatThrownBy { createScope(androidOwner, "") }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("key cannot be empty")
  }

  @Test
  fun `scope must be created after Android state restoration`() {
    val androidOwner = TestSavedStateOwner(restore = false)

    assertThatThrownBy { createScope(androidOwner) }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("after the Android owner's saved state is restored")
  }

  @Test
  fun `scope cannot be created after Android owner destruction`() {
    val androidOwner = TestSavedStateOwner()
    androidOwner.destroy()

    assertThatThrownBy { createScope(androidOwner) }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("after its Android owner was destroyed")
  }

  @Test
  fun `scope cannot be attached after Android owner destruction`() {
    val androidOwner = TestSavedStateOwner()
    val scope = createScope(androidOwner)
    androidOwner.destroy()

    assertThatThrownBy {
          ComponentTree.create(ComponentContext(RuntimeEnvironment.getApplication()))
              .withSaveableStateScope(scope)
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("after its Android owner was destroyed")

    scope.close()
  }

  private fun createScope(
      androidOwner: SavedStateRegistryOwner,
      key: String = OWNER_KEY,
  ): LithoSaveableStateScope = getOrCreateLithoSaveableStateScope(
      androidOwner,
      key,
      RuntimeEnvironment.getApplication().classLoader,
  )

  private class TestSavedStateOwner(
      restoredState: Bundle? = null,
      restore: Boolean = true,
  ) : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry

    init {
      controller.performAttach()
      if (restore) {
        controller.performRestore(restoredState)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
      }
    }

    fun save(): Bundle = Bundle().also(controller::performSave)

    fun destroy() {
      lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
  }

  private companion object {
    const val OWNER_KEY: String = "litho_test"
  }
}
