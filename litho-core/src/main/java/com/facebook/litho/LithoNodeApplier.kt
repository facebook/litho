package com.facebook.litho

import androidx.compose.runtime.*
import com.facebook.yoga.YogaFlexDirection

val LocalParentContext = compositionLocalOf<ComponentContext> { error("parent context missing") }

class LithoNodeApplier(root: LithoNode) : AbstractApplier<LithoNode>(root) {

  override fun insertTopDown(index: Int, instance: LithoNode) {
    current.addChildAt(instance, index)
  }

  override fun insertBottomUp(index: Int, instance: LithoNode) {
    // Ignored as the tree is built top-down.
  }

  override fun remove(index: Int, count: Int) {
    throw UnsupportedOperationException("remove not supported")
  }

  override fun move(from: Int, to: Int, count: Int) {
    throw UnsupportedOperationException("move not supported")
  }

  override fun onClear() {
    throw UnsupportedOperationException("clear not supported")
  }
}

@Composable
fun Column(content: @Composable () -> Unit) {
  val parent = LocalParentContext.current
  ComposeNode<LithoNode, LithoNodeApplier>(factory = {
    LithoNode().apply {
      val component = Column.create(parent).build()
      val context = ComponentContext.withComponentScope(
          parent,
          component,
          ComponentKeyUtils.generateGlobalKey(parent, component)
      )
      val info = ScopedComponentInfo(context)
      flexDirection(YogaFlexDirection.COLUMN)
      appendComponent(info)
    }
  }, update = {}, content = content)
}