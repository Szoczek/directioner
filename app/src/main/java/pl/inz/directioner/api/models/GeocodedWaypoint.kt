package pl.inz.directioner.api.models

import com.google.gson.annotations.SerializedName

data class GeocodedWaypoint(
    @SerializedName("types")
    val types: List<String>?,
    @SerializedName("geocoder_status")
    val geocoderStatus: String = "",
    @SerializedName("place_id")
    val placeId: String = ""
)