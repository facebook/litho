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
package com.facebook.samples.lithocodelab.examples;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import javax.annotation.Nullable;

/**
 * Renders {@link ExamplesActivityComponentSpec} initially, and then handles all navigation to
 * example module Components and back button presses. This was hackily thrown together since it's
 * just for demo purposes.
 */
public class ExamplesLithoLabActivity extends AppCompatActivity {

  private LabExampleController mLabExampleController;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext c = new ComponentContext(this);

    mLabExampleController = new LabExampleController(c);
    mLabExampleController.goToMain();
  }

  @Override
  public void onBackPressed() {
    if (mLabExampleController.isMain()) {
      super.onBackPressed();
    }

    mLabExampleController.goToMain();
  }

  public class LabExampleController {
    private final ComponentContext c;
    private final Component examplesActivityComponent;

    private boolean isMain = false;

    private LabExampleController(ComponentContext c) {
      this.c = c;
      examplesActivityComponent =
          ExamplesActivityComponent.create(c).labExampleController(this).build();
    }

    private boolean isMain() {
      return isMain;
    }

    public void goToMain() {
      setContentComponent(examplesActivityComponent);
      isMain = true;
    }

    public void setContentComponent(Component component) {
      isMain = false;
      ExamplesLithoLabActivity.this.setContentView(
          LithoView.create(ExamplesLithoLabActivity.this /* context */, component));
    }
  }
}
