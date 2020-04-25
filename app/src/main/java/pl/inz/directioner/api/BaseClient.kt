package pl.inz.directioner.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

abstract class BaseClient<T>(
    baseUrl: String,
    service: Class<T>
) {
    abstract val apiKey: String
    val client: T = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(service)
}