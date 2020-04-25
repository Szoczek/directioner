package pl.inz.directioner.api.models

import com.google.gson.annotations.SerializedName

data class OverviewPolyline(
    @SerializedName("points") val points: String
)