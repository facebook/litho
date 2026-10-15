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

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalLithoApi::class)
@SuppressLint("KComponentSyncUpdateDetector")
@RunWith(LithoTestRunner::class)
class LithoSaveableStateTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `state is forgotten after its component leaves the tree`() {
    val first = AtomicReference<State<Int>>()
    val restored = AtomicReference<State<Int>>()
    val registry = LithoSaveableStateRegistry()
    val testLithoView = render(createTree(registry, CapturingStateComponent(onState = first::set)))

    first.get().updateSync(4)
    testLithoView.setRoot(EmptyComponent())
    testLithoView.setRoot(CapturingStateComponent(onState = restored::set))

    assertThat(restored.get().value).isEqualTo(0)
    testLithoView.release()
    registry.close()
  }

  @Test
  fun `two declarations of the same type remain independent`() {
    val first = AtomicReference<State<Int>>()
    val second = AtomicReference<State<Int>>()
    val registry = LithoSaveableStateRegistry()
    val testLithoView = render(createTree(registry, TwoStateComponent(first::set, second::set)))

    first.get().updateSync(3)

    assertThat(first.get().value).isEqualTo(3)
    assertThat(second.get().value).isEqualTo(0)
    testLithoView.release()
    registry.close()
  }

  @Test
  fun `two component instances retain independent state`() {
    val first = AtomicReference<State<Int>>()
    val second = AtomicReference<State<Int>>()
    val registry = LithoSaveableStateRegistry()
    val testLithoView = render(
        createTree(
            registry,
            TwoComponentInstances(first::set, second::set),
        ),
    )

    first.get().updateSync(5)

    assertThat(first.get().value).isEqualTo(5)
    assertThat(second.get().value).isEqualTo(0)
    testLithoView.release()
    registry.close()
  }

  @Test
  fun `changing an explicit key initializes different logical state`() {
    val first = AtomicReference<State<Int>>()
    val second = AtomicReference<State<Int>>()
    val registry = LithoSaveableStateRegistry()
    val testLithoView = render(
        createTree(
            registry,
            CapturingStateComponent(key = "first", onState = first::set),
        ),
    )
    first.get().updateSync(8)

    testLithoView.setRoot(CapturingStateComponent(key = "second", onState = second::set))

    assertThat(second.get().value).isEqualTo(0)
    testLithoView.release()
    registry.close()
  }

  @Test
  fun `unhosted tree rejects saveable state`() {
    val tree = ComponentTree.create(lithoTestRule.context).build()
    val sizeSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY)

    assertThatThrownBy {
          tree.setRootAndSizeSpecSync(
              CapturingStateComponent(onState = {}),
              sizeSpec,
              sizeSpec,
          )
        }
        .isInstanceOf(LithoMetadataExceptionWrapper::class.java)
        .hasRootCauseInstanceOf(IllegalStateException::class.java)
        .hasStackTraceContaining("No LithoSaveableStateRegistry")
        .hasStackTraceContaining("lifecycle-backed Litho host")
    tree.release()
  }

  @Test
  fun `structural and legacy global keys produce the same persistent identity`() {
    val rootTypeId = Component.getOrCreateId(PersistentRootComponent::class.java)
    val childTypeId = Component.getOrCreateId(PersistentChildComponent::class.java)
    val structuralKey = DefaultGlobalKey(
        key = childTypeId,
        position = 2,
        parent = DefaultGlobalKey(key = rootTypeId, position = 0, parent = null),
    )
    val legacyKey = StringGlobalKey("$rootTypeId,$childTypeId!2")
    val hookKey =
        HookKey.create(
            callSiteId = 4,
            index = 1,
            parent = HookKey.create(callSiteId = 2, index = 0, parent = null, intern = false),
            intern = false,
        )

    val structural = saveableStateKey(structuralKey, hookKey, "photo/7#!", "child")
    val legacy = saveableStateKey(legacyKey, hookKey, "photo/7#!", "child")

    assertThat(structural).isEqualTo(legacy)
    assertThat(structural).contains(PersistentRootComponent::class.java.name)
    assertThat(structural).contains(PersistentChildComponent::class.java.name)
  }

  @Test
  fun `persistent identity does not collide when runtime global key hashes collide`() {
    val firstTypeId = Component.getOrCreateId(PersistentFirstComponent::class.java)
    val secondTypeId = Component.getOrCreateId(PersistentSecondComponent::class.java)
    val parent = ManualGlobalKey(key = "\$root", position = 0, parent = null)
    val typeIdDifference = secondTypeId - firstTypeId
    check(typeIdDifference > 0)
    val first = DefaultGlobalKey(
        key = firstTypeId,
        position = 31 * typeIdDifference,
        parent = parent,
    )
    val second = DefaultGlobalKey(key = secondTypeId, position = 0, parent = parent)
    val hookKey = HookKey.create(callSiteId = 0, index = 0, parent = null, intern = false)

    assertThat(first.hashCode()).isEqualTo(second.hashCode())
    assertThat(saveableStateKey(first, hookKey, null, "first"))
        .isNotEqualTo(saveableStateKey(second, hookKey, null, "second"))
  }

  @Test
  fun `unknown numeric legacy type id fails instead of persisting an unstable identity`() {
    val hookKey = HookKey.create(callSiteId = 0, index = 0, parent = null, intern = false)

    assertThatThrownBy {
          saveableStateKey(
              globalKey = StringGlobalKey(Int.MAX_VALUE.toString()),
              hookLocalKey = hookKey,
              explicitKey = null,
              componentName = "legacy component",
          )
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("cannot derive a persistent identity")
        .hasMessageContaining("explicit Litho component key")
  }

  @Test
  fun `custom saver restores a non-Bundle value`() {
    val saver = Saver<Filter>(save = Filter::name, restore = { Filter(checkNotNull(it) as String) })
    val first = AtomicReference<State<Filter>>()
    val registry = LithoSaveableStateRegistry()
    val firstTree = createTree(registry, CustomStateComponent(saver, first::set))
    val firstView = render(firstTree)
    first.get().updateSync(Filter("favorites"))
    firstView.release()
    val snapshot = registry.snapshot()
    registry.close()

    val restored = AtomicReference<State<Filter>>()
    val restoredRegistry = LithoSaveableStateRegistry(snapshot)
    val restoredTree = createTree(restoredRegistry, CustomStateComponent(saver, restored::set))
    val restoredView = render(restoredTree)

    assertThat(restored.get().value).isEqualTo(Filter("favorites"))
    restoredView.release()
    restoredRegistry.close()
  }

  @Test
  fun `inline saver updates without re-registering its provider`() {
    val registry = LithoSaveableStateRegistry()
    val saveCalls = AtomicInteger()
    val tree = createTree(registry, InlineSaverComponent(version = 1, saveCalls = saveCalls))
    val view = render(tree)
    assertThat(saveCalls).hasValue(1)

    view.setRoot(InlineSaverComponent(version = 2, saveCalls = saveCalls))

    assertThat(saveCalls).hasValue(1)
    assertThat(registry.restoredValueForTest()).isEqualTo("2:0")
    assertThat(saveCalls).hasValue(2)
    view.release()
    registry.close()
  }

  @Test
  fun `acquired registry survives tree release`() {
    val state = AtomicReference<State<Int>>()
    val registry = LithoSaveableStateRegistry()
    val tree = createTree(registry, CapturingStateComponent(onState = state::set))
    val view = render(tree)
    state.get().updateSync(6)
    assertThat(tree.acquireLithoSaveableStateRegistry()).isSameAs(registry)
    assertThatThrownBy { tree.acquireLithoSaveableStateRegistry() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("already been acquired")

    view.release()

    assertThat(LithoSaveableStateRegistry(registry.snapshot()).restoredValueForTest()).isEqualTo(6)
    registry.close()
  }

  @Test
  fun `registry acquisition is restricted to the main thread`() {
    val tree = ComponentTree.create(lithoTestRule.context).build()
    val acquire = FutureTask {
      runCatching { tree.acquireLithoSaveableStateRegistry() }.exceptionOrNull()
    }

    Thread(acquire).start()

    assertThat(acquire.get(5, TimeUnit.SECONDS))
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("must run on the main thread")
    tree.release()
  }

  /**
   * ```text
   * First process scope                Recreated process scope
   * -------------------                -----------------------
   * ComponentTree A                    ComponentTree B
   * ViewModel A                        New ViewModel B
   * Saveable state = 7  -- Bundle -->  Restored saveable state = 7
   * Process owner closes store A
   * ```
   */
  @Test
  fun `snapshot restores state but not ViewModels into a new tree`() {
    val firstRegistry = LithoSaveableStateRegistry()
    val firstViewModelStore = LithoViewModelStore()
    val firstState = AtomicReference<State<Int>>()
    val firstModel = AtomicReference<ProcessViewModel>()
    val firstTree = createTree(
        firstRegistry,
        ProcessStateComponent(onState = firstState::set, onModel = firstModel::set),
        firstViewModelStore,
    )
    val firstView = render(firstTree)
    firstState.get().updateSync(7)
    val savedState = firstRegistry.snapshot()

    firstView.release()
    firstViewModelStore.close()
    firstRegistry.close()

    val restoredRegistry = LithoSaveableStateRegistry(savedState)
    val restoredViewModelStore = LithoViewModelStore()
    val restoredState = AtomicReference<State<Int>>()
    val restoredModel = AtomicReference<ProcessViewModel>()
    val restoredTree = createTree(
        restoredRegistry,
        ProcessStateComponent(onState = restoredState::set, onModel = restoredModel::set),
        restoredViewModelStore,
    )
    val restoredView = render(restoredTree)

    assertThat(firstModel.get().clearCount).hasValue(1)
    assertThat(restoredModel.get()).isNotSameAs(firstModel.get())
    assertThat(restoredState.get().value).isEqualTo(7)
    restoredView.release()
    restoredViewModelStore.close()
    restoredRegistry.close()
  }

  /**
   * ```text
   * Parent registry                  Restored parent registry
   *  |- child "first" = 11  Bundle   |- child "first" = 11
   *  `- child "second" = 22 ------>  `- child "second" = 22
   * ```
   */
  @Test
  fun `nested trees restore independent state from the parent snapshot`() {
    val parentRegistry = LithoSaveableStateRegistry()
    val first = AtomicReference<State<Int>>()
    val second = AtomicReference<State<Int>>()
    val firstTree = createNestedTree(parentRegistry, "first", first::set)
    val secondTree = createNestedTree(parentRegistry, "second", second::set)
    val firstView = render(firstTree)
    val secondView = render(secondTree)

    first.get().updateSync(11)
    second.get().updateSync(22)
    val snapshot = parentRegistry.snapshot()

    firstView.release()
    secondView.release()
    parentRegistry.close()

    val restoredParentRegistry = LithoSaveableStateRegistry(snapshot)
    val restoredFirst = AtomicReference<State<Int>>()
    val restoredSecond = AtomicReference<State<Int>>()
    val restoredFirstView =
        render(createNestedTree(restoredParentRegistry, "first", restoredFirst::set))
    val restoredSecondView =
        render(createNestedTree(restoredParentRegistry, "second", restoredSecond::set))

    assertThat(restoredFirst.get().value).isEqualTo(11)
    assertThat(restoredSecond.get().value).isEqualTo(22)
    restoredFirstView.release()
    restoredSecondView.release()
    restoredParentRegistry.close()
  }

  @Test
  fun `releasing a nested tree removes its state from the parent snapshot`() {
    val parentRegistry = LithoSaveableStateRegistry()
    val state = AtomicReference<State<Int>>()
    val childView = render(createNestedTree(parentRegistry, "child", state::set))
    state.get().updateSync(9)

    childView.release()

    assertThat(parentRegistry.snapshot().isEmpty).isTrue()
    parentRegistry.close()
  }

  @Test
  fun `nested tree without a stable identity fails before reading restored state`() {
    val parentRegistry = LithoSaveableStateRegistry()
    val childRegistry = parentRegistry.createChildRegistry(key = null)

    assertThatThrownBy { childRegistry.restoredValue("counter") }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("stable identity")
        .hasMessageContaining("withNestedTreeSaveableStateKey")

    childRegistry.close()
    parentRegistry.close()
  }

  @Test
  fun `failed nested registration rolls back the child provider`() {
    val parentRegistry = LithoSaveableStateRegistry()
    val firstChild = parentRegistry.createChildRegistry("shared")
    firstChild.registerProvider("counter") { 1 }
    val secondChild = parentRegistry.createChildRegistry("shared")

    assertThatThrownBy { secondChild.registerProvider("counter") { 2 } }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("already registered")

    firstChild.close()
    secondChild.registerProvider("counter") { 3 }
    val restoredParent = LithoSaveableStateRegistry(parentRegistry.snapshot())
    val restoredChild = restoredParent.createChildRegistry("shared")

    assertThat(restoredChild.restoredValue("counter")?.value).isEqualTo(3)

    secondChild.close()
    restoredChild.close()
    restoredParent.close()
    parentRegistry.close()
  }

  @Test
  fun `acquired nested registry survives tree replacement and remains in parent snapshot`() {
    val parentRegistry = LithoSaveableStateRegistry()
    val first = AtomicReference<State<Int>>()
    val firstTree = createNestedTree(parentRegistry, "child", first::set)
    val firstView = render(firstTree)
    first.get().updateSync(13)
    val childRegistry = checkNotNull(firstTree.acquireLithoSaveableStateRegistry())

    firstView.release()

    val restored = AtomicReference<State<Int>>()
    val replacementTree = createTree(
        registry = childRegistry,
        component = CapturingStateComponent(onState = restored::set),
    )
    val replacementView = render(replacementTree)

    assertThat(restored.get().value).isEqualTo(13)
    assertThat(parentRegistry.snapshot().isEmpty).isFalse()
    replacementView.release()
    childRegistry.close()
    assertThat(parentRegistry.snapshot().isEmpty).isTrue()
    parentRegistry.close()
  }

  private fun createTree(
      registry: LithoSaveableStateRegistry,
      component: Component,
      viewModelStore: LithoViewModelStore? = null,
  ): ComponentTree =
      ComponentTree.create(lithoTestRule.context, component)
          .withLithoSaveableStateRegistry(registry)
          .apply { viewModelStore?.let(::withLithoViewModelStore) }
          .build()

  private fun createNestedTree(
      parentRegistry: LithoSaveableStateRegistry,
      key: String,
      onState: (State<Int>) -> Unit,
  ): ComponentTree {
    val treeProps =
        TreePropContainer().apply {
          put(LithoSaveableStateRegistryTreeProp, parentRegistry)
        }
    return ComponentTree.create(
            ComponentContext(lithoTestRule.context.androidContext, treePropContainer = treeProps),
            CapturingStateComponent(onState = onState),
        )
        .withNestedTreeSaveableStateKey(key)
        .build()
  }

  private fun render(tree: ComponentTree) =
      lithoTestRule.createTestLithoView(componentTree = tree).attachToWindow().measure().layout()

  private class CapturingStateComponent(
      private val key: String? = null,
      private val onState: (State<Int>) -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val state = useSaveableState(key = key) { 0 }
      onState(state)
      return EmptyComponent()
    }
  }

  private class TwoStateComponent(
      private val onFirst: (State<Int>) -> Unit,
      private val onSecond: (State<Int>) -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val first = useSaveableState { 0 }
      val second = useSaveableState { 0 }
      onFirst(first)
      onSecond(second)
      return EmptyComponent()
    }
  }

  private class TwoComponentInstances(
      private val onFirst: (State<Int>) -> Unit,
      private val onSecond: (State<Int>) -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = Row {
      child(CapturingStateComponent(onState = onFirst))
      child(CapturingStateComponent(onState = onSecond))
    }
  }

  private class CustomStateComponent(
      private val saver: Saver<Filter>,
      private val onState: (State<Filter>) -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val state = useSaveableState(saver = saver) { Filter("all") }
      onState(state)
      return EmptyComponent()
    }
  }

  private class InlineSaverComponent(
      private val version: Int,
      private val saveCalls: AtomicInteger,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      useSaveableState(
          saver =
              Saver(
                  save = { value ->
                    saveCalls.incrementAndGet()
                    "$version:$value"
                  },
                  restore = { value -> (value as String).substringAfter(':').toInt() },
              ),
      ) {
        0
      }
      return EmptyComponent()
    }
  }

  private class ProcessStateComponent(
      private val onState: (State<Int>) -> Unit,
      private val onModel: (ProcessViewModel) -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val state = useSaveableState { 0 }
      val model by useViewModel { ProcessViewModel() }
      onState(state)
      onModel(model)
      return EmptyComponent()
    }
  }

  private class ProcessViewModel : ViewModel() {
    val clearCount = AtomicInteger()

    override fun onCleared() {
      clearCount.incrementAndGet()
    }
  }

  private class PersistentRootComponent : KComponent() {
    override fun ComponentScope.render(): Component = EmptyComponent()
  }

  private class PersistentChildComponent : KComponent() {
    override fun ComponentScope.render(): Component = EmptyComponent()
  }

  private class PersistentFirstComponent : KComponent() {
    override fun ComponentScope.render(): Component = EmptyComponent()
  }

  private class PersistentSecondComponent : KComponent() {
    override fun ComponentScope.render(): Component = EmptyComponent()
  }

  private data class Filter(val name: String)

  private fun LithoSaveableStateRegistry.restoredValueForTest(): Any? {
    val snapshot = snapshot()
    val restored = LithoSaveableStateRegistry(snapshot)
    val value = restored.restoredValue(snapshot.keySet().single())?.value
    restored.close()
    return value
  }
}
