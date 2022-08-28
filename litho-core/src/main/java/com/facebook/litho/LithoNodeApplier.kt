package com.facebook.litho

import androidx.compose.runtime.AbstractApplier
import java.lang.UnsupportedOperationException

class LithoNodeApplier(root: LithoNode): AbstractApplier<LithoNode>(root) {

    override fun insertTopDown(index: Int, instance: LithoNode) {
        current.addChild(instance, index)
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
