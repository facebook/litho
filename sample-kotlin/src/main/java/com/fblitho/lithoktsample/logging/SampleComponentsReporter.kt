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
import com.facebook.litho.ComponentsLogger
import com.facebook.litho.ComponentsReporter

class SampleComponentsReporter : ComponentsReporter.Reporter {
    private val tag = "LITHOSAMPLE"

    override fun emitMessage(level: ComponentsReporter.LogLevel?, message: String?) {
        when (level) {
            ComponentsLogger.LogLevel.WARNING -> {
                Log.w(tag, message)
            }
            ComponentsLogger.LogLevel.ERROR -> {
                Log.e(tag, message)
            }
        }
    }

    override fun emitMessage(level: ComponentsReporter.LogLevel?, message: String?, samplingFrequency: Int) {
        emitMessage(level, message)
    }

    override fun getKeyCollisionStackTraceKeywords() = emptySet<String>()

    override fun getKeyCollisionStackTraceBlacklist() = emptySet<String>()
}
