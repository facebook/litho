package com.facebook.samples.lithoktbarebones

import com.facebook.litho.annotations.Event

@Event
data class BoxItemChangedEvent(
    val newColor: Int,
    val newStatus: String,
    val highlightedItemIndex: Int,
    val newBoolean: Boolean
)
