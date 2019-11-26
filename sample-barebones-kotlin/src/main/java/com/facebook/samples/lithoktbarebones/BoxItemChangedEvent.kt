package com.facebook.samples.lithoktbarebones

import com.facebook.litho.annotations.Event

@Event
class BoxItemChangedEvent {
  var newColor = 0
  var newStatus = ""
  var highlightedItemIndex = -1
}
