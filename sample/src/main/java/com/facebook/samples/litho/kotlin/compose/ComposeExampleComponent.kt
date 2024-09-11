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

package com.facebook.samples.litho.kotlin.compose

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComposeComponent
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.widthPercent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useCached
import com.facebook.litho.useCallback
import com.facebook.litho.useComposable
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.TextAlignment
import com.facebook.litho.widget.VerticalGravity
import com.facebook.rendercore.dp as lithoDp

private val EXAMPLE_COMPOSE_COMPONENT_CONTENT_TYPE = {
  "com.facebook.samples.litho.kotlin.compose.ComposeExampleComponent"
}

class ComposeExampleComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val parentRerenders = useCached { LongArray(1) }
    parentRerenders[0]++
    val parentLocalState = useState { 1 }

    val parentSharedState = useState { 1 }
    val onUpdateParentSharedState = useCallback<Unit> { parentSharedState.update { it + 1 } }

    val composeSharedState = useState { 1 }

    return Column {
      // Litho parent
      child(Text("Litho Parent Component"))
      child(Text("Rerenders: ${parentRerenders[0]}"))
      child(Column(style = Style.height(10.lithoDp)))
      child(Text("State value: ${parentLocalState.value}"))
      child(Text("State updated by child value: ${parentSharedState.value}"))
      child(
          Text(
              "Litho: update Self (Litho Parent)",
              alignment = TextAlignment.CENTER,
              verticalGravity = VerticalGravity.CENTER,
              textColor = AndroidColor.WHITE,
              style =
                  Style.backgroundColor(AndroidColor.DKGRAY).height(48.lithoDp).onClick {
                    parentLocalState.update { it + 1 }
                  }))
      child(Column(style = Style.height(5.lithoDp)))
      child(
          Text(
              "Litho: update Child Compose Component",
              alignment = TextAlignment.CENTER,
              verticalGravity = VerticalGravity.CENTER,
              textColor = AndroidColor.WHITE,
              style =
                  Style.backgroundColor(AndroidColor.DKGRAY).height(48.lithoDp).onClick {
                    composeSharedState.update { it + 1 }
                  }))
      // Compose child
      child(Column(style = Style.height(20.lithoDp)))
      child(
          ComposeComponent(
              composable =
                  useComposable(composeSharedState.value) {
                    ExampleComponent(
                        stateFromParent = composeSharedState.value,
                        onUpdateParentSharedState = onUpdateParentSharedState)
                  },
              contentType = EXAMPLE_COMPOSE_COMPONENT_CONTENT_TYPE,
              style = Style.widthPercent(100f).height(200.lithoDp)))
    }
  }
}

@Composable
private fun ExampleComponent(stateFromParent: Int, onUpdateParentSharedState: () -> Unit) {
  val recompositions = remember { LongArray(1) }
  SideEffect { recompositions[0]++ }

  var localState by remember { mutableIntStateOf(1) }

  Box(modifier = Modifier.background(ComposeColor.LightGray).border(2.dp, ComposeColor.Black)) {
    Column {
      BasicText("Compose Component")
      BasicText("Recompositions: ${recompositions[0]}")
      Spacer(modifier = Modifier.height(10.dp))
      BasicText("State value: $localState")
      BasicText("State from parent value: $stateFromParent")
      BasicText(
          "Compose: update Self (Compose Child)",
          style = TextStyle.Default.copy(textAlign = TextAlign.Center, color = ComposeColor.White),
          modifier =
              Modifier.background(ComposeColor.DarkGray)
                  .fillMaxWidth()
                  .height(48.dp)
                  .clickable { localState += 1 }
                  .wrapContentHeight(Alignment.CenterVertically))
      Spacer(modifier = Modifier.height(5.dp))
      BasicText(
          "Compose: update Parent Litho Component",
          style = TextStyle.Default.copy(textAlign = TextAlign.Center, color = ComposeColor.White),
          modifier =
              Modifier.background(ComposeColor.DarkGray)
                  .fillMaxWidth()
                  .height(48.dp)
                  .clickable { onUpdateParentSharedState() }
                  .wrapContentHeight(Alignment.CenterVertically))
    }
  }
}
