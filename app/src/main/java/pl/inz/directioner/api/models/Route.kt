package pl.inz.directioner.api.models

import com.google.gson.annotations.SerializedName

data class Route(
    @SerializedName("bounds") val bounds: Bounds,
    @SerializedName("legs") val legs: List<Leg>,
    @SerializedName("overview_polyline") val overview_polyline: OverviewPolyline,
    @SerializedName("summary") val summary: String,
    @SerializedName("warnings") val warnings: List<String>,
    @SerializedName("waypoint_order") val waypoint_order: List<String>
)