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

package com.facebook.samples.lithocodelab;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import javax.annotation.Nullable;

/**
 * <b>*************** START THE LAB HERE ***************</b>
 *
 * <p>
 *
 * <p>This is a simple "Hello, world." activity that renders using Views. The goal of the lab is to
 * build this into something that resembles LithoLabApproximateEndActivity using Litho.
 *
 * <p>
 *
 * <p>Build a header. Then leverage {@link StoryCardComponent} to render the rest of the story card.
 * Then add some statefulness and click handling to the save button in the story card.
 */
public class LithoLabActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.hello_world);
  }
}
