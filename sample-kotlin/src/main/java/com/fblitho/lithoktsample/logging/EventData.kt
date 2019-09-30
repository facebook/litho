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

data class EventData(val startTimeNs: Long) {
    private val annotations: MutableMap<String, Any> = HashMap()
    private val markers: MutableList<Pair<Long, String>> = ArrayList()

    fun addAnnotation(annotationKey: String, annotationValue: Any) : EventData = apply {
        annotations[annotationKey] = annotationValue
    }

    fun getAnnotations(): Map<String, Any> = annotations

    fun addMarker(timeNs: Long, eventName: String) : EventData  = apply {
        markers.add(Pair(timeNs, eventName))
    }

    fun getMarkers(): List<Pair<Long, String>> = markers

    var endTimeNs: Long? = null
}
