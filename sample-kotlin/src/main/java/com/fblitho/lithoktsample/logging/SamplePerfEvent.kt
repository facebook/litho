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

class SamplePerfEvent(private val markerId: Int, private val instanceKey: Int) : PerfEvent {

    override fun getInstanceKey(): Int = instanceKey

    override fun getMarkerId(): Int = markerId

    override fun markerAnnotate(annotationKey: String, annotationValue: String) {
        PerfEventStore.markerAnnotate(this, annotationKey, annotationValue)
    }

    override fun markerAnnotate(annotationKey: String, annotationValue: Double) {
        PerfEventStore.markerAnnotate(this, annotationKey, annotationValue)
    }

    override fun markerAnnotate(annotationKey: String, annotationValue: Int) {
        PerfEventStore.markerAnnotate(this, annotationKey, annotationValue)
    }

    override fun markerAnnotate(annotationKey: String, annotationValue: Boolean) {
        PerfEventStore.markerAnnotate(this, annotationKey, annotationValue)
    }

    override fun markerAnnotate(annotationKey: String, annotationValue: Array<out String>) {
        // Using Lists here only to make formatting easier. Otherwise we'd have to
        // keep separate stores for the types (which would be the right thing to do).
        PerfEventStore.markerAnnotate(this, annotationKey, annotationValue.toList())
    }

    override fun markerAnnotate(annotationKey: String, annotationValue: Array<out Double>) {
        PerfEventStore.markerAnnotate(this, annotationKey, annotationValue.toList())
    }

    override fun markerAnnotate(annotationKey: String, annotationValue: IntArray) {
        PerfEventStore.markerAnnotate(this, annotationKey, annotationValue.toList())
    }

    override fun markerPoint(eventName: String) {
        PerfEventStore.markerPoint(this, eventName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SamplePerfEvent

        if (markerId != other.markerId) return false
        if (instanceKey != other.instanceKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = markerId
        result = 31 * result + instanceKey
        return result
    }
}