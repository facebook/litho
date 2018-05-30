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
import com.fblitho.lithoktsample.demo.DemoListActivity.Companion.INDEX

@SuppressLint("Registered")
open class NavigatableDemoActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent
        .getIntExtra(INDEX, -1)
        .takeIf { it > -1 }
        ?.let {
          supportActionBar?.setDisplayHomeAsUpEnabled(true)
          setTitleFromIndex(it)
        }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean =
      if (item.itemId == android.R.id.home) {
        NavUtils.navigateUpFromSameTask(this)
        true
      } else {
        super.onOptionsItemSelected(item)
      }

  override fun getParentActivityIntent(): Intent = Intent(this, DemoListActivity::class.java)

  private fun setTitleFromIndex(index: Int) {
    index
        .takeIf { it > -1 }
        ?.let {
          title = DataModels.DATA_MODELS[it].name
        }
  }
}
