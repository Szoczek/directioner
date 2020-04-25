package pl.inz.directioner.api.maps

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import io.reactivex.rxjava3.core.Observable
import pl.inz.directioner.R
import pl.inz.directioner.api.BaseClient
import pl.inz.directioner.api.maps.utils.Modes
import pl.inz.directioner.api.models.DirectionsResponse
import java.util.*

open class MapsClient(
    private val context: Context
) : BaseClient<MapsApi>(
    "https://maps.googleapis.com/maps/api",
    MapsApi::class.java
) {
    override val apiKey: String
        get() = context.getString(R.string.google_maps_key)

    open fun getDirectionsFromLatLng(
        origin: LatLng,
        destination: LatLng,
        waypoints: Collection<LatLng> = listOf(),
        mode: Modes = Modes.WALKING
    ): Observable<DirectionsResponse> {
        val originParam = "${origin.latitude},${origin.longitude}"
        val destinationParam = "${destination.latitude},${destination.longitude}"
        val modeParam = mode.toString().toLowerCase(Locale.getDefault())

        var waypointsParam = ""
        waypoints.forEach {
            waypointsParam += "via:${it.latitude},${it.longitude}|"
        }

        return getDirections(originParam, destinationParam, waypointsParam, modeParam)
    }

    private fun getDirections(
        origin: String,
        destination: String,
        waypoints: String?,
        mode: String
    ): Observable<DirectionsResponse> {
        return client.getDirections(
            origin,
            destination,
            waypoints,
            mode,
            apiKey
        )
    }
}