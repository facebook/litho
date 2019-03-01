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


package com.fblitho.lithoktsample.lithography.data

import androidx.lifecycle.MutableLiveData
import android.os.AsyncTask

class DataFetcher(val model: MutableLiveData<Model>) : Fetcher {
  private var fetching = -1

  override fun invoke(lastFetchedDecade: Int) {
    if (fetching == lastFetchedDecade + 1) {
      return
    }

    fetching = lastFetchedDecade + 1

    model.value = model.value?.let {
      Model(it.decades, true)
    }

    FetchTask(model, lastFetchedDecade).execute()
  }
}

class FetchTask(val model: MutableLiveData<Model>, private val lastFetchedDecade: Int) : AsyncTask<Void, Void, List<Decade>>() {
  override fun doInBackground(vararg params: Void?): List<Decade> {
    //Let's simulate a network call here.
    Thread.sleep(2000)
    return DataCreator.createPageOfData(lastFetchedDecade + 1)
  }

  override fun onPostExecute(result: List<Decade>?) {
    result?.let {
      val decades = model.value?.decades ?: emptyList()
      model.value = Model(decades + it, false)
    }
  }
}
