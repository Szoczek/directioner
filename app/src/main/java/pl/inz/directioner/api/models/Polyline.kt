package pl.inz.directioner.api.models

import com.google.gson.annotations.SerializedName

data class Polyline(
    @SerializedName("points") val points: String
)