/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.fblitho.lithoktsample.logging

import com.facebook.litho.PerfEvent

/**
 * The Litho logging system is designed to be stateless and have batches of data
 * sent to the server for processing, to avoid overhead on the device. This
 * sample implementation is to illustrate the data you can work with, not on
 * how to actually implement logging on the client.
 */
object PerfEventStore {
    private val events: MutableMap<PerfEvent, EventData> = HashMap()

    fun obtain(eventId: Int, instanceKey: Int): PerfEvent {
        val event = SamplePerfEvent(eventId, instanceKey)
        events[event] = EventData(System.nanoTime())
        return event
    }

    fun markerAnnotate(event: SamplePerfEvent, annotationKey: String, annotationValue: Any) {
        // We can be pretty lose with lookup here as we control event creation.
        // Also, if we misuse the APIs in the framework, we don't want that to silently fail.
        events[event]!!.addAnnotation(annotationKey, annotationValue)
    }

    fun markerPoint(event: SamplePerfEvent, eventName: String) {
        events[event]!!.addMarker(System.nanoTime(), eventName)
    }

    fun release(event: PerfEvent): EventData {
        val data = events.remove(event)!!
        data.endTimeNs = System.nanoTime()
        return data
    }
}