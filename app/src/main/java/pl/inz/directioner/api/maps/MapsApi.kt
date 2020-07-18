package pl.inz.directioner.api.maps

import io.reactivex.Single
import pl.inz.directioner.api.models.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MapsApi {
    @GET("directions/json")
    fun getDirections(
        @Query("origin") origin: String?,
        @Query("destination") destination: String?,
        @Query("waypoints") waypoints: String?,
        @Query("mode") mode: String?,
        @Query("key") key: String
    ): Single<DirectionsResponse>
}