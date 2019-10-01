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

package com.facebook.samples.lithocodelab.end;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import javax.annotation.Nullable;

/** This is a simple wrapper around {@link LithoLabEndComponentSpec}. */
public class LithoLabApproximateEndActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext c = new ComponentContext(this);

    final LithoView lithoView =
        LithoView.create(this /* context */, LithoLabEndComponent.create(c).build());

    setContentView(lithoView);
  }
}
