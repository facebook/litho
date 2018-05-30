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

package com.fblitho.lithoktsample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.fblitho.lithoktsample.demo.DataModels
import com.fblitho.lithoktsample.demo.DemoListActivity
import com.fblitho.lithoktsample.demo.DemoListActivity.Companion.INDICES
import com.fblitho.lithoktsample.demo.DemoListDataModel
import java.util.Arrays

@SuppressLint("Registered")
open class NavigatableDemoActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val indices = intent.getIntArrayExtra(INDICES)

    if (indices != null) {
      supportActionBar?.setDisplayHomeAsUpEnabled(true)
      setTitleFromIndices(indices)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
    // Respond to the action bar's Up/Home button
      android.R.id.home -> {
        NavUtils.navigateUpFromSameTask(this)
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun getParentActivityIntent(): Intent? {
    val indices = intent.getIntArrayExtra(DemoListActivity.INDICES) ?: return null

    val parentIntent = Intent(this, DemoListActivity::class.java)
    if (indices.size > 1) {
      parentIntent.putExtra(DemoListActivity.INDICES, Arrays.copyOf(indices, indices.size - 1))
    }

    return parentIntent
  }

  private fun setTitleFromIndices(indices: IntArray) {
    var dataModels: List<DemoListDataModel>? = DataModels.DATA_MODELS

    for (i in 0 until indices.size - 1) {
      dataModels = dataModels?.get(indices[i])?.datamodels
    }

    val title = dataModels?.get(indices[indices.size - 1])?.name
    setTitle(title)
  }
}
