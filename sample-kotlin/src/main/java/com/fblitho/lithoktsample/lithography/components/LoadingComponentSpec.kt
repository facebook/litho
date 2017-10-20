package com.fblitho.lithoktsample.lithography.components

import android.graphics.Color
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.widget.Progress
import com.facebook.yoga.YogaJustify

/**
 * Created by pasqualea on 10/19/17.
 */
@LayoutSpec
object LoadingComponentSpec {

    @OnCreateLayout
    fun onCreateLayout(c: ComponentContext): ComponentLayout {
        return Row.create(c)
                .justifyContent(YogaJustify.CENTER)
                .child(Progress.create(c)
                        .color(Color.DKGRAY)
                        .widthDip(50f))
                .build()
    }
}