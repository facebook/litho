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
import com.facebook.flipper.plugins.litho.LithoSonarDescriptors;
import com.facebook.soloader.SoLoader;
import com.facebook.flipper.android.AndroidSonarClient;
import com.facebook.flipper.android.utils.SonarUtils;
import com.facebook.flipper.core.FlipperClient;
import com.facebook.flipper.plugins.console.JavascriptEnvironment;
import com.facebook.flipper.plugins.inspector.DescriptorMapping;
import com.facebook.flipper.plugins.inspector.InspectorSonarPlugin;

public class LithoSampleApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    Fresco.initialize(this);
    SoLoader.init(this, false);

    if (SonarUtils.shouldEnableSonar(this)) {
      final FlipperClient client = AndroidSonarClient.getInstance(this);
      final DescriptorMapping descriptorMapping = DescriptorMapping.withDefaults();
      LithoSonarDescriptors.add(descriptorMapping);
      client.addPlugin(
          new InspectorSonarPlugin(this, descriptorMapping, new JavascriptEnvironment()));
      client.start();
    }
  }
}
