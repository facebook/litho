// @nolint
// PROCESSOR: SimpleClassDumpProcessor
// class HookInRender
//   val a: String
//   val b: Int
//   fun render(): Component
// class NonLitho
//   val age: Int
//   val name: String
//   fun useBinding(): Boolean
//   fun useEffect(): Boolean
//   fun useHooks(): Unit
//   fun useState(): Boolean
// EXPECTED:
// END

// FILE: HookInRender.kt

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.useState

class HookInRender(private val a: String, val b: Int) : KComponent() {
  override fun ComponentScope.render(): Component {
    val reversed = useState { false }
    return Column(isReversed = reversed.value)
  }
}

// FILE: NonLitho.kt

class NonLitho {
  val name = "Some Name"
  val age get() = 42

  fun useHooks() {
    useEffect()
    useBinding()
    useState()
  }

  fun useEffect(): Boolean = true
  fun useBinding(): Boolean = true
  fun useState(): Boolean = false
}

// FILE: HookInNestedComponent.kt

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.annotations.Hook

fun foo() = object : KComponent() {
  override fun ComponentScope.render(): Component {
    val reversed = useState { false }
    return Column(isReversed = reversed.value)
  }
}

// FILE: HookInNestedFunction.kt

import com.facebook.litho.ComponentScope
import com.facebook.litho.useCached
import com.facebook.litho.annotations.Hook

fun bar() {
  @Hook fun ComponentScope.useZero(): Int = useCached { 0 }
}
