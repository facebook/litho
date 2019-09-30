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

import android.util.Log
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentsLogger
import com.facebook.litho.FrameworkLogEvents
import com.facebook.litho.PerfEvent
import com.facebook.litho.TreeProps
import java.util.concurrent.atomic.AtomicInteger

class SampleComponentsLogger : ComponentsLogger {
    private val tag = "LITHOSAMPLE"
    private val instanceCounter: AtomicInteger = AtomicInteger(0)

    override fun newPerformanceEvent(c: ComponentContext, eventId: Int): PerfEvent {
        return PerfEventStore.obtain(eventId, instanceCounter.incrementAndGet())
    }

    override fun logPerfEvent(event: PerfEvent) {
        printEventData(event, PerfEventStore.release(event))
    }

    override fun cancelPerfEvent(event: PerfEvent) {
        PerfEventStore.release(event)
    }

    override fun isTracing(logEvent: PerfEvent): Boolean = true

    override fun getExtraAnnotations(treeProps: TreeProps?): Map<String, String> =
            treeProps?.get(LogContext::class.java)?.let {
                mapOf("log_context" to it.toString())
            } ?: emptyMap()

    private fun printEventData(event: PerfEvent, data: EventData) {
        val totalTimeMs = (data.endTimeNs!! - data.startTimeNs) / (1000 * 1000f)
        var msg = """
        |--- <PERFEVENT> ---
        |type: ${getEventNameById(event.markerId)}
        |total time: ${totalTimeMs.withDigits(2)}ms
        """

        val annotations = formatAnnotations(data)
        if (annotations.isNotEmpty()) {
            msg += "|annotations: $annotations\n"
        }

        val markers = formatMarkers(data)
        if (markers.isNotEmpty()) {
            msg += "|markers: $markers\n"
        }

        msg += "|--- </PERFEVENT> ---\n"

        Log.v(tag, msg.trimMargin())
    }

    private fun formatMarkers(data: EventData): String =
            data.getMarkers().joinToString {
                "${((it.first - data.startTimeNs) / (1000 * 1000f)).withDigits(
                        2)}ms -> ${it.second}"
            }

    private fun formatAnnotations(data: EventData): String =
            data.getAnnotations().map { "${it.key}=${it.value}" }.joinToString()

    private fun getEventNameById(@FrameworkLogEvents.LogEventId markerId: Int): String =
            when (markerId) {
                FrameworkLogEvents.EVENT_LAYOUT_CALCULATE -> "LAYOUT_CALCULATE"
                FrameworkLogEvents.EVENT_MOUNT -> "MOUNT"
                FrameworkLogEvents.EVENT_PRE_ALLOCATE_MOUNT_CONTENT -> "PRE_ALLOCATE_MOUNT_CONTENT"
                FrameworkLogEvents.EVENT_SECTIONS_CREATE_NEW_TREE -> "SECTIONS_CREATE_NEW_TREE"
                FrameworkLogEvents.EVENT_SECTIONS_DATA_DIFF_CALCULATE_DIFF -> "SECTIONS_DATA_DIFF_CALCULATE_DIFF"
                FrameworkLogEvents.EVENT_SECTIONS_GENERATE_CHANGESET -> "SECTIONS_GENERATE_CHANGESET"
                FrameworkLogEvents.EVENT_SECTIONS_ON_CREATE_CHILDREN -> "SECTIONS_ON_CREATE_CHILDREN"
                FrameworkLogEvents.EVENT_SECTIONS_SET_ROOT -> "SECTIONS_SET_ROOT"
                FrameworkLogEvents.EVENT_CALCULATE_LAYOUT_STATE -> "CALCULATE_LAYOUT_STATE"
                else -> "UNKNOWN"
            }

    private fun Float.withDigits(digits: Int) = java.lang.String.format("%.${digits}f", this)
}
