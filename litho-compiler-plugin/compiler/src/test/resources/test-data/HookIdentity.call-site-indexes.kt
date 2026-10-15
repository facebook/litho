// @nolint
// PROCESSOR: HookIdentityCallProcessor
// enterHook arguments=1
// enterHook arguments=2
// enterHook arguments=1
// enterHook arguments=1
// EXPECTED:
// END

// FILE: HookIdentityCallSiteIndexes.kt

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.useState

fun ComponentScope.render(value: Int) {
  useState { value }
}

class HookIdentityCallSiteIndexes(private val enabled: Boolean) : KComponent() {
  override fun ComponentScope.render(): Component? {
    if (enabled) {
      useState { 0 }
    }

    for (index in 0 until 2) {
      useState { index }
    }

    repeat(2) { index -> useState { index } }
    return null
  }
}
