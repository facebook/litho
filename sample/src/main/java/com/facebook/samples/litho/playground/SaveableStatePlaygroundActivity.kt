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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Saver
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useCallback
import com.facebook.litho.useSaveableState
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.VerticalScroll
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import com.facebook.rendercore.sp
import com.facebook.rendercore.stringRes
import com.facebook.samples.litho.NavigatableDemoActivity
import com.facebook.samples.litho.R
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("BadSuperClassFragmentActivity.FbFragmentActivity", "EndpointWithoutSwitchOff")
class SaveableStatePlaygroundActivity : NavigatableDemoActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(
        FrameLayout(this).apply { id = R.id.fragment_view },
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )
    if (savedInstanceState == null) {
      supportFragmentManager
          .beginTransaction()
          .add(R.id.fragment_view, SaveableStatePlaygroundFragment())
          .commit()
    }
  }
}

@OptIn(ExperimentalLithoApi::class)
@SuppressLint("BadSuperClassFragment.FbFragment")
class SaveableStatePlaygroundFragment : Fragment() {
  private var headerTree: ComponentTree? = null
  private var bodyTree: ComponentTree? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    val context = requireContext()
    val header =
        ComponentTree.create(context, this, SaveableHeaderComponent()).build().also {
          headerTree = it
        }
    val body = ComponentTree.create(context, this).build().also { bodyTree = it }
    body.setRootAsync(
        SaveableStatePlaygroundComponent(onRecreate = { requireActivity().recreate() }),
    )
    return LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      addView(
          LithoView(context).apply { componentTree = header },
          LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.WRAP_CONTENT,
          ),
      )
      addView(
          LithoView(context).apply { componentTree = body },
          LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
      )
    }
  }

  override fun onDestroyView() {
    headerTree?.release()
    headerTree = null
    bodyTree?.release()
    bodyTree = null
    super.onDestroyView()
  }
}

@OptIn(ExperimentalLithoApi::class)
private class SaveableHeaderComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val taps = useSaveableState { 0 }
    val onTap = useCallback<ClickEvent, Unit> { taps.update { count -> count + 1 } }
    return Text(
        text = stringRes(R.string.saveable_state_playground_header_taps, taps.value),
        textSize = 18.sp,
        style = secondaryButtonStyle.onClick(onTap),
    )
  }
}

@OptIn(ExperimentalLithoApi::class)
private class SaveableStatePlaygroundComponent(
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
                      text = stringRes(R.string.saveable_state_playground_title),
                      textSize = 24.sp,
                  ),
              )
              if (showCounter.value) {
                child(CounterComponent())
              } else {
                child(Text(text = stringRes(R.string.saveable_state_playground_component_removed)))
              }
              child(
                  Text(
                      text =
                          stringRes(
                              if (showCounter.value) {
                                R.string.saveable_state_playground_remove_component
                              } else {
                                R.string.saveable_state_playground_restore_component
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
                              R.string.saveable_state_playground_scroll_position,
                              scrollPosition.value.offset.get(),
                          ),
                  ),
              )
              for (index in 1..SCROLL_ITEM_COUNT) {
                child(
                    Text(
                        text = stringRes(R.string.saveable_state_playground_scroll_item, index),
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
      text = stringRes(R.string.saveable_state_playground_recreate),
      style = secondaryButtonStyle.onClick(onClick),
  )
}

private class ScrollPosition(initialOffset: Int) {
  val offset = AtomicInteger(initialOffset)
}

/** Shows a saved and an unsaved count side by side, so recreation shows which one survives. */
@OptIn(ExperimentalLithoApi::class)
private class CounterComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val savedCount = useSaveableState { 0 }
    val unsavedCount = useState { 0 }
    val onIncrementClick =
        useCallback<ClickEvent, Unit> {
          savedCount.update { count -> count + 1 }
          unsavedCount.update { count -> count + 1 }
        }

    return Column(gap = 12.dp) {
      child(
          Text(
              text = stringRes(R.string.saveable_state_playground_saved_count, savedCount.value),
              textSize = 20.sp,
          ),
      )
      child(
          Text(
              text =
                  stringRes(R.string.saveable_state_playground_unsaved_count, unsavedCount.value),
              textSize = 20.sp,
          ),
      )
      child(
          Text(
              text = stringRes(R.string.saveable_state_playground_increment),
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
