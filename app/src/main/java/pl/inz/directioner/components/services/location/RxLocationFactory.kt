package pl.inz.directioner.components.services.location

import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import pl.inz.directioner.components.services.location.service.AndroidLocationService
import pl.inz.directioner.components.services.location.service.FusedLocationService
import pl.inz.directioner.components.services.location.service.RxLocationAttributes


object RxLocationFactory {
    fun create(context: Context, attributes: RxLocationAttributes): RxLocationManager {
        return RxLocationManagerImpl(
            createFusedLocationService(context),
            createAndroidLocationService(context),
            attributes
        )
    }

    private fun createAndroidLocationService(context: Context): AndroidLocationService {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return AndroidLocationService(locationManager)
    }

    private fun createFusedLocationService(context: Context): FusedLocationService {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return FusedLocationService(fusedLocationClient)
    }
}