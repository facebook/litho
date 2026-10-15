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

import android.view.View
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalLithoApi::class)
@RunWith(LithoTestRunner::class)
class LithoViewModelHooksTest {

  @get:Rule val lithoTestRule = LithoTestRule()
  private val stores = mutableListOf<LithoViewModelStore>()

  @After
  fun closeStores() {
    stores.forEach(LithoViewModelStore::close)
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
  }

  private class CapturingComponent(
      private val onModel: (TrackingViewModel) -> Unit = {},
      private val factory: () -> TrackingViewModel = ::TrackingViewModel,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val model by useViewModel(factory = factory)
      onModel(model)
      return EmptyComponent()
    }
  }

  private class BlockingAfterViewModelComponent(
      private val model: AtomicReference<TrackingViewModel>,
      private val modelReady: CountDownLatch,
      private val allowRenderToFinish: CountDownLatch,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val current by useViewModel(factory = ::TrackingViewModel)
      model.set(current)
      modelReady.countDown()
      check(allowRenderToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      return EmptyComponent()
    }
  }

  private class BlockingBeforeViewModelComponent(
      private val model: AtomicReference<TrackingViewModel>,
      private val renderStarted: CountDownLatch,
      private val allowRenderToContinue: CountDownLatch,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      renderStarted.countDown()
      check(allowRenderToContinue.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      val current by useViewModel(factory = ::TrackingViewModel)
      model.set(current)
      return EmptyComponent()
    }
  }

  private class BlockingLayoutAfterViewModelComponent(
      private val model: AtomicReference<TrackingViewModel>,
      private val modelReady: CountDownLatch,
      private val allowRenderToFinish: CountDownLatch,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = RenderWithConstraints {
      val current by useViewModel(factory = ::TrackingViewModel)
      model.set(current)
      modelReady.countDown()
      check(allowRenderToFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
      EmptyComponent()
    }
  }

  private class RenderTrackingComponent(private val onRender: () -> Unit) : KComponent() {
    override fun ComponentScope.render(): Component {
      onRender()
      return EmptyComponent()
    }
  }

  @Test
  fun `useViewModel reuses the model across root updates`() {
    val creations = AtomicInteger()
    val first = AtomicReference<TrackingViewModel>()
    val latest = AtomicReference<TrackingViewModel>()
    val testLithoView = renderWithStore(
        CapturingComponent {
          creations.incrementAndGet()
          TrackingViewModel().also(first::set)
        },
    )

    testLithoView.setRoot(
        CapturingComponent {
          creations.incrementAndGet()
          TrackingViewModel()
        },
    )
    testLithoView.setRoot(
        CapturingComponent(onModel = latest::set) {
          creations.incrementAndGet()
          TrackingViewModel()
        },
    )

    assertThat(creations).hasValue(1)
    assertThat(latest.get()).isSameAs(first.get())
    testLithoView.release()
  }

  @Test
  fun `components in the same store share the default model`() {
    val first = AtomicReference<TrackingViewModel>()
    val second = AtomicReference<TrackingViewModel>()

    val testLithoView = renderWithStore(TwoCapturingComponents(first, second))

    assertThat(second.get()).isSameAs(first.get())
    testLithoView.release()
  }

  @Test
  fun `different explicit keys receive different models`() {
    val first = AtomicReference<TrackingViewModel>()
    val second = AtomicReference<TrackingViewModel>()

    val testLithoView = renderWithStore(DifferentlyKeyedComponent(first, second))

    assertThat(second.get()).isNotSameAs(first.get())
    testLithoView.release()
  }

  private class DifferentlyKeyedComponent(
      private val first: AtomicReference<TrackingViewModel>,
      private val second: AtomicReference<TrackingViewModel>,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val firstModel by useViewModel(
          key = "first",
          factory = ::TrackingViewModel,
      )
      val secondModel by useViewModel(
          key = "second",
          factory = ::TrackingViewModel,
      )
      first.set(firstModel)
      second.set(secondModel)
      return EmptyComponent()
    }
  }

  @Test
  fun `same explicit key shares a model within one component`() {
    val first = AtomicReference<TrackingViewModel>()
    val second = AtomicReference<TrackingViewModel>()

    val testLithoView = renderWithStore(ExplicitlySharedComponent(first, second))

    assertThat(second.get()).isSameAs(first.get())
    testLithoView.release()
  }

  private class ExplicitlySharedComponent(
      private val first: AtomicReference<TrackingViewModel>,
      private val second: AtomicReference<TrackingViewModel>,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val firstModel by useViewModel(
          key = "shared",
          factory = ::TrackingViewModel,
      )
      val secondModel by useViewModel(
          key = "shared",
          factory = ::TrackingViewModel,
      )
      first.set(firstModel)
      second.set(secondModel)
      return EmptyComponent()
    }
  }

  @Test
  fun `view model survives component and tree removal until its store closes`() {
    val creations = AtomicInteger()
    val first = AtomicReference<TrackingViewModel>()
    val restored = AtomicReference<TrackingViewModel>()
    val store = LithoViewModelStore()
    val testLithoView = renderWithStore(
        CapturingComponent(onModel = first::set) {
          creations.incrementAndGet()
          TrackingViewModel()
        },
        store,
    )

    testLithoView.setRoot(EmptyComponent())
    assertThat(first.get().clearCount).hasValue(0)

    testLithoView.setRoot(
        CapturingComponent(onModel = restored::set) {
          creations.incrementAndGet()
          TrackingViewModel()
        },
    )

    assertThat(creations).hasValue(1)
    assertThat(restored.get()).isSameAs(first.get())

    testLithoView.release()
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    assertThat(first.get().clearCount).hasValue(0)

    store.close()
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    assertThat(first.get().clearCount).hasValue(1)
  }

  @Test
  fun `owner supplied to tree supports async resolve before LithoView exists`() {
    val owner = TestViewModelStoreOwner()
    val model = AtomicReference<TrackingViewModel>()
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val tree =
        ComponentTree.create(lithoTestRule.context)
            .withLithoViewModelStore(owner.getLithoViewModelStore())
            .build()

    tree.setRootAndSizeSpecAsync(
        CapturingComponent(onModel = model::set),
        sizeSpec,
        sizeSpec,
    )
    lithoTestRule.finishAsyncUpdates()

    assertThat(tree.lithoView).isNull()
    assertThat(model.get()).isNotNull()

    tree.release()
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    assertThat(model.get().clearCount).hasValue(0)

    owner.viewModelStore.clear()
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    assertThat(model.get().clearCount).hasValue(1)
  }

  @Test
  fun `owner clearing waits for an active background resolve`() {
    val owner = TestViewModelStoreOwner()
    val model = AtomicReference<TrackingViewModel>()
    val modelReady = CountDownLatch(1)
    val allowRenderToFinish = CountDownLatch(1)
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val tree =
        ComponentTree.create(lithoTestRule.context)
            .withLithoViewModelStore(owner.getLithoViewModelStore())
            .build()
    val resolve = FutureTask {
      tree.setRootAndSizeSpecSync(
          BlockingAfterViewModelComponent(model, modelReady, allowRenderToFinish),
          sizeSpec,
          sizeSpec,
      )
    }
    val resolveThread = Thread(resolve)

    try {
      resolveThread.start()
      check(modelReady.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

      tree.release()
      owner.viewModelStore.clear()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      assertThat(model.get().clearCount).hasValue(0)

      allowRenderToFinish.countDown()
      resolve.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      assertThat(model.get().clearCount).hasValue(1)
    } finally {
      allowRenderToFinish.countDown()
      resolveThread.interrupt()
      tree.release()
      owner.viewModelStore.clear()
    }
  }

  @Test
  fun `active resolve can create a ViewModel after owner clearing starts`() {
    val owner = TestViewModelStoreOwner()
    val model = AtomicReference<TrackingViewModel>()
    val renderStarted = CountDownLatch(1)
    val allowRenderToContinue = CountDownLatch(1)
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val tree =
        ComponentTree.create(lithoTestRule.context)
            .withLithoViewModelStore(owner.getLithoViewModelStore())
            .build()
    val resolve = FutureTask {
      tree.setRootAndSizeSpecSync(
          BlockingBeforeViewModelComponent(model, renderStarted, allowRenderToContinue),
          sizeSpec,
          sizeSpec,
      )
    }
    val resolveThread = Thread(resolve)

    try {
      resolveThread.start()
      check(renderStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

      tree.release()
      owner.viewModelStore.clear()
      allowRenderToContinue.countDown()

      resolve.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      assertThat(model.get().clearCount).hasValue(1)
    } finally {
      allowRenderToContinue.countDown()
      resolveThread.interrupt()
      tree.release()
      owner.viewModelStore.clear()
    }
  }

  @Test
  fun `owner clearing waits for an active background layout render`() {
    val owner = TestViewModelStoreOwner()
    val model = AtomicReference<TrackingViewModel>()
    val modelReady = CountDownLatch(1)
    val allowRenderToFinish = CountDownLatch(1)
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val tree =
        ComponentTree.create(lithoTestRule.context)
            .withLithoViewModelStore(owner.getLithoViewModelStore())
            .build()
    val layout = FutureTask {
      tree.setRootAndSizeSpecSync(
          BlockingLayoutAfterViewModelComponent(model, modelReady, allowRenderToFinish),
          sizeSpec,
          sizeSpec,
      )
    }
    val layoutThread = Thread(layout)

    try {
      layoutThread.start()
      check(modelReady.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

      tree.release()
      owner.viewModelStore.clear()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      assertThat(model.get().clearCount).hasValue(0)

      allowRenderToFinish.countDown()
      layout.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      assertThat(model.get().clearCount).hasValue(1)
    } finally {
      allowRenderToFinish.countDown()
      layoutThread.interrupt()
      tree.release()
      owner.viewModelStore.clear()
    }
  }

  @Test
  fun `async calculation without ViewModels completes after owner clearing`() {
    val owner = TestViewModelStoreOwner()
    val rendered = AtomicInteger()
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val tree =
        ComponentTree.create(lithoTestRule.context)
            .withLithoViewModelStore(owner.getLithoViewModelStore())
            .build()

    owner.viewModelStore.clear()
    tree.setRootAndSizeSpecAsync(
        RenderTrackingComponent { rendered.incrementAndGet() },
        sizeSpec,
        sizeSpec,
    )
    lithoTestRule.finishAsyncUpdates()

    assertThat(rendered).hasValue(1)
    tree.release()
  }

  @Test
  fun `owner backed store supports background ComponentTree construction`() {
    val owner = TestViewModelStoreOwner()
    val store = owner.getLithoViewModelStore()
    val model = AtomicReference<TrackingViewModel>()
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val treeTask = FutureTask {
      val tree = ComponentTree.create(lithoTestRule.context).withLithoViewModelStore(store).build()
      tree.setRootAndSizeSpecSync(
          CapturingComponent(onModel = model::set),
          sizeSpec,
          sizeSpec,
      )
      tree
    }
    val builderThread = Thread(treeTask)

    builderThread.start()
    val tree = treeTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    assertThat(tree.lithoView).isNull()
    assertThat(model.get()).isNotNull()
    tree.release()
    owner.viewModelStore.clear()
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    assertThat(model.get().clearCount).hasValue(1)
  }

  @Test
  fun `detached trees supplied the same owner share models`() {
    val owner = TestViewModelStoreOwner()
    val creations = AtomicInteger()
    val first = AtomicReference<TrackingViewModel>()
    val second = AtomicReference<TrackingViewModel>()
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val store = owner.getLithoViewModelStore()
    val firstTree =
        ComponentTree.create(lithoTestRule.context).withLithoViewModelStore(store).build()
    val secondTree =
        ComponentTree.create(lithoTestRule.context).withLithoViewModelStore(store).build()

    firstTree.setRootAndSizeSpecAsync(
        CapturingComponent(onModel = first::set) {
          creations.incrementAndGet()
          TrackingViewModel()
        },
        sizeSpec,
        sizeSpec,
    )
    secondTree.setRootAndSizeSpecAsync(
        CapturingComponent(onModel = second::set) {
          creations.incrementAndGet()
          TrackingViewModel()
        },
        sizeSpec,
        sizeSpec,
    )
    lithoTestRule.finishAsyncUpdates()

    assertThat(creations).hasValue(1)
    assertThat(second.get()).isSameAs(first.get())

    firstTree.release()
    secondTree.release()
    owner.viewModelStore.clear()
  }

  @Test
  fun `late inherited store supports an ownerless tree`() {
    val inheritedStore = LithoViewModelStore()
    stores += inheritedStore
    val model = AtomicReference<TrackingViewModel>()
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val tree = ComponentTree.create(lithoTestRule.context, EmptyComponent()).build()

    tree.setRootAndSizeSpecSync(
        CapturingComponent(onModel = model::set),
        sizeSpec,
        sizeSpec,
        null,
        TreePropContainer().apply { put(LithoViewModelStoreTreeProp, inheritedStore) },
    )

    assertThat(model.get()).isNotNull()
    tree.release()
  }

  @Test
  fun `inherited store follows root TreeProp updates`() {
    val firstStore = LithoViewModelStore()
    val secondStore = LithoViewModelStore()
    stores += firstStore
    stores += secondStore
    val latest = AtomicReference<TrackingViewModel>()
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val tree = ComponentTree.create(lithoTestRule.context, EmptyComponent()).build()

    tree.setRootAndSizeSpecSync(
        CapturingComponent(onModel = latest::set),
        sizeSpec,
        sizeSpec,
        null,
        TreePropContainer().apply { put(LithoViewModelStoreTreeProp, firstStore) },
    )
    val firstModel = latest.get()
    tree.setRootAndSizeSpecSync(
        CapturingComponent(onModel = latest::set),
        sizeSpec,
        sizeSpec,
        null,
        TreePropContainer().apply { put(LithoViewModelStoreTreeProp, secondStore) },
    )

    assertThat(latest.get()).isNotSameAs(firstModel)
    tree.release()
  }

  @Test
  fun `getLithoViewModelStore rejects a destroyed owner`() {
    val owner = TestViewModelStoreOwner(Lifecycle.State.DESTROYED)

    assertThatThrownBy { owner.getLithoViewModelStore() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("destroyed ViewModelStoreOwner")
  }

  @Test
  fun `owner supplied store is not replaced by inherited tree props`() {
    val owner = TestViewModelStoreOwner()
    val inheritedStore = LithoViewModelStore()
    stores += inheritedStore
    val latest = AtomicReference<TrackingViewModel>()
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val tree =
        ComponentTree.create(lithoTestRule.context)
            .withLithoViewModelStore(owner.getLithoViewModelStore())
            .build()

    tree.setRootAndSizeSpecSync(
        CapturingComponent(onModel = latest::set),
        sizeSpec,
        sizeSpec,
    )
    val firstModel = latest.get()
    tree.setRootAndSizeSpecSync(
        CapturingComponent(onModel = latest::set),
        sizeSpec,
        sizeSpec,
        null,
        TreePropContainer().apply { put(LithoViewModelStoreTreeProp, inheritedStore) },
    )

    assertThat(latest.get()).isSameAs(firstModel)

    tree.release()
    owner.viewModelStore.clear()
  }

  @Test
  fun `owner lookup does not access its default ViewModel factory`() {
    val owner = ThrowingDefaultFactoryOwner()

    val first = owner.getLithoViewModelStore()
    val second = owner.getLithoViewModelStore()

    assertThat(second).isSameAs(first)
    owner.viewModelStore.clear()
  }

  @Test
  fun `cleared Android owner store is recreated when the owner is reused`() {
    val owner = TestViewModelStoreOwner()
    val latest = AtomicReference<TrackingViewModel>()
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE_PX, View.MeasureSpec.EXACTLY)
    val firstTree =
        ComponentTree.create(lithoTestRule.context)
            .withLithoViewModelStore(owner.getLithoViewModelStore())
            .build()
    firstTree.setRootAndSizeSpecSync(
        CapturingComponent(onModel = latest::set),
        sizeSpec,
        sizeSpec,
    )
    val firstModel = latest.get()

    owner.viewModelStore.clear()
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    val secondTree =
        ComponentTree.create(lithoTestRule.context)
            .withLithoViewModelStore(owner.getLithoViewModelStore())
            .build()
    secondTree.setRootAndSizeSpecSync(
        CapturingComponent(onModel = latest::set),
        sizeSpec,
        sizeSpec,
    )
    val secondModel = latest.get()

    assertThat(secondModel).isNotSameAs(firstModel)
    assertThat(firstModel.clearCount).hasValue(1)
    assertThat(secondModel.clearCount).hasValue(0)

    firstTree.release()
    secondTree.release()
    owner.viewModelStore.clear()
  }

  @Test
  fun `nested ComponentTree inherits its parent's ViewModel store`() {
    val owner = TestViewModelStoreOwner()
    val parentModel = AtomicReference<TrackingViewModel>()
    val nestedModel = AtomicReference<TrackingViewModel>()
    val parentContext = AtomicReference<ComponentContext>()
    val parentTree =
        ComponentTree.create(
                lithoTestRule.context,
                ParentContextCapturingComponent(parentModel::set, parentContext::set),
            )
            .withLithoViewModelStore(owner.getLithoViewModelStore())
            .build()
    val parentLithoView =
        lithoTestRule
            .createTestLithoView(componentTree = parentTree)
            .attachToWindow()
            .measure()
            .layout()
    val nestedTree =
        ComponentTree.create(
                ComponentContext.makeCopyForNestedTree(parentContext.get()),
                CapturingComponent(onModel = nestedModel::set),
            )
            .build()
    val nestedLithoView =
        lithoTestRule
            .createTestLithoView(componentTree = nestedTree)
            .attachToWindow()
            .measure()
            .layout()

    assertThat(nestedModel.get()).isSameAs(parentModel.get())

    nestedLithoView.release()
    parentLithoView.release()
    owner.viewModelStore.clear()
  }

  @Test
  fun `delegated property resolves the model during render`() {
    val store = LithoViewModelStore()
    val capturedModel = AtomicReference<TrackingViewModel>()
    val deferredRead = AtomicReference<() -> TrackingViewModel>()
    val testLithoView = renderWithStore(
        DeferredReadComponent(capturedModel::set, deferredRead::set),
        store,
    )

    store.close()
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

    assertThat(deferredRead.get().invoke()).isSameAs(capturedModel.get())
    testLithoView.release()
  }

  @Test
  fun `ownerless tree requires an explicitly supplied store`() {
    assertThatThrownBy {
          lithoTestRule.render { CapturingComponent() }
        }
        .hasMessageContaining("No ViewModelStore is available for useViewModel")
        .hasMessageContaining("withLithoViewModelStore(owner.getLithoViewModelStore())")
        .hasMessageContaining("Nested ComponentTrees inherit their parent's store")
  }

  @Test
  fun `releasing a tree does not clear an externally supplied store`() {
    val store = LithoViewModelStore()
    val model = AtomicReference<TrackingViewModel>()
    val tree =
        ComponentTree.create(
                lithoTestRule.context,
                CapturingComponent(onModel = model::set),
            )
            .withLithoViewModelStore(store)
            .build()
    val testLithoView =
        lithoTestRule.createTestLithoView(componentTree = tree).attachToWindow().measure().layout()

    testLithoView.release()
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    assertThat(model.get().clearCount).hasValue(0)

    store.close()
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    assertThat(model.get().clearCount).hasValue(1)
  }

  private class TrackingViewModel : ViewModel() {
    val clearCount = AtomicInteger()

    override fun onCleared() {
      clearCount.incrementAndGet()
    }
  }

  private class TwoCapturingComponents(
      private val first: AtomicReference<TrackingViewModel>,
      private val second: AtomicReference<TrackingViewModel>,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = Row {
      child(CapturingComponent(onModel = first::set))
      child(CapturingComponent(onModel = second::set))
    }
  }

  private class DeferredReadComponent(
      private val onModel: (TrackingViewModel) -> Unit,
      private val onDeferredRead: (() -> TrackingViewModel) -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val model by useViewModel { TrackingViewModel() }
      onModel(model)
      onDeferredRead { model }
      return EmptyComponent()
    }
  }

  private class ParentContextCapturingComponent(
      private val onModel: (TrackingViewModel) -> Unit,
      private val onContext: (ComponentContext) -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val model by useViewModel { TrackingViewModel() }
      onModel(model)
      onContext(context)
      return EmptyComponent()
    }
  }

  private class ThrowingDefaultFactoryOwner :
      ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val viewModelStore = ViewModelStore()

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
      get() = error("The default factory must not be accessed")
  }

  private class TestViewModelStoreOwner(
      initialState: Lifecycle.State = Lifecycle.State.INITIALIZED,
  ) : ViewModelStoreOwner, LifecycleOwner {
    override val viewModelStore = ViewModelStore()
    private val registry =
        LifecycleRegistry(this).apply {
          if (initialState == Lifecycle.State.DESTROYED) {
            currentState = Lifecycle.State.CREATED
          }
          currentState = initialState
        }
    override val lifecycle: Lifecycle = registry
  }

  private fun renderWithStore(
      component: Component,
      store: LithoViewModelStore = LithoViewModelStore(),
  ): TestLithoView {
    stores += store
    val tree =
        ComponentTree.create(lithoTestRule.context, component)
            .withLithoViewModelStore(store)
            .build()
    return lithoTestRule
        .createTestLithoView(componentTree = tree)
        .attachToWindow()
        .measure()
        .layout()
  }

  private companion object {
    const val TIMEOUT_SECONDS = 5L
    const val VIEW_SIZE_PX = 100
  }
}
