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

package com.facebook.samples.litho.errors;

import android.os.Bundle;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.samples.litho.NavigatableDemoActivity;
import java.util.Arrays;

public class ErrorHandlingActivity extends NavigatableDemoActivity {

  private static final ListRow[] DATA =
      new ListRow[] {
        new ListRow("First Title", "First Subtitle"),
        new ListRow("Second Title", "Second Subtitle"),
        new ListRow("Third Title", "Third Subtitle"),
        new ListRow("Fourth Title", "Fourth Subtitle"),
        new ListRow("Fifth Title", "Fifth Subtitle"),
        new ListRow("Sixth Title", "Sixth Subtitle"),
        new ListRow("Seventh Title", "Seventh Subtitle"),
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // This feature is currently experimental and not enabled by default.
    ComponentsConfiguration.enableOnErrorHandling = true;

    setContentView(
        LithoView.create(
            this,
            ErrorRootComponent.create(new ComponentContext(this))
                .dataModels(Arrays.asList(DATA))
                .build()));
  }
}
