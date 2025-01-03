// @nolint
// PROCESSOR: ErrorOnlyProcessor
// EXPECTED:
// e: file:///Hooks.kt:10:40 Hooks can be only called from render() or another @Hook annotated method. See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks
// e: file:///Hooks.kt:17:7 Hooks should not be called from conditionals (if, for, while, or when). See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks
// e: file:///Hooks.kt:29:15 Hooks should not be called from conditionals (if, for, while, or when). See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks
// e: file:///Hooks.kt:37:20 Hooks should not be called from conditionals (if, for, while, or when). See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks
// e: file:///Hooks.kt:53:28 Hooks should not be called from conditionals (if, for, while, or when). See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks
// e: file:///Hooks.kt:61:21 Hooks should not be called from conditionals (if, for, while, or when). See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks
// e: file:///Hooks.kt:68:22 Hooks should not be called from conditionals (if, for, while, or when). See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks
// e: file:///Hooks.kt:74:22 Hooks should not be called from conditionals (if, for, while, or when). See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks
// END

// FILE: Hooks.kt

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.animated.useBinding
import com.facebook.litho.annotations.Hook
import com.facebook.litho.useState

class HookInNonHookCallSite {
  fun ComponentScope.useOurBinding() = useBinding { true }
}

class HookInIfCondition {
  @Hook
  fun ComponentScope.useConditionalState(): Boolean {
    if (true) {
      useState { true }
      return true
    } else {
      return false
    }
  }
}

class HookInWhileLoop {
  @Hook
  fun ComponentScope.useConditionalState() {
    while (true) {
      val k = useState { 0 }
    }
  }
}

class HookInWhenCondition(val color: Color) : com.facebook.litho.KComponent() {
  override fun ComponentScope.render(): Component {
    when (color) {
      Color.RED -> useState {}
      Color.GREEN -> println("green")
      Color.BLUE -> println("blue")
    }
    return Column()
  }
}

enum class Color {
  RED,
  GREEN,
  BLUE
}

class HookInWhileLoopInRender : KComponent() {
  override fun ComponentScope.render(): Component {
    while (true) { val k = useState {} }
    return Column()
  }
}

class HookInForLoop {
  @Hook
  fun ComponentScope.useConditionalState() {
    for (i in 1..3) useState {}
  }
}

class HookInForEachLoop {
  @Hook
  fun ComponentScope.useConditionalState() {
    (1..3).forEach { useState {} }
  }
}

class HookInForEachLoopInRender : KComponent() {
  override fun ComponentScope.render(): Component {
    (1..3).forEach { useState {} }
    return Column()
  }
}
