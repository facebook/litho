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

package com.facebook.samples.litho;

import android.app.Application;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.flipper.android.AndroidFlipperClient;
import com.facebook.flipper.android.utils.FlipperUtils;
import com.facebook.flipper.core.FlipperClient;
import com.facebook.flipper.plugins.inspector.DescriptorMapping;
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin;
import com.facebook.flipper.plugins.litho.LithoFlipperDescriptors;
import com.facebook.soloader.SoLoader;

public class LithoSampleApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    Fresco.initialize(this);
    SoLoader.init(this, false);

    if (FlipperUtils.shouldEnableFlipper(this)) {
      final FlipperClient client = AndroidFlipperClient.getInstance(this);
      final DescriptorMapping descriptorMapping = DescriptorMapping.withDefaults();
      LithoFlipperDescriptors.add(descriptorMapping);
      client.addPlugin(new InspectorFlipperPlugin(this, descriptorMapping));
      client.start();
    }
  }
}
