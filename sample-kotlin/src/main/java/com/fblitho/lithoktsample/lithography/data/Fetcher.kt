package com.fblitho.lithoktsample.lithography.data

/**
 * Created by pasqualea on 10/18/17.
 */
interface Fetcher {
    fun fetchMoreData(lastFetchedDecade: Int)
}