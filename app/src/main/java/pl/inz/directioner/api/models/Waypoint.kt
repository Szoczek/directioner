package pl.inz.directioner.api.models

import com.google.gson.annotations.SerializedName

data class Waypoint(
    @SerializedName("location") val location: Location,
    @SerializedName("step_index") val stepIndex: Int,
    @SerializedName("step_interpolation") val stepInterpolation: Double
)