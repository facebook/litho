package com.fblitho.lithoktsample.lithography.sections

import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.fblitho.lithoktsample.lithography.components.SingleImageComponent

/**
 * Created by pasqualea on 10/13/17.
 */
@GroupSectionSpec
object ImagesSectionSpec {
    @OnCreateChildren
    fun onCreateChildren(c: SectionContext, @Prop images: List<String>): Children {
        return Children.create()
                .child(DataDiffSection.create<String>(c)
                        .data(images)
                        .renderEventHandler(ImagesSection.onRender(c)))
                .build();
    }

    @JvmStatic
    @OnEvent(RenderEvent::class)
    fun onRender(
            c: SectionContext, @FromEvent model: String): RenderInfo {
        return ComponentRenderInfo.create()
                .component(SingleImageComponent.create(c).image(model).imageAspectRatio(2f).build())
                .build();
    }
}