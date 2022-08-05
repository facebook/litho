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

class Selectable(val text: String, val selected: Boolean, val onClick: (ClickEvent) -> Unit) :
    KComponent() {

  override fun ComponentScope.render(): Component? {
    return Row(
        justifyContent = YogaJustify.SPACE_BETWEEN,
        style = Style.padding(horizontal = 20.dp, vertical = 10.dp).onClick(action = onClick)) {
          child(Text(text))
          child(
              Image(
                  drawableRes(
                      if (selected) android.R.drawable.checkbox_on_background
                      else android.R.drawable.checkbox_off_background)))
        }
  }
}
