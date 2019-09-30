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

package com.facebook.samples.litho.animations.renderthread;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import com.facebook.samples.litho.R;

public class RenderThreadAnimationActivity extends NavigatableDemoActivity
    implements CompoundButton.OnCheckedChangeListener {
  private LithoView mLithoView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_render_thread);

    mLithoView = (LithoView) findViewById(R.id.lithoView);
    buildAndSetComponentTree(true);

    CheckBox checkRT = (CheckBox) findViewById(R.id.checkRT);
    checkRT.setChecked(true);
    checkRT.setOnCheckedChangeListener(this);
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    buildAndSetComponentTree(isChecked);
  }

  private void buildAndSetComponentTree(boolean useRT) {
    final ComponentContext context = new ComponentContext(this);
    final Component component = RTAnimationComponent.create(context).useRT(useRT).build();
    mLithoView.setComponentTree(ComponentTree.create(context, component).build());
  }

  void pauseUI(View view) {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // Ignore
    }
  }
}
