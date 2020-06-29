package pl.inz.directioner.api.models

import com.google.gson.annotations.SerializedName

data class Leg(
    @SerializedName("distance") val distance: Distance,
    @SerializedName("duration") val duration: Duration,
    @SerializedName("end_address") val endAddress: String,
    @SerializedName("end_location") val endLocation: EndLocation,
    @SerializedName("start_address") val startAddress: String,
    @SerializedName("start_location") val startLocation: StartLocation,
    @SerializedName("steps") val steps: List<Step>,
    @SerializedName("traffic_speed_entry") val trafficSpeedEntry: List<String>,
    @SerializedName("via_waypoint") val viaWaypoint: List<Waypoint>
)