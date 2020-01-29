package com.revolut.dunno

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

  val retrofit: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl("https://revolut.duckdns.org")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
  }

}
