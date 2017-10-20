package com.fblitho.droidcon17demo.lithography.data

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.facebook.litho.StateHandler

/**
 * Created by pasqualea on 10/18/17.  */

class DecadeViewModel : ViewModel() {

    val model = MutableLiveData<Model>()

    fun init() {
        if (model.value == null) {
            model.value = Model(DataCreator.createPageOfData(0), false)
        }
    }
}
