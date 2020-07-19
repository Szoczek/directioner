package pl.inz.directioner.ui.route

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import com.example.compass.Compass
import com.example.compass.SOTW
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.patloew.rxlocation.RxLocation
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_learn_route.*
import kotlinx.android.synthetic.main.activity_route.*
import kotlinx.android.synthetic.main.activity_route.showMap
import pl.inz.directioner.R
import pl.inz.directioner.api.maps.MapsClient
import pl.inz.directioner.api.models.DirectionsResponse
import pl.inz.directioner.api.models.Leg
import pl.inz.directioner.api.models.Step
import pl.inz.directioner.components.BaseActivity
import pl.inz.directioner.components.controllers.GoogleMapController
import pl.inz.directioner.components.interfaces.RouteInstance
import pl.inz.directioner.db.models.MyLocation
import pl.inz.directioner.db.models.Route
import pl.inz.directioner.utils.toObservable
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class RouteActivity : BaseActivity(), OnMapReadyCallback {
    private val dataIntent: DataIntent by lazy {
        intent.getSerializableExtra(
            ARG_ROUTE_DATA_INTENT
        ) as DataIntent
    }
    private lateinit var mMap: GoogleMap
    private lateinit var mapApiClient: MapsClient
    private lateinit var mMapUiController: GoogleMapController
    private lateinit var mCompass: Compass
    private lateinit var rxLocation: RxLocation
    private lateinit var mCurrentLeg: Leg
    private lateinit var mCurrentStep: Step

    private var routeStarted = false
    private var isNextStepProcessing = false

    //Mockup data
    private var startPoint = LatLng(49.885407, 18.894316)
    private var mWaypoints = listOf(
        LatLng(49.8767943, 18.9212501),
        LatLng(49.8696198, 18.9378713),
        LatLng(49.8684286, 18.9367457),
        LatLng(49.8621465, 18.9446868)
    )
    private var endPoint = LatLng(49.8546154, 18.9441142)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_route)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.routeMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mapApiClient = MapsClient(this)
        initOnSwipeListener(this, this.routeListener)
        initTextToSpeech(this)
        mCompass = Compass(this)
        rxLocation = RxLocation(this)

        initUI()
        introduction()
        setSubscriptions()
    }

    private var currentAzimuth = 0 to SOTW.NORTH
    private fun setSubscriptions() {
        mCompass.azimuthChangedSubject.subscribe {
            currentAzimuth = it
        }.addTo(subscriptions)
        mCompass.start()
    }

    private fun initUI() {
        this.showMap.setOnClickListener {
            if (this.routeListener.isVisible) {
                this.routeListener.visibility = View.GONE
                this.routeListener.setText(R.string.hide_map)
            } else {
                this.routeListener.visibility = View.VISIBLE
                this.routeListener.setText(R.string.show_map)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun start() {
        rxLocation.location().lastLocation()
            .subscribeOn(Schedulers.io())
            .flatMap { currentLocation ->
                val route = getRoute(dataIntent.routeId)
                val closestLocation = getClosestLocation(currentLocation, route.locations)
                val currentDestinationIndex = route.locations.indexOf(closestLocation)

                var waypoints =
                    if (route.locations.size == 1)
                        route.locations
                    else
                        route.locations
                            .subList(currentDestinationIndex, route.locations.lastIndex)

                val originCoordinates = LatLng(currentLocation.latitude, currentLocation.longitude)

                val destinationLocation = waypoints.last()
                val destinationCoordinates = LatLng(
                    destinationLocation.lat!!,
                    destinationLocation.lon!!
                )

                waypoints = if (waypoints.size > 1) waypoints.subList(
                    0,
                    waypoints.lastIndex - 1
                )
                else mutableListOf()

                mapApiClient.getDirectionsFromLatLng(
                    startPoint,
//                    originCoordinates,
                    endPoint,
//                    destinationCoordinates,
                    mWaypoints
//                    waypoints.map { LatLng(it.lat!!, it.lon!!) }
                ).toMaybe()

            }.observeOn(AndroidSchedulers.mainThread())
            .subscribe { response: DirectionsResponse ->
                val currentRoute = response.routes.first()
                mCurrentLeg = currentRoute.legs.first()
                mCurrentStep = mCurrentLeg.steps.first()

                mMapUiController.clearMarkersAndRoute()
                mMapUiController.setMarkersAndRoute(currentRoute)
                startRoute()
            }.addTo(subscriptions)
    }

    private fun introduction() {
        makeVoiceToast(R.string.map_activity_introduction).doOnComplete {
            makeVoiceToast(R.string.map_activity_introduction_msg)
                .subscribe()
        }.subscribe()
            .addTo(subscriptions)
    }

    override fun doubleClick(): Boolean {
        if (routeStarted) {
            makeVoiceToast(R.string.route_stopped).subscribe()
                .addTo(subscriptions)
        } else {
            makeVoiceToast(R.string.route_started).doOnComplete {
                start()
            }.subscribe()
                .addTo(subscriptions)
        }
        return true
    }

    private var bypassFilters = false
    override fun leftSwipe() {
        bypassFilters = true
    }

    override fun rightSwipe() {
        finishRoute()
    }

    override fun longClick() {
        makeVoiceToast(R.string.activity_closing).doOnComplete {
            finish()
        }.subscribe()
            .addTo(subscriptions)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMapUiController = GoogleMapController(this, googleMap)
    }

    @SuppressLint("MissingPermission")
    private fun startRoute() {
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
        }
        isNextStepProcessing = false
        rxLocation.location().updates(request)
            .subscribeOn(Schedulers.io())
            .debounce (30, TimeUnit.SECONDS)
            .filter {
                !isNextStepProcessing
            }
            .switchMap {
                onCurrentLocationUpdated(it)
                it.toObservable()
            }
            .filter {
                bypassFilters ||
                        isLocationCloseToGoal(it)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                isNextStepProcessing = true
                val nextStep = getNextStepOrNull()
                if (nextStep == null)
                    finishRoute()
                else
                    startStep(nextStep)
            }.addTo(subscriptions)

        routeStarted = true
    }

    private fun getNextStepOrNull(): Step? {
        val nextIndex = mCurrentLeg.steps.indexOf(mCurrentStep) + 1
        val isCurrentStepLast = mCurrentLeg.steps.size == (nextIndex)

        return if (isCurrentStepLast)
            null
        else
            mCurrentLeg.steps[(nextIndex)]
    }


    private fun startStep(step: Step) {
        getDirectionsForStep(step).switchMap {
            makeVoiceToast(it)
        }.subscribe {
            bypassFilters = false

            isNextStepProcessing = false
            mCurrentStep = step
        }.addTo(subscriptions)
    }

    private fun finishRoute() {
        routeStarted = false

        makeVoiceToast(R.string.you_have_arrived).doOnComplete {
            makeVoiceToast(R.string.activity_closing).doOnComplete {
                finish()
            }.subscribe()
        }.subscribe()
            .addTo(subscriptions)
    }

    private fun getDirectionsForStep(step: Step): Observable<Int> {
        val currentManeuverRaw = step.maneuver

        return when (currentManeuverRaw?.toLowerCase(Locale.ROOT)) {
            "turn-left" -> {
                R.string.turn_left.toObservable()
            }

            "turn-right" -> {
                R.string.turn_right.toObservable()
            }

            else -> {
                R.string.go_straight.toObservable()
            }
        }
    }

    private fun onCurrentLocationUpdated(currentLocation: Location) {
        val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        mMapUiController.replaceUserMarker(currentLatLng)
    }

    private fun isLocationCloseToGoal(location: Location): Boolean {
        val distance = FloatArray(3)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            mCurrentStep.endLocation.lat,
            mCurrentStep.endLocation.lng,
            distance
        )

        return distance[0].roundToInt() <= 1
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