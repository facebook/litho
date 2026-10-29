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

@file:OptIn(ExperimentalCompilerApi::class)

package com.facebook.litho.autodeps

import com.facebook.litho.AbstractCompilerTest
import com.facebook.litho.LithoCompilerPlugin
import com.facebook.litho.LithoCompilerPlugins
import com.facebook.litho.common.AutoDepsConfig
import com.facebook.litho.ir.AutoDepsPlugin
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Before
import org.junit.Test

/**
 * Behavioural tests for the auto-deps IR transform.
 *
 * Each test compiles a subject that calls one of the synthetic APIs in `AutoDepsTestApi`, runs the
 * compiled code, and asserts on the dependency array the API actually received. Asserting on
 * behaviour rather than on golden IR text keeps the suite alive across Kotlin upgrades -- the
 * plugin carries `compilercompat` precisely because that drift is real here.
 */
class AutoDepsTransformerTest : AbstractCompilerTest() {

  @Before
  fun resetRecorder() {
    DepsRecorder.reset()
  }

  @Test
  fun `infers a captured local`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          Layout { w -> w + count }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `leaves explicit deps alone`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          Layout("a", "b") { w -> w + count }
        }
        """,
    )
    assertThat(deps).containsExactly("a", "b")
  }

  @Test
  fun `treats Unit as an explicit empty dependency list`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          Layout(Unit) { w -> w + count }
        }
        """,
    )
    assertThat(deps).containsExactly(Unit)
  }

  @Test
  fun `infers into a non-vararg deps array`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          onValues(1, 2) { count.toString() }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `leaves an explicit non-vararg deps array alone`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          onValues(1, 2, deps = arrayOf("a", "b")) { count.toString() }
        }
        """,
    )
    assertThat(deps).containsExactly("a", "b")
  }

  @Test
  fun `treats an explicit empty array as no dependencies`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          onValues(deps = emptyArray()) { count.toString() }
        }
        """,
    )
    assertThat(deps).isEmpty()
  }

  @Test
  fun `does not capture the values the lambda binds`() {
    val deps = depsOf(
        """
        fun subject() {
          Layout { w -> w * 2 }
        }
        """,
    )
    assertThat(deps).isEmpty()
  }

  @Test
  fun `does not capture the receiver the lambda binds`() {
    val deps = depsOf(
        """
        fun subject() {
          onScoped { width + 1 }
        }
        """,
    )
    assertThat(deps).isEmpty()
  }

  @Test
  fun `reads properties of the enclosing receiver as paths, not as this`() {
    val deps = depsOf(
        """
        class Greeting(val name: String, val count: Int) {
          fun build(): Layout = Layout { w -> w + count + name.hashCode() }
        }

        fun subject() {
          Greeting("hi", 3).build()
        }
        """,
    )
    assertThat(deps).containsExactly(3, "hi")
  }

  @Test
  fun `rejects a var property of the enclosing receiver, by name`() {
    // Falling back to `this` would compile silently, and `this` compares equal to itself however
    // often `count` changed.
    val result = compileSubject(
        """
        class Counter(var count: Int) {
          fun build(): Layout = Layout { w -> w + count }
        }

        fun subject() {
          Counter(1).build()
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_MUTABLE_CAPTURE: 'count' is a var")
  }

  @Test
  fun `rejects a var property read through a custom getter`() {
    val result = compileSubject(
        """
        class Counter {
          var count: Int = 0
            get() = field + 1

          fun build(): Layout = Layout { w -> w + count }
        }

        fun subject() {
          Counter().build()
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_MUTABLE_CAPTURE: 'count' is a var")
  }

  @Test
  fun `does not lift a custom getter the lambda guards`() {
    // Lifted to the call, `requiredUser` would throw there, though the lambda never reads it.
    val deps = depsOf(
        """
        class Profile(private val user: String?) {
          val requiredUser: String
            get() = checkNotNull(user)

          fun build(): Layout = Layout { w -> if (w > 0) w + requiredUser.length else w }
        }

        fun subject() {
          Profile(null).build()
        }
        """,
    )
    assertThat(deps).hasSize(1)
  }

  @Test
  fun `does not lift an extension property the lambda guards`() {
    val deps = depsOf(
        """
        val List<Int>.lastItem: Int
          get() = this[size - 1]

        class Feed(val items: List<Int>) {
          fun build(): Layout = Layout { w -> if (items.isNotEmpty()) w + items.lastItem else w }
        }

        fun subject() {
          Feed(emptyList()).build()
        }
        """,
    )
    assertThat(deps).containsExactly(emptyList<Int>())
  }

  @Test
  fun `does not lift an open property a subclass overrides`() {
    // `Screen.binding` has a default getter, but the read dispatches to `Detached`'s, which throws.
    val deps = depsOf(
        """
        open class Screen {
          open val binding: String = "bound"

          fun build(attached: Boolean): Layout = Layout { w -> if (attached) w + binding.length else w }
        }

        class Detached : Screen() {
          private val current: String? = null
          override val binding: String
            get() = checkNotNull(current)
        }

        fun subject() {
          Detached().build(attached = false)
        }
        """,
    )
    assertThat(deps).hasSize(2)
    assertThat(deps.first()).isEqualTo(false)
  }

  @Test
  fun `reads an inherited final property through a subclass`() {
    val deps = depsOf(
        """
        open class Base(val title: String)

        class Sub(title: String) : Base(title) {
          fun build(): Layout = Layout { w -> w + title.length }
        }

        fun subject() {
          Sub("hi").build()
        }
        """,
    )
    assertThat(deps).containsExactly("hi")
  }

  @Test
  fun `does not read through an object under construction`() {
    // `config` is declared below `layout`, so it is still null when `layout` is initialised. Lifted
    // to the call, `config.title` would throw there; the lambda only reads it later.
    val deps = depsOf(
        """
        class Config(val title: String)

        class Screen {
          val layout = Layout { w -> w + config.title.length }
          val config = Config("hi")
        }

        fun subject() {
          Screen()
        }
        """,
    )
    assertThat(deps.single()?.javaClass?.simpleName).isEqualTo("Screen")
  }

  @Test
  fun `rejects a var read through an object under construction`() {
    val result = compileSubject(
        """
        class Screen {
          var count = 0

          init {
            Layout { w -> w + count }
          }
        }

        fun subject() {
          Screen()
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_MUTABLE_CAPTURE: 'count' is a var")
  }

  @Test
  fun `does not lift a property of a class from another module`() {
    // `Measured` is compiled in the test API. Its getter arrives deserialised, without the origin
    // that says it is a default one, so the path stops at its receiver: coarser, still sound.
    val deps = depsOf(
        """
        class Holder(val measured: Measured) {
          fun build(): Layout = Layout { w -> w + measured.value }
        }

        fun subject() {
          Holder(Measured(7)).build()
        }
        """,
    )
    assertThat(deps.single()).isInstanceOf(Measured::class.java)
  }

  @Test
  fun `reads a labelled outer receiver as a property path`() {
    val deps = depsOf(
        """
        class Outer(val outer: Int) {
          inner class Inner(val inner: Int) {
            fun build(): Layout = Layout { w -> w + this@Outer.outer + inner }
          }
        }

        fun subject() {
          val outer = Outer(3)
          outer.Inner(4).build()
        }
        """,
    )
    assertThat(deps).containsExactly(3, 4)
  }

  @Test
  fun `captures a loop variable`() {
    val deps = depsOf(
        """
        fun subject() {
          for (item in listOf(7)) {
            Layout { w -> w + item }
          }
        }
        """,
    )
    assertThat(deps).containsExactly(7)
  }

  @Test
  fun `emits dependencies in source order of first read, de-duplicated`() {
    val deps = depsOf(
        """
        fun subject() {
          val first = "a"
          val second = "b"
          Layout { w -> w + second.length + first.length + second.length }
        }
        """,
    )
    assertThat(deps).containsExactly("b", "a")
  }

  @Test
  fun `does not capture a nested lambda's parameter or a local declared inside`() {
    val deps = depsOf(
        """
        fun subject() {
          val items = listOf(1, 2)
          val offset = 10
          Layout { w ->
            var total = w
            items.forEach { item -> total += item + offset }
            total
          }
        }
        """,
    )
    assertThat(deps).containsExactly(listOf(1, 2), 10)
  }

  @Test
  fun `does not capture a name the lambda shadows`() {
    val deps = depsOf(
        """
        fun subject() {
          val name = "outer"
          Layout { w ->
            val name = "inner"
            w + name.length
          }
        }
        """,
    )
    assertThat(deps).isEmpty()
  }

  @Test
  fun `expands a captured local lambda to its own captures`() {
    val deps = depsOf(
        """
        fun subject() {
          val flag = true
          val toggle = { flag }
          onThing { toggle() }
        }
        """,
    )
    assertThat(deps).containsExactly(true)
  }

  @Test
  fun `rejects a captured var lambda rather than expanding its initialiser`() {
    // Expanding this would infer [first] and miss the reassignment entirely -- a silent
    // under-capture, which is the one thing the repairs must never be.
    val result = compileSubject(
        """
        fun subject() {
          val first = true
          val second = false
          var toggle = { first }
          toggle = { second }
          onThing { toggle() }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_MUTABLE_CAPTURE: 'toggle' is a var")
  }

  @Test
  fun `rejects an expandable var rather than expanding its initialiser`() {
    val result = compileSubject(
        """
        fun subject() {
          var measured = Measured(1)
          measured = Measured(2)
          onThing { measured.value }
        }
        """,
        plugins = withConfig("expand  com.facebook.litho.autodeps.Measured"),
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_UNEXPANDABLE_CAPTURE")
  }

  @Test
  fun `unions the captures of every DepsFrom parameter`() {
    val deps = depsOf(
        """
        fun subject() {
          val a = 1
          val b = 2
          onTwoThings(first = { a.toString() }, second = { b.toString() })
        }
        """,
    )
    assertThat(deps).containsExactly(1, 2)
  }

  @Test
  fun `rejects a captured var the lambda reads`() {
    // The dependency would be read now and `dep` later, when the lambda runs: here [0], while the
    // lambda would see 1.
    val result = compileSubject(
        """
        fun subject() {
          var dep = 0
          onThing { dep.toString() }
          dep = 1
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_MUTABLE_CAPTURE: 'dep' is a var")
  }

  @Test
  fun `rejects a captured var the lambda only assigns`() {
    // The assignment reads nothing, so without the check this would infer no dependencies at all.
    val result = compileSubject(
        """
        fun subject() {
          var sink = 0
          onThing { sink = 42 }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages)
        .contains("AUTODEPS_MUTABLE_CAPTURE: 'sink' is a var the lambda assigns")
  }

  @Test
  fun `reports a captured var the lambda reads and assigns once`() {
    val result = compileSubject(
        """
        fun subject() {
          var count = 0
          onThing { count += 1 }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(Regex("AUTODEPS_MUTABLE_CAPTURE").findAll(result.messages).count()).isEqualTo(1)
  }

  @Test
  fun `warns about a dependency that compares by identity`() {
    val result = compileSubject(
        """
        fun subject(callback: () -> Unit) {
          onThing { callback() }
        }
        """,
    )
    assertThat(result.messages).contains("AUTODEPS_UNSTABLE_CAPTURE")
  }

  @Test
  fun `rejects a DepsFrom argument that is not a lambda literal`() {
    val result = compileSubject(
        """
        fun subject() {
          val f = LayoutFunction { w -> w }
          Layout(func = f)
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_NON_LITERAL_TARGET")
  }

  @Test
  fun `keeps the deps default for a non-literal argument when the parameter opts in`() {
    val deps = depsOf(
        """
        fun subject() {
          val f = LayoutFunction { w -> w }
          KeepsDefault(func = f)
        }
        """,
    )
    assertThat(deps).containsExactly("default")
  }

  @Test
  fun `still infers a lambda literal when the parameter opts in`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          KeepsDefault { w -> w + count }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `still rejects a var capture when the parameter opts in`() {
    val result = compileSubject(
        """
        fun subject() {
          var count = 1
          count++
          KeepsDefault { w -> w + count }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_MUTABLE_CAPTURE")
  }

  @Test
  fun `still reports another argument's problem when one keeps the default`() {
    val result = compileSubject(
        """
        fun subject() {
          val f = {}
          var count = 1
          count++
          onKeepsDefaultAndThing(first = f) { println(count) }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_MUTABLE_CAPTURE")
    assertThat(result.messages).doesNotContain("AUTODEPS_NON_LITERAL_TARGET")
  }

  @Test
  fun `drops captures of an ignored type`() {
    val deps = depsOf(
        """
        fun subject() {
          val ambient = AmbientContext()
          val count = 1
          onThing { ambient.hashCode() + count }
        }
        """,
        plugins = withConfig("ignore  com.facebook.litho.autodeps.AmbientContext"),
    )
    assertThat(deps).containsExactly(1)
  }

  @Test
  fun `expands a capture whose type has no meaningful equivalence`() {
    val deps = depsOf(
        """
        fun subject() {
          val base = 7
          val measured = Measured(base)
          onThing { measured.value }
        }
        """,
        plugins = withConfig("expand  com.facebook.litho.autodeps.Measured"),
    )
    assertThat(deps).containsExactly(7)
  }

  @Test
  fun `rejects a capture of an expandable type it cannot expand`() {
    val result = compileSubject(
        """
        fun subject(measured: Measured) {
          onThing { measured.value }
        }
        """,
        plugins = withConfig("expand  com.facebook.litho.autodeps.Measured"),
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_UNEXPANDABLE_CAPTURE")
  }

  @Test
  fun `every Litho type named by the shipped default config resolves`() {
    // A misspelt name is a directive that never applies. Three of the original entries were wrong
    // -- one package, two types that do not exist yet -- and nothing caught it.
    //
    // The default is exempt from the unresolved warning by design, so the shipped lines are
    // re-supplied here as an author config, which is not. Android SDK names are dropped: they
    // genuinely do not resolve against this JVM-only test compilation.
    val shipped =
        checkNotNull(javaClass.getResourceAsStream("/litho-autodeps.conf")) {
              "the default config is not on the test classpath"
            }
            .bufferedReader()
            .readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() && !it.contains(" android.") }
    assertThat(shipped).isNotEmpty()

    val result = compileSubject("fun subject() {}", plugins = withConfig(*shipped.toTypedArray()))
    assertThat(result.messages).doesNotContain("does not resolve")
  }

  @Test
  fun `warns about an author config entry that does not resolve`() {
    val result = compileSubject(
        "fun subject() {}",
        plugins = withConfig("ignore  com.example.NoSuchType"),
    )
    assertThat(result.messages).contains("'ignore com.example.NoSuchType' does not resolve")
  }

  @Test
  fun `transforms with auto deps as the only registered plugin`() {
    // The annotation is the whole gate: nothing about the transform depends on another plugin
    // being installed alongside it.
    val result = compileSubject(
        """
            fun subject() {
              val count = 42
              Layout { w -> w + count }
            }
            """,
        plugins = listOf(AutoDepsPlugin()),
    )
    assertThat(result.exitCode)
        .describedAs(result.messages)
        .isEqualTo(KotlinCompilation.ExitCode.OK)
    assertThat(result.run().single()).containsExactly(42)
  }

  @Test
  fun `infers through the primary constructor of a class with arity overloads`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          Container(listOf(1, 2)) { children -> children.size + count }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `infers through a secondary constructor`() {
    // Only a class can put an overload in a secondary constructor, and the transform has to find
    // the `@Deps` slot on the constructor the call site actually picked.
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          Container(1) { child -> child + count }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `rejects a call to a local function that closes over the enclosing scope`() {
    // The walk does not enter a local function's body, so `count` would silently be missing from
    // the dependency list.
    val result = compileSubject(
        """
        fun subject() {
          val count = 42
          fun helper() = count.toString()
          onThing { helper() }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_LOCAL_FUNCTION_CALL")
  }

  @Test
  fun `reports the local function the lambda calls when another one reads the enclosing scope`() {
    // `a` reads nothing itself; `b` reads `count`. The error belongs on the call the lambda makes,
    // not on `b()` inside `a`, which is outside the lambda.
    val result = compileSubject(
        """
        fun subject() {
          val count = 42
          fun b() = count.toString()
          fun a() = b()
          onThing { a() }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("Subject.kt:7:13 AUTODEPS_LOCAL_FUNCTION_CALL: 'a' is")
    assertThat(result.messages).doesNotContain("'b' is")
  }

  @Test
  fun `rejects a call to a local function that assigns a captured var`() {
    val result = compileSubject(
        """
        fun subject() {
          var sink = 0
          fun set() {
            sink = 42
          }
          onThing { set() }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_LOCAL_FUNCTION_CALL: 'set' is")
  }

  @Test
  fun `rejects a delegated local property, naming the property`() {
    val result = compileSubject(
        """
        fun subject() {
          val text by lazy { "hi" }
          onThing { text.length }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages)
        .contains("AUTODEPS_LOCAL_FUNCTION_CALL: 'text' is a delegated local property")
  }

  @Test
  fun `does not report a local function that reads only ignored values`() {
    val deps = depsOf(
        """
        fun subject() {
          val ambient = AmbientContext()
          fun helper() = ambient.hashCode()
          onThing { helper() }
        }
        """,
        plugins = withConfig("ignore  com.facebook.litho.autodeps.AmbientContext"),
    )
    assertThat(deps).isEmpty()
  }

  @Test
  fun `follows a local function declared inside the lambda`() {
    // Declared inside, the function's body is part of the walk, so its reads are already
    // dependencies. Reporting the call would fail a closure the analysis handled correctly.
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          onThing {
            fun helper() = count.toString()
            helper()
          }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `rejects a callable reference to a local function that closes over the enclosing scope`() {
    val result = compileSubject(
        """
        fun subject() {
          val count = 42
          fun helper(n: Int) = n + count
          onThing { listOf(1).map(::helper) }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_LOCAL_FUNCTION_CALL")
  }

  @Test
  fun `rejects a method of a local class that closes over the enclosing scope`() {
    val result = compileSubject(
        """
        fun subject() {
          val count = 42
          class Helper {
            fun go() = count
          }
          onThing { Helper().go() }
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_LOCAL_FUNCTION_CALL")
  }

  @Test
  fun `allows a local class that closes over nothing`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          class Helper(val n: Int) {
            fun go() = n * 2
          }
          Layout { w -> w + count + Helper(w).go() }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `allows a call to a local function that closes over nothing`() {
    val deps = depsOf(
        """
        fun subject() {
          val count = 42
          fun twice(n: Int) = n * 2
          Layout { w -> w + count + twice(w) }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `does not hoist a path whose extension receiver reads a lambda parameter`() {
    // The dispatch root is `this@Owner`, but the extension receiver reads `w`, which the lambda
    // binds. Emitting the whole path would copy `w` into the deps array outside the lambda, where
    // it does not exist.
    val deps = depsOf(
        """
        class Holder(val value: Int)

        class Owner {
          val Holder.doubled: Int
            get() = value * 2

          fun build(): Layout = Layout { w -> w + Holder(w).doubled }
        }

        fun subject() {
          Owner().build()
        }
        """,
    )
    // The getter is walked like a call, so the dependency is the enclosing receiver: an
    // over-capture, but sound, and nothing the lambda binds escapes into the deps array.
    assertThat(deps).hasSize(1)
  }

  @Test
  fun `reads both receivers of a member extension property`() {
    // The getter is walked like a call, so each receiver is a dependency of its own: `this`, and
    // the `a` and `b` paths, which differ only in the extension receiver.
    val deps = depsOf(
        """
        data class Holder(val value: Int)

        class Owner(val a: Holder, val b: Holder) {
          val Holder.doubled: Int
            get() = value * 2

          fun build(): Layout = Layout { w -> w + a.doubled + b.doubled }
        }

        fun subject() {
          Owner(Holder(1), Holder(2)).build()
        }
        """,
    )
    assertThat(deps).hasSize(3)
    assertThat(deps.map { it.toString() }).contains("Holder(value=1)", "Holder(value=2)")
  }

  @Test
  fun `infers through an override that does not repeat the annotation`() {
    // The call resolves to `Sub.onThing`, which carries no annotations. Skipping it would pass an
    // empty array, and the forwarded `*deps` would then count as explicit at the base.
    val deps = depsOf(
        """
        class Sub : AutoDepsBase() {
          override fun onThing(vararg deps: Any?, func: () -> Unit) = super.onThing(*deps, func = func)
        }

        fun subject() {
          val count = 42
          Sub().onThing { count.toString() }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `infers through a member a subclass inherits`() {
    val deps = depsOf(
        """
        class Plain : AutoDepsBase()

        fun subject() {
          val count = 42
          Plain().onThing { count.toString() }
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `rejects a call to a declaration whose deps element type is not Any`() {
    assertMalformed(
        "onWronglyTypedDeps { count.toString() }",
        "the @Deps parameter of 'onWronglyTypedDeps' is neither",
    )
  }

  @Test
  fun `rejects a call to a declaration with no Deps parameter`() {
    assertMalformed(
        "onMissingDeps { count.toString() }",
        "'onMissingDeps' is annotated @AutoDeps but has no @Deps parameter",
    )
  }

  @Test
  fun `rejects a call to a declaration with no DepsFrom parameter`() {
    assertMalformed("onMissingDepsFrom()", "'onMissingDepsFrom' has no @DepsFrom parameter")
  }

  @Test
  fun `rejects a call that omits a defaulted DepsFrom argument`() {
    assertMalformed("onDefaultedDepsFrom()", "this call omits @DepsFrom parameter 'func'")
  }

  @Test
  fun `infers through a super constructor delegation`() {
    // `super(…)` is an IrDelegatingConstructorCall, not an IrConstructorCall. Missing it would let
    // the subclass take the parameter's default -- an empty array, silently.
    val deps = depsOf(
        """
        class Sub(count: Int) : OpenContainer(func = { children -> children.size + count })

        fun subject() {
          Sub(42)
        }
        """,
    )
    assertThat(deps).containsExactly(42)
  }

  @Test
  fun `runs auto deps before hook identity`() {
    val result = compileSubject(
        """
            @com.facebook.litho.annotations.Hook
            fun com.facebook.litho.ComponentScope.useSubject(count: Int) {
              useAutoDepsThing { count.toString() }
            }
            """,
    )
    assertThat(result.exitCode)
        .describedAs(result.messages)
        .isEqualTo(KotlinCompilation.ExitCode.OK)
  }

  /**
   * Compiles [call] against a malformed declaration from the test API, which was compiled without
   * the declaration checks, and asserts the call site is rejected for [reason] rather than given an
   * empty array.
   */
  private fun assertMalformed(call: String, reason: String) {
    val result = compileSubject(
        """
        fun subject() {
          val count = 42
          $call
        }
        """,
    )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(result.messages).contains("AUTODEPS_MALFORMED_DECLARATION: $reason")
  }

  /**
   * The shipped plugins, with Auto Deps reconfigured by [lines].
   *
   * Supplied as author config rather than as defaults, so an entry that does not resolve warns.
   */
  private fun withConfig(vararg lines: String): List<LithoCompilerPlugin> {
    val config =
        AutoDepsConfig.load() +
            AutoDepsConfig.parse(lines.joinToString("\n"), "test-config", isDefault = false)
    return listOf(AutoDepsPlugin(config)) +
        LithoCompilerPlugins.ALL.filterNot { it is AutoDepsPlugin }
  }

  /** Compiles [body] against the synthetic APIs, with both IR transforms installed. */
  private fun compileSubject(
      body: String,
      plugins: List<LithoCompilerPlugin> = LithoCompilerPlugins.ALL,
  ): JvmCompilationResult = compile(
      SourceFile.kotlin(
          "Subject.kt",
          "import com.facebook.litho.autodeps.*\n\n" + body.trimIndent(),
      ),
      plugins = plugins,
  )

  private fun depsOf(
      body: String,
      plugins: List<LithoCompilerPlugin> = LithoCompilerPlugins.ALL,
  ): List<Any?> {
    val result = compileSubject(body, plugins)
    assertThat(result.exitCode)
        .describedAs(result.messages)
        .isEqualTo(KotlinCompilation.ExitCode.OK)
    return result.run().single()
  }

  /** Invokes `subject()` in the compiled output and returns what each API call recorded. */
  private fun JvmCompilationResult.run(): List<List<Any?>> {
    DepsRecorder.reset()
    val subject = classLoader.loadClass("SubjectKt").getDeclaredMethod("subject")
    subject.invoke(null)
    return DepsRecorder.all()
  }
}
