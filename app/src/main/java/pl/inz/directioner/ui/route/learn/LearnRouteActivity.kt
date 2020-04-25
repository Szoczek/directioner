package pl.inz.directioner.ui.route.learn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.activity_learn_route.*
import pl.inz.directioner.R
import pl.inz.directioner.components.BaseActivity
import pl.inz.directioner.components.interfaces.NewRouteInstance
import pl.inz.directioner.components.listeners.SignificantMotionListener
import pl.inz.directioner.db.models.MyLocation
import pl.inz.directioner.db.models.Route
import java.io.Serializable
import java.util.*

class LearnRouteActivity : BaseActivity(), OnMapReadyCallback, TextToSpeech.OnInitListener {

    private val dataIntent: DataIntent by lazy {
        intent.getSerializableExtra(
            ARG_LEARN_ROUTE_DATA_INTENT
        ) as DataIntent
    }
    private lateinit var locationCallback: LocationCallback
    private lateinit var mMap: GoogleMap
    private lateinit var mRoute: Route
    private var locationUpdatedStarted = false
    private lateinit var mSignificantMotionListener: SignificantMotionListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRoute = createRoute(dataIntent.no, dataIntent.name)
        setContentView(R.layout.activity_learn_route)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.learnRouteMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        initOnSwipeListener(this, this.learnRouteListener)
        initTextToSpeech(this, this)
        mSignificantMotionListener = SignificantMotionListener(this, this::onSignificantMotion)
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            val result = txtToSpeech.setLanguage(Locale("pl_PL"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                txtToSpeech.language = Locale.ENGLISH
                makeVoiceToast(R.string.language_not_supported_en, null)
            } else
                introduction()
        } else
            Log.e("error", "Initialization Failed!")
    }

    private fun introduction() {
        makeVoiceToast(
            R.string.map_learn_activity_introduction,
            object : UtteranceProgressListener() {
                override fun onDone(utteranceId: String?) {
                    makeVoiceToast(R.string.map_learn_activity_introduction_msg, null)
                }

                override fun onError(utteranceId: String?) {
                }

                override fun onStart(utteranceId: String?) {
                }
            })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
    }

    override fun doubleClick(): Boolean {
        if (locationUpdatedStarted) {
            stopLocationUpdates()
            makeVoiceToast(R.string.route_learning_stopped, null)
        } else {
            startLocationUpdates()
            makeVoiceToast(R.string.route_learning_started, null)
        }
        return true
    }

    override fun longClick() {
        makeVoiceToast(R.string.activity_closing, object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                if (locationUpdatedStarted)
                    stopLocationUpdates()

                finish()
            }

            override fun onError(utteranceId: String?) {
            }

            override fun onStart(utteranceId: String?) {
            }
        })
    }

    private fun stopLocationUpdates() {
        val box: Box<Route> = db.boxFor()
        box.put(mRoute)
        locationClient.removeLocationUpdates(locationCallback)
        locationUpdatedStarted = false
    }

    private fun removeLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
        locationUpdatedStarted = false
    }

    private fun onSignificantMotion() {
        removeLocationUpdates()
        startSingleLocationUpdate()
    }

    private fun startSingleLocationUpdate() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                updateLocation(locationResult.locations.last(), false)
                removeLocationUpdates()
                startLocationUpdates()
            }
        }

        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 1000
            smallestDisplacement = 0f
        }

        locationClient.requestLocationUpdates(request, locationCallback, null)
        locationUpdatedStarted = true
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                updateLocation(locationResult.locations.last(), true)
            }
        }

        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 20000
            fastestInterval = 10000
            smallestDisplacement = 5f
        }

        locationClient.requestLocationUpdates(request, locationCallback, null)
        locationUpdatedStarted = true
    }

    private fun updateLocation(location: Location?, checkDistance: Boolean) {
        location ?: return

        if (!locationChanged(location) && checkDistance)
            return

        val currentLocation = createLocation(location, mRoute)
        mRoute.locations.add(currentLocation)


        val newLatLng = LatLng(currentLocation.lat!!, currentLocation.lon!!)
        mMap.addMarker(createMarker(newLatLng))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 25f))
    }

    private fun createMarker(latLng: LatLng): MarkerOptions {
        val title: String = mRoute.locations.count().toString()
        return MarkerOptions()
            .position(latLng)
            .draggable(false)
            .visible(true)
            .title(title)
    }

    private fun createLocation(location: Location, route: Route): MyLocation {
        val box: Box<MyLocation> = this.db.boxFor()

        val tmp = MyLocation()
        tmp.alt = location.altitude
        tmp.lon = location.longitude
        tmp.lat = location.latitude
        tmp.date = Date()
        tmp.route.target = route

        box.put(tmp)
        return tmp
    }

    private fun locationChanged(currentLocation: Location?): Boolean {
        currentLocation ?: return false
        if (mRoute.locations.isNullOrEmpty())
            return true

        val lastLocation = mRoute.locations.last()

        val distance = FloatArray(3)
        Location.distanceBetween(
            lastLocation.lat!!,
            lastLocation.lon!!,
            currentLocation.latitude,
            currentLocation.longitude,
            distance
        )

        return distance[0] > 5f
    }

    private fun createRoute(no: Int, name: String): Route {
        val box: Box<Route> = db.boxFor()

        val route = Route()
        route.no = no
        route.name = name
        box.put(route)

        return route
    }

    companion object : NewRouteInstance {
        const val ARG_LEARN_ROUTE_DATA_INTENT = "learn-route-data-intent"

        override fun newInstance(context: Context, no: Int, name: String): Intent {
            return Intent(context, LearnRouteActivity::class.java).putExtra(
                ARG_LEARN_ROUTE_DATA_INTENT,
                DataIntent(no, name)
            )
        }
    }

    data class DataIntent(
        val no: Int,
        val name: String
    ) : Serializable
}
