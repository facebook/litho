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

package com.facebook.litho.diagnostics

import com.facebook.litho.AbstractCompilerTest
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class LithoHookUsageCheckerTest : AbstractCompilerTest() {

  @Test
  fun `doesn't complain about hook used in render function`() {
    val content =
        """
          |import com.facebook.litho.KComponent
          |import com.facebook.litho.Component
          |import com.facebook.litho.ComponentScope
          |import com.facebook.litho.Column
          |import com.facebook.litho.useState
          |import com.facebook.litho.core.width
          |import com.facebook.litho.Style
          |import com.facebook.rendercore.dp
          |
          |class TestClass : KComponent() {
          |  override fun ComponentScope.render(): Component {
          |    val state = useState { true }
          |    return Column()
          |  }
          |}
          """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.OK)
  }

  @Test
  fun `doesn't complain about hook used in function annotated @Hook`() {
    val content =
        """
        |import com.facebook.litho.KComponent
        |import com.facebook.litho.Component
        |import com.facebook.litho.ComponentScope
        |import com.facebook.litho.useState
        |import com.facebook.litho.annotations.Hook
        |
        |class TestClass {
        |
        |  @Hook
        |  fun ComponentScope.useOurState() = useState { true }
        |  
        |  @Hook
        |  fun ComponentScope.useOurBetterState() = useOurState()
        |}
        """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.OK)
  }

  @Test
  fun `complain about hook used in function without @Hook annotation`() {
    val content =
        """
      |import com.facebook.litho.KComponent
      |import com.facebook.litho.Component
      |import com.facebook.litho.ComponentScope
      |import com.facebook.litho.animated.useBinding
      |
      |class TestClass {
      |  fun ComponentScope.useOurBinding() = /*issue*/useBinding { true }/*issue*/
      |}
      """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("Hooks") // use site
  }

  @Test
  fun `complain about hook used in if else`() {
    val content =
        """
        |import com.facebook.litho.KComponent
        |import com.facebook.litho.Component
        |import com.facebook.litho.ComponentScope
        |import com.facebook.litho.useState
        |import com.facebook.litho.annotations.Hook
        |
        |class TestClass {
        |  @Hook
        |  fun ComponentScope.useConditionalState(): Boolean {
        |    if(context.globalKey.isEmpty()) {
        |      /*issue*/useState{true}/*issue*/
        |      return true
        |    } else {
        |      return false
        |    }
        |  }
        |}
        """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("Hooks") // conditional
  }

  @Test
  fun `complain about hook used in while`() {
    val content =
        """
        |import com.facebook.litho.KComponent
        |import com.facebook.litho.Component
        |import com.facebook.litho.ComponentScope
        |import com.facebook.litho.annotations.Hook
        |import com.facebook.litho.useState
        |
        |class TestClass{
        |
        |  @Hook
        |  fun ComponentScope.useConditionalState() {
        |    while(true) {
        |      val k = /*issue*/useState{ 0 }/*issue*/
        |    }
        |  }
        |}
        """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("Hooks") // conditional
  }

  @Test
  fun `complain about hook used in when in render`() {
    val content =
        """
        |
        |import com.facebook.litho.KComponent
        |import com.facebook.litho.Column
        |import com.facebook.litho.Component
        |import com.facebook.litho.ComponentScope
        |import com.facebook.litho.useState
        |import com.facebook.litho.annotations.Hook
        |
        |class TestClass(val color: Color) : com.facebook.litho.KComponent() {
        |  
        |  override fun ComponentScope.render(): Component {
        |    when(color) {
        |      Color.RED -> /*issue*/useState{}/*issue*/
        |      Color.GREEN -> println("green")
        |      Color.BLUE -> println("blue")
        |    }
        |    return Column()
        |  }
        |}
        |
        |enum class Color {
        |    RED, GREEN, BLUE
        |}
        """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("Hooks") // conditional
  }

  @Test
  fun `complain about hook used used in while in render`() {
    val content =
        """
        |
        |import com.facebook.litho.KComponent
        |import com.facebook.litho.Column
        |import com.facebook.litho.Component
        |import com.facebook.litho.ComponentScope
        |import com.facebook.litho.useState
        |import com.facebook.litho.annotations.Hook
        |
        |class TestClass : KComponent() {
        |  
        |  override fun ComponentScope.render(): Component {
        |    while(true) { val k = /*issue*/useState{}/*issue*/ }
        |    return Column()
        |  }
        |}
        """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("Hooks") // conditional
  }

  @Test
  fun `complain about hook used in for loop`() {
    val content =
        """
        |
        |import com.facebook.litho.KComponent
        |import com.facebook.litho.Component
        |import com.facebook.litho.ComponentScope
        |import com.facebook.litho.useState
        |import com.facebook.litho.annotations.Hook
        |
        |class TestClass {
        |
        |  @Hook
        |  fun ComponentScope.useConditionalState() {
        |    for(i in 1..3) {/*issue*/useState{}/*issue*/}
        |  }
        |}
        """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("Hooks") // conditional
  }

  @Test
  fun `complain about hook used in forEach loop`() {
    val content =
        """
        |
        |import com.facebook.litho.KComponent
        |import com.facebook.litho.Component
        |import com.facebook.litho.ComponentScope
        |import com.facebook.litho.useState
        |import com.facebook.litho.annotations.Hook
        |
        |class TestClass {
        |
        |  @Hook
        |  fun ComponentScope.useConditionalState() {
        |    (1..3).forEach {/*issue*/useState{}/*issue*/}
        |  }
        |}
        """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("Hooks") // conditional
  }

  @Test
  fun `complain about hook used in forEach loop in render`() {
    val content =
        """
        |
        |import com.facebook.litho.KComponent
        |import com.facebook.litho.Column
        |import com.facebook.litho.Component
        |import com.facebook.litho.ComponentScope
        |import com.facebook.litho.useState
        |import com.facebook.litho.annotations.Hook
        |
        |class TestClass : KComponent(){
        |  
        |  override fun ComponentScope.render(): Component {
        |    (1..3).forEach {/*issue*/useState{}/*issue*/}
        |    return Column()
        |  }
        |}
        """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("Hooks") // conditional
  }

  @Test
  fun `doesn't complain about useState, useBinding or useEffect functions if they are not the litho ones`() {
    val content =
        """
        |
        |class TestClass {
        |
        |  fun useHooks() {
        |    val i = useEffect()
        |    val j = useBinding()
        |    val k = useState()
        |  }
        |  
        |  fun useState(): Boolean = false
        |  
        |  fun useEffect(): Boolean = true
        |  
        |  fun useBinding(): Boolean = true
        |}
        """
            .trimMargin()
    val result = compile(content)
    assertThat(result.exitCode).isEqualTo(ExitCode.OK)
  }

  private fun compile(@Language("kotlin") source: String): CompilationResult {
    return compile(SourceFile.kotlin("TestClass.kt", source))
  }
}
