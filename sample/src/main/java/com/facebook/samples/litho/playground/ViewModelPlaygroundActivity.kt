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

package com.facebook.samples.litho.playground

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModel
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Saver
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.padding
import com.facebook.litho.getLithoViewModelStore
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.lithoSaveableStateScope
import com.facebook.litho.useCallback
import com.facebook.litho.useFlow
import com.facebook.litho.useSaveableState
import com.facebook.litho.useState
import com.facebook.litho.useViewModel
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.VerticalScroll
import com.facebook.litho.withSaveableStateScope
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import com.facebook.rendercore.sp
import com.facebook.rendercore.stringRes
import com.facebook.samples.litho.NavigatableDemoActivity
import com.facebook.samples.litho.R
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalLithoApi::class)
@SuppressLint("BadSuperClassFragmentActivity.FbFragmentActivity", "EndpointWithoutSwitchOff")
class ViewModelPlaygroundActivity : NavigatableDemoActivity() {
  private val saveableStateScope by lithoSaveableStateScope(SAVEABLE_STATE_KEY)
  private lateinit var componentTree: ComponentTree

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    componentTree =
        ComponentTree.create(ComponentContext(this))
            .withLithoViewModelStore(getLithoViewModelStore())
            .withSaveableStateScope(saveableStateScope)
            .build()
    val lithoView = LithoView(this)
    setContentView(lithoView)
    lithoView.componentTree = componentTree
    componentTree.setRootAsync(ViewModelPlaygroundKComponent(onRecreate = ::recreate))
  }

  override fun onDestroy() {
    if (::componentTree.isInitialized) {
      componentTree.release()
      saveableStateScope.close()
    }
    super.onDestroy()
  }

  private companion object {
    const val SAVEABLE_STATE_KEY: String = "view_model_playground"
  }
}

@OptIn(ExperimentalLithoApi::class)
private class ViewModelPlaygroundKComponent(
    private val onRecreate: () -> Unit,
) : KComponent() {
  override fun ComponentScope.render(): Component {
    val showCounter = useState { true }
    val scrollPosition = useSaveableState(
        saver =
            Saver(
                save = { it.offset.get() },
                restore = { ScrollPosition(it as? Int ?: 0) },
            ),
    ) {
      ScrollPosition(0)
    }
    val onToggleCounter =
        useCallback<ClickEvent, Unit> { showCounter.update { visible -> !visible } }
    val onRecreateClick = useCallback<ClickEvent, Unit> { onRecreate() }
    val onScrollChange =
        useCallback<NestedScrollView, Int, Int, Unit> { _, scrollY, _ ->
          scrollPosition.value.offset.set(scrollY)
        }

    return VerticalScroll(
        child =
            Column(style = Style.padding(all = 24.dp), gap = 12.dp) {
              child(
                  Text(
                      text = stringRes(R.string.view_model_playground_title),
                      textSize = 24.sp,
                  ),
              )
              if (showCounter.value) {
                child(ViewModelCounterComponent())
              } else {
                child(Text(text = stringRes(R.string.view_model_playground_component_removed)))
              }
              child(
                  Text(
                      text =
                          stringRes(
                              if (showCounter.value) {
                                R.string.view_model_playground_remove_component
                              } else {
                                R.string.view_model_playground_restore_component
                              },
                          ),
                      style = secondaryButtonStyle.onClick(onToggleCounter),
                  ),
              )
              child(RecreateActivityButton(onRecreateClick))
              child(
                  Text(
                      text =
                          stringRes(
                              R.string.view_model_playground_scroll_position,
                              scrollPosition.value.offset.get(),
                          ),
                  ),
              )
              for (index in 1..SCROLL_ITEM_COUNT) {
                child(
                    Text(
                        text = stringRes(R.string.view_model_playground_scroll_item, index),
                        textSize = 18.sp,
                        style =
                            Style.backgroundColor(
                                    if (index % 2 == 0) {
                                      Color.rgb(245, 246, 247)
                                    } else {
                                      Color.WHITE
                                    },
                                )
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                    ),
                )
              }
              child(RecreateActivityButton(onRecreateClick))
            },
        scrollbarEnabled = true,
        initialScrollPosition = scrollPosition.value.offset.get().px,
        onScrollChange = onScrollChange,
    )
  }

  private companion object {
    const val SCROLL_ITEM_COUNT: Int = 40
  }
}

private class RecreateActivityButton(
    private val onClick: (ClickEvent) -> Unit,
) : KComponent() {
  override fun ComponentScope.render(): Component = Text(
      text = stringRes(R.string.view_model_playground_recreate),
      style = secondaryButtonStyle.onClick(onClick),
  )
}

private class ScrollPosition(initialOffset: Int) {
  val offset = AtomicInteger(initialOffset)
}

@OptIn(ExperimentalLithoApi::class)
private class ViewModelCounterComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val viewModel by useViewModel { CounterViewModel() }
    val count = useFlow(viewModel.count)
    val onIncrementClick = useCallback<ClickEvent, Unit> { viewModel.increment() }

    return Column(gap = 12.dp) {
      child(
          Text(
              text =
                  stringRes(
                      R.string.view_model_playground_count,
                      count,
                  ),
              textSize = 20.sp,
          ),
      )
      child(
          Text(
              text =
                  stringRes(
                      R.string.view_model_playground_creation_thread,
                      viewModel.creationThread,
                  ),
          ),
      )
      child(
          Text(
              text = stringRes(R.string.view_model_playground_increment),
              textColor = Color.WHITE,
              style = primaryButtonStyle.onClick(onIncrementClick),
          ),
      )
    }
  }
}

private val secondaryButtonStyle =
    Style.backgroundColor(Color.rgb(232, 234, 237)).padding(horizontal = 16.dp, vertical = 12.dp)

private val primaryButtonStyle =
    Style.backgroundColor(Color.rgb(25, 103, 210)).padding(horizontal = 16.dp, vertical = 12.dp)

private class CounterViewModel : ViewModel() {
  private val mutableCount = MutableStateFlow(0)
  val count: StateFlow<Int> = mutableCount.asStateFlow()

  val creationThread: String = Thread.currentThread().name

  fun increment() {
    mutableCount.update { count -> count + 1 }
  }
}
