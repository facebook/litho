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

package com.facebook.samples.lithoktbarebones

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.recyclerview.widget.OrientationHelper
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.LinearLayoutInfo
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder

class SampleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = ComponentContext(this)

        val recyclerBinder = RecyclerBinder.Builder()
                .layoutInfo(LinearLayoutInfo(this, OrientationHelper.VERTICAL, false))
                .build(context)

        val component = Recycler.create(context).binder(recyclerBinder).build()

        addContent(recyclerBinder, context)

        setContentView(LithoView.create(context, component))
    }

    private fun addContent(recyclerBinder: RecyclerBinder, context: ComponentContext) {
        for (i in 0 until 31) {
            recyclerBinder.insertItemAt(
                    i,
                    ComponentRenderInfo.create()
                            .component(
                                    ListItem.create(context)
                                            .color(if (i % 2 == 0) Color.WHITE else Color.LTGRAY)
                                            .title("Hello, world!")
                                            .subtitle("Litho tutorial")
                                            .build())
                            .build())
        }
    }
}
