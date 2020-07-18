package pl.inz.directioner.components.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import io.reactivex.Single

class LocationRepository(private val context: Context) {
    private var mFusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getLastLocation(): Single<Location> {
        return Single.create { e ->
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mFusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location ->
                        e.onSuccess(location)
                    }
            } else e.onError(Throwable("You haven't given the permissions"))
        }
    }
}