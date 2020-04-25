package pl.inz.directioner.ui.route

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.activity_route.*
import pl.inz.directioner.R
import pl.inz.directioner.components.BaseActivity
import pl.inz.directioner.components.interfaces.RouteInstance
import pl.inz.directioner.db.models.MyLocation
import pl.inz.directioner.db.models.Route
import java.io.Serializable
import java.util.*


class RouteActivity : BaseActivity(), OnMapReadyCallback, TextToSpeech.OnInitListener {
    private val dataIntent: DataIntent by lazy {
        intent.getSerializableExtra(
            ARG_ROUTE_DATA_INTENT
        ) as DataIntent
    }
    private lateinit var locationCallback: LocationCallback
    private lateinit var mMap: GoogleMap
    private lateinit var mRoute: Route
    private var routeStarted = false
    private var revertMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRoute = getRoute(dataIntent.routeId)
        setContentView(R.layout.activity_route)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.routeMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        initOnSwipeListener(this, this.routeListener)
        initTextToSpeech(this, this)
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
        makeVoiceToast(R.string.map_activity_introduction, object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                makeVoiceToast(R.string.map_activity_introduction_msg, null)
            }

            override fun onError(utteranceId: String?) {
            }

            override fun onStart(utteranceId: String?) {
            }
        })
    }

    override fun doubleClick(): Boolean {
        if (routeStarted) {
            makeVoiceToast(R.string.route_stopped, null)
        } else {
            makeVoiceToast(R.string.route_started, null)
            startRoute()
        }
        return true
    }

    override fun longClick() {
        makeVoiceToast(R.string.activity_closing, object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                finish()
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

    private fun startRoute() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                locationClient.removeLocationUpdates(locationCallback)
                onCurrentLocationUpdated(locationResult.locations.last())
            }
        }

        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 2500
        }

        routeStarted = true
        locationClient.requestLocationUpdates(request, locationCallback, null)
    }

    private fun onCurrentLocationUpdated(currentLocation: Location) {
        val allLocations = mRoute.locations.toList()
        val locations = getLocationsFromClosest(
            getClosestLocation(currentLocation, allLocations),
            allLocations,
            revertMode
        )
        val url = getDirectionsIntentUrl(locations, revertMode)
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

    private fun getLocationsFromClosest(
        closestLocation: MyLocation,
        allLocations: List<MyLocation>,
        revertMode: Boolean
    ): List<MyLocation> {

        val sortedLocations: List<MyLocation> = if (revertMode)
            allLocations.sortedByDescending { it.id }
        else
            allLocations.sortedBy { it.id }

        var res = sortedLocations.subList(
            sortedLocations.indexOf(closestLocation),
            sortedLocations.indexOf(sortedLocations.last())
        )

        if (res.isEmpty())
            res = listOf(sortedLocations.last())

        return res
    }

    private fun getClosestLocation(
        currentLocation: Location,
        locations: List<MyLocation>
    ): MyLocation {
        var closestLocation = locations[0]
        var smallestDistance = Float.MAX_VALUE
        locations.forEach {
            val distance = FloatArray(3)
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                it.lat!!,
                it.lon!!,
                distance
            )

            if (smallestDistance < distance[0]) {
                smallestDistance = distance[0]
                closestLocation = it
            }
        }

        return closestLocation
    }

    private fun getDirectionsIntentUrl(
        locations: List<MyLocation>,
        revertMode: Boolean
    ): String {
        val mode = "m=w"
        val avoid = "avoid=thf"
        var waypoints = ""

        val points: List<MyLocation> = if (revertMode)
            locations.sortedByDescending { it.id }
        else
            locations.sortedBy { it.id }


        points.forEach {
            waypoints += "/${it.lat},${it.lon}"
        }
        return "https://www.google.com/maps/dir$waypoints&$mode&$avoid"
    }

    private fun getRoute(id: Long): Route {
        val box: Box<Route> = db.boxFor()
        return box.get(id)
    }

    companion object : RouteInstance {
        const val ARG_ROUTE_DATA_INTENT = "route-data-intent"

        override fun newInstance(context: Context, route: Route): Intent {
            return Intent(context, RouteActivity::class.java).putExtra(
                ARG_ROUTE_DATA_INTENT,
                DataIntent(route.id)
            )
        }
    }

    data class DataIntent(
        val routeId: Long
    ) : Serializable
}