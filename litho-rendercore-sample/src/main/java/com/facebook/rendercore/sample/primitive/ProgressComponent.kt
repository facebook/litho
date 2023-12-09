// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore.sample.primitive

import android.widget.ProgressBar
import com.facebook.rendercore.primitives.ExactSizeConstraintsLayoutBehavior
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.ViewAllocator

fun ProgressComponent(id: Long, progress: Int): Primitive {
  return Primitive(
      layoutBehavior = ExactSizeConstraintsLayoutBehavior,
      mountBehavior =
          MountBehavior(
              id = id,
              contentAllocator = ViewAllocator { ProgressBar(it) },
          ) {
            progress.bindTo(ProgressBar::setProgress, 0)
          })
}
