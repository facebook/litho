package com.fblitho.lithoktsample.lithography.data

import android.arch.lifecycle.MutableLiveData
import android.os.AsyncTask

/**
 * Created by pasqualea on 10/19/17.
 */
class DataFetcher(
        val model: MutableLiveData<Model>) : Fetcher {

    var fetching = -1

    override fun fetchMoreData(lastFetchedDecade: Int) {
        if (fetching == lastFetchedDecade + 1) {
            return
        }

        fetching = lastFetchedDecade + 1;

        model.value = Model(model.value!!.decades, true)
        object : AsyncTask<Void, Void, List<Decade>>() {
            override fun doInBackground(vararg params: Void?): List<Decade> {
                //Let's simulate a network call here.
                Thread.sleep(2000);
                return DataCreator.createPageOfData(lastFetchedDecade + 1);
            }

            override fun onPostExecute(result: List<Decade>?) {
                model.value = Model(model.value!!.decades+ result!!, false)
            }

        }.execute();
    }
}