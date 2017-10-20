package com.fblitho.droidcon17demo.lithography.sections

import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.annotations.OnViewportChanged
import com.facebook.litho.sections.common.SingleComponentSection
import com.fblitho.droidcon17demo.lithography.components.LoadingComponent
import com.fblitho.droidcon17demo.lithography.data.Decade
import com.fblitho.droidcon17demo.lithography.data.Fetcher

@GroupSectionSpec
object LithoFeedSectionSpec {
    @OnCreateChildren
    fun onCreateChildren(c: SectionContext,
                         @Prop decades: List<Decade>,
                         @Prop loading: Boolean): Children {
        val children = Children.create()

        decades.forEach {
            children.child(
                    DecadeSection.create(c)
                            .decade(it)
                            .key(it.year.toString())
                            .build())
        }

        if (loading) {
            children.child(
                    SingleComponentSection.create(c).
                            component(LoadingComponent.create(c).build()))
        }

        return children.build()
    }

    @OnViewportChanged
    fun onViewportChanged(
            c: SectionContext,
            firstVisile: Int,
            lastVisible: Int,
            totalCount: Int,
            fistFullyVisible: Int,
            lastFullyVisible: Int,
            @Prop fetcher: Fetcher,
            @Prop decades: List<Decade>) {
        val threshold = 2
        if (totalCount - lastVisible < threshold) {
            fetcher.fetchMoreData(decades.last().year)
        }
    }
}