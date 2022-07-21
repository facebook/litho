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

package com.facebook.litho.editor.flipper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import java.util.AbstractMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropDataTest {

  @Test
  fun `test KComponent with no props does not create a Props section`() {
    class ComponentWithNoProps : KComponent() {
      override fun ComponentScope.render(): Component? = null
    }

    val propData = DataUtils.getPropData(ComponentWithNoProps())
    val props = propData.find { it.name == "Props" }

    assertThat(props).isNull()
  }

  @Test
  fun `test KComponent props included in Props scetion`() {
    class ComponentWithProps(
        val a: Any? = null,
        val b: Int = 2,
        val c: Boolean = true,
    ) : KComponent() {
      override fun ComponentScope.render(): Component? = null
    }

    val propData = DataUtils.getPropData(ComponentWithProps())
    val props = propData.find { it.name == "Props" }

    assertThat(props).isNotNull
    props ?: return

    val flipperObject = props.value
    // Fallback to class name
    assertThat(flipperObject.getObject("a").get("value")).isEqualTo("class java.lang.Object")
    assertThat(flipperObject.getObject("b").getInt("value")).isEqualTo(2)
    assertThat(flipperObject.getObject("c").getBoolean("value")).isTrue
  }

  @Test
  fun `test KComponent PropWithInspectorSection props creates new scetion`() {
    class A : PropWithInspectorSection {
      override fun getFlipperLayoutInspectorSection(): AbstractMap.SimpleEntry<String, String> =
          AbstractMap.SimpleEntry("Inspector Section", "{\"A\":\"B\"}")
    }

    class ComponentWithProps(val a: Any = A()) : KComponent() {
      override fun ComponentScope.render(): Component? = null
    }

    val propData = DataUtils.getPropData(ComponentWithProps())
    val inspectorSection = propData.find { it.name == "Inspector Section" }
    assertThat(inspectorSection).isNotNull
    inspectorSection ?: return

    val flipperObject = inspectorSection.value
    assertThat(flipperObject.getString("A")).isEqualTo("B")
  }

  @Test
  fun `test KComponent PropWithDescription props included in Props scetion`() {
    class SingleDescription : PropWithDescription {
      override fun getFlipperLayoutInspectorPropDescription(): Any = "Hello World!"
    }

    class MapDescription : PropWithDescription {
      override fun getFlipperLayoutInspectorPropDescription(): Any =
          mapOf(
              "Hello" to 1,
              "World" to 2,
          )
    }

    class ComponentWithProps(
        val singleDescription: Any = SingleDescription(),
        val mapDescription: Any = MapDescription(),
    ) : KComponent() {
      override fun ComponentScope.render(): Component? = null
    }

    val propData = DataUtils.getPropData(ComponentWithProps())
    val props = propData.find { it.name == "Props" }
    assertThat(props).isNotNull
    props ?: return

    val flipperObject = props.value

    // Single values are overridden
    assertThat(flipperObject.getObject("singleDescription").getString("value"))
        .isEqualTo("Hello World!")

    // Map values are added directly to the prop list
    assertThat(flipperObject.getObject("Hello").getInt("value")).isEqualTo(1)
    assertThat(flipperObject.getObject("World").getInt("value")).isEqualTo(2)
  }
}
