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

package com.facebook.samples.litho.bordereffects;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

public class BorderEffectsActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstance) {
    super.onCreate(savedInstance);
    final ComponentContext componentContext = new ComponentContext(this);
    final RecyclerBinder binder = createRecyclerBinder(componentContext);
    setContentView(
        LithoView.create(this, Recycler.create(componentContext).binder(binder).build()));
  }

  private RecyclerBinder createRecyclerBinder(ComponentContext c) {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(new LinearLayoutInfo(c, OrientationHelper.VERTICAL, false))
            .build(c);

    int i = 0;
    recyclerBinder.insertItemAt(i++, AllBorder.create(c).build());
    recyclerBinder.insertItemAt(i++, AlternateColorBorder.create(c).build());
    recyclerBinder.insertItemAt(i++, AlternateWidthBorder.create(c).build());
    recyclerBinder.insertItemAt(i++, AlternateColorWidthBorder.create(c).build());
    recyclerBinder.insertItemAt(i++, RtlColorWidthBorder.create(c).build());

    return recyclerBinder;
  }
}
