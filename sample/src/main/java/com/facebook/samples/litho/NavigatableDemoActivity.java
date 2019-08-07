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

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import java.util.Arrays;
import java.util.List;

public class NavigatableDemoActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    int[] indices = getIntent().getIntArrayExtra(DemoListActivity.INDICES);
    if (indices != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      setTitleFromIndices(indices);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
      case android.R.id.home:
        NavUtils.navigateUpFromSameTask(this);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public @Nullable Intent getParentActivityIntent() {
    int[] indices = getIntent().getIntArrayExtra(DemoListActivity.INDICES);
    if (indices == null) {
      return null;
    }

    final Intent parentIntent = new Intent(this, DemoListActivity.class);
    if (indices.length > 1) {
      parentIntent.putExtra(DemoListActivity.INDICES, Arrays.copyOf(indices, indices.length - 1));
    }

    return parentIntent;
  }

  private void setTitleFromIndices(int[] indices) {
    List<DemoListActivity.DemoListDataModel> dataModels = DemoListActivity.DATA_MODELS;
    for (int i = 0; i < indices.length - 1; i++) {
      dataModels = dataModels.get(indices[i]).datamodels;
    }

    final String title = dataModels.get(indices[indices.length - 1]).name;
    setTitle(title);
  }
}
