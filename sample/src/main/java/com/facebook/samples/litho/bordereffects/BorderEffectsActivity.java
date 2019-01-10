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
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;

public class BorderEffectsActivity extends NavigatableDemoActivity {
  @Override
  protected void onCreate(Bundle savedInstance) {
    super.onCreate(savedInstance);
    setContentView(
        LithoView.create(this, BorderEffectsComponent.create(new ComponentContext(this)).build()));
  }
}
