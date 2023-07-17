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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.testing.LithoViewRule
import java.util.AbstractMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropDataTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `test KComponent with no props does not create a Props section`() {
    class ComponentWithNoProps : KComponent() {
      override fun ComponentScope.render(): Component? = null
    }

    val propData = DataUtils.getPropData(ComponentWithNoProps())
    val props = propData.find { it.name == "Props" }

    assertThat(props).isNull()
  }

  @DataClassGenerate(toString = Mode.KEEP, equalsHashCode = Mode.KEEP)
  data class Data(val d: String)

  @Test
  fun `test KComponent props included in Props scetion`() {

    class ComponentWithProps(
        val a: Data = Data("data"),
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
    assertThat(flipperObject.getObject("a").get("value")).isEqualTo("Data(d=data)")
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

    val mapDescription = flipperObject.getObject("mapDescription")
    assertThat(mapDescription.getObject("Hello").getInt("value")).isEqualTo(1)
    assertThat(mapDescription.getObject("World").getInt("value")).isEqualTo(2)
  }

  @Test
  fun `test LayoutSpec with no props does not create a Props section`() {
    val component = LayoutNoProps.create(lithoViewRule.context).build()

    val propData = DataUtils.getPropData(component)
    val props = propData.find { it.name == "Props" }

    assertThat(props).isNull()
  }

  @Test
  fun `test LayoutSpec props included in Props scetion`() {
    val component = LayoutWithBasicProps.create(lithoViewRule.context).a(null).b(2).c(true).build()
    val propData = DataUtils.getPropData(component)
    val props = propData.find { it.name == "Props" }

    assertThat(props).isNotNull
    props ?: return

    val flipperObject = props.value
    // Fallback to class name
    assertThat(flipperObject.getObject("a").get("value")).isEqualTo("null")
    assertThat(flipperObject.getObject("b").getInt("value")).isEqualTo(2)
    assertThat(flipperObject.getObject("c").getBoolean("value")).isTrue
  }

  @Test
  fun `test LayoutSpec color props included in Props scetion`() {
    val component =
        LayoutWithColorProps.create(lithoViewRule.context)
            .a(Color.WHITE)
            .b(ColorDrawable(Color.RED))
            .build()
    val propData = DataUtils.getPropData(component)
    val props = propData.find { it.name == "Props" }

    assertThat(props).isNotNull
    props ?: return

    val flipperObject = props.value
    val a = flipperObject.getObject("a")
    assertThat(a.getInt("value")).isEqualTo(Color.WHITE)
    assertThat(a.getString("__type__")).isEqualTo("color")

    // ColorDrawables are treated as colors
    val b = flipperObject.getObject("b")
    assertThat(b.getInt("value")).isEqualTo(Color.RED)
    assertThat(b.getString("__type__")).isEqualTo("color")
  }

  @Test
  fun `test LayoutSpec PropWithInspectorSection props creates new scetion`() {
    val propWithInspectorSection =
        object : PropWithInspectorSection {
          override fun getFlipperLayoutInspectorSection(): AbstractMap.SimpleEntry<String, String> =
              AbstractMap.SimpleEntry("New Section", "{\"A\":\"B\"}")
        }

    val component =
        LayoutWithPropWithInspectorSection.create(lithoViewRule.context)
            .a(propWithInspectorSection)
            .build()

    val propData = DataUtils.getPropData(component)
    val inspectorSection = propData.find { it.name == "New Section" }
    assertThat(inspectorSection).isNotNull
    inspectorSection ?: return

    val flipperObject = inspectorSection.value
    assertThat(flipperObject.getString("A")).isEqualTo("B")
  }

  @Test
  fun `test LayoutSpec PropWithDescription props included in Props scetion`() {
    val propWithOverride =
        object : PropWithDescription {
          override fun getFlipperLayoutInspectorPropDescription(): Any = "Hello World!"
        }

    val propWithMapOverride =
        object : PropWithDescription {
          override fun getFlipperLayoutInspectorPropDescription(): Any =
              mapOf(
                  "Hello" to 1,
                  "World" to 2,
              )
        }

    val component =
        LayoutWithPropWithDescription.create(lithoViewRule.context)
            .a(propWithOverride)
            .b(propWithMapOverride)
            .build()

    val propData = DataUtils.getPropData(component)
    val props = propData.find { it.name == "Props" }
    assertThat(props).isNotNull
    props ?: return

    val flipperObject = props.value

    // Single values are overridden
    assertThat(flipperObject.getObject("a").getString("value")).isEqualTo("Hello World!")

    val mapDescription = flipperObject.getObject("b")
    assertThat(mapDescription.getObject("Hello").getInt("value")).isEqualTo(1)
    assertThat(mapDescription.getObject("World").getInt("value")).isEqualTo(2)
  }
}
