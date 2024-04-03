package com.bignerdranch.android.photogallery.api

import com.bignerdranch.android.photogallery.BuildConfig
import retrofit2.http.GET

private const val API_KEY = BuildConfig.API_KEY
interface FlickrApi {
    @GET(
        "services/rest/?method=flickr.interestingness.getList" +
                "&api_key=$API_KEY" +
                "&format=json" +
                "&nojsoncallback=1" +
                "&extras=url_s"
    )
    suspend fun fetchPhotos(): FlickrResponse

}