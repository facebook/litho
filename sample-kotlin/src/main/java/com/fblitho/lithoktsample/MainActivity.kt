package com.fblitho.lithoktsample

import android.arch.lifecycle.Observer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.facebook.litho.LithoView
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.fblitho.lithoktsample.lithography.sections.LithoFeedSection
import android.arch.lifecycle.ViewModelProviders
import com.facebook.litho.Component
import com.fblitho.lithoktsample.lithography.data.DataFetcher
import com.fblitho.lithoktsample.lithography.data.DecadeViewModel
import com.fblitho.lithoktsample.lithography.data.Model


class MainActivity : AppCompatActivity() {

    private val sectionContext: SectionContext by lazy { SectionContext(this) }
    private val lithoView: LithoView by lazy { LithoView(sectionContext) }
    private val fetcher: DataFetcher by lazy { DataFetcher(viewModel.model) }
    private val viewModel: DecadeViewModel by lazy {
        ViewModelProviders.of(this).get(DecadeViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.init()
        viewModel.model.observe(this, Observer { model -> setList(model!!) })
        setContentView(lithoView)
    }

    private fun setList(model: Model) {

        if (lithoView.componentTree == null) {
            lithoView.setComponent(createRecyclerComponent(model))
        } else {
            lithoView.setComponentAsync(createRecyclerComponent(model))
        }
    }

    private fun createRecyclerComponent(model: Model): Component<RecyclerCollectionComponent> {

        return RecyclerCollectionComponent
                .create(sectionContext)
                .section(LithoFeedSection.create(sectionContext)
                        .decades(model.decades)
                        .fetcher(fetcher)
                        .loading(model.isLoading)
                        .build())
                .disablePTR(true)
                .build()
    }
}
