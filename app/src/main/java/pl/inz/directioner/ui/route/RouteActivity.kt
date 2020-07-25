package pl.inz.directioner.ui.route

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.WindowManager
import com.example.compass.Compass
import com.example.compass.SOTW
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_route.*
import pl.inz.directioner.R
import pl.inz.directioner.api.maps.MapsClient
import pl.inz.directioner.api.models.DirectionsResponse
import pl.inz.directioner.api.models.Leg
import pl.inz.directioner.api.models.Step
import pl.inz.directioner.components.controllers.GoogleMapController
import pl.inz.directioner.components.interfaces.RouteInstance
import pl.inz.directioner.components.services.location.RxLocationFactory
import pl.inz.directioner.components.services.location.RxLocationManager
import pl.inz.directioner.components.services.location.service.Priority
import pl.inz.directioner.components.services.location.service.RxLocationAttributes
import pl.inz.directioner.db.models.MyLocation
import pl.inz.directioner.db.models.Route
import pl.inz.directioner.ui.detection.DetectorActivity
import pl.inz.directioner.utils.toObservable
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class RouteActivity : DetectorActivity(false), OnMapReadyCallback {
    private val dataIntent: DataIntent by lazy {
        intent.getSerializableExtra(
            ARG_ROUTE_DATA_INTENT
        ) as DataIntent
    }
    private lateinit var mMap: GoogleMap
    private lateinit var mapApiClient: MapsClient
    private lateinit var mMapUiController: GoogleMapController
    private lateinit var mCompass: Compass
    private lateinit var rxLocationManager: RxLocationManager
    private lateinit var mCurrentLeg: Leg
    private lateinit var mCurrentStep: Step
    private lateinit var mapFragment: SupportMapFragment

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
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.routeMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mapApiClient = MapsClient(this)
        initOnSwipeListener(this, this.routeListener)
        initTextToSpeech(this)
        mCompass = Compass(this)

        rxLocationManager = RxLocationFactory.create(
            context = this, attributes = RxLocationAttributes(
                priority = Priority.HighAccuracy,
                requestTimeOut = TimeUnit.SECONDS.toMillis(30),
                updateInterval = TimeUnit.SECONDS.toSeconds(5),
                fastestInterval = TimeUnit.SECONDS.toSeconds(2),
                useCalledThreadToEmitValue = false
            )
        )

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

    var lastSpoken = Date()
    override fun objectTooClose() {
        val current = Date()
        val a = current.seconds - lastSpoken.seconds
        if (a < 5) return
        lastSpoken = Date()
        val txt = "Wykryto duży obiekt w pobliżu, zachowaj ostrożność"
        this.makeVoiceToast(txt)
            .subscribe()
    }

    override fun dangerAhead() {
        val current = Date()
        val a = current.seconds - lastSpoken.seconds
        if (a < 15) return
        lastSpoken = Date()
        val txt = "Wykryto zbliżający się obiekt, zachowaj ostrożność"
        this.makeVoiceToast(txt)
            .subscribe()
    }

    var isListenerVisible = true
    private fun initUI() {
        this.showMap.setOnClickListener {
            if (isListenerVisible) {
                this.routeCamera.translationZ = -888f
                this.routeListener.translationZ = -999f
                isListenerVisible = false
                this.showMap.setText(R.string.hide_map)
            } else {
                this.routeCamera.translationZ = 888f
                this.routeListener.translationZ = 999f
                isListenerVisible = true
                this.showMap.setText(R.string.show_map)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun start() {
        rxLocationManager.singleLocation()
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
//                    startPoint,
                    originCoordinates,
//                    endPoint,
                    destinationCoordinates,
//                    mWaypoints
                    waypoints.map { LatLng(it.lat!!, it.lon!!) }
                )

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
            makeVoiceToast(R.string.route_started).subscribe()
                .addTo(subscriptions)

            start()
        }
        return true
    }

    private var bypassFilters = false
    override fun leftSwipe() {
        if (dataIntent.isMockup)
            bypassFilters = true
    }

    override fun rightSwipe() {
        if (dataIntent.isMockup)
            finishRoute()
    }

    override fun longClick() {
        makeVoiceToast(R.string.activity_closing).doOnComplete {
            finish()
        }.subscribe()
            .addTo(subscriptions)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
        mMapUiController = GoogleMapController(this, googleMap)
    }

    @SuppressLint("MissingPermission")
    private fun startRoute() {
        isNextStepProcessing = false
        rxLocationManager.observeLocationChange()
            .subscribeOn(Schedulers.io())
            .debounce(10, TimeUnit.SECONDS)
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

        override fun newInstance(context: Context, route: Route, isMockup: Boolean): Intent {
            return Intent(context, RouteActivity::class.java).putExtra(
                ARG_ROUTE_DATA_INTENT,
                DataIntent(route.id, isMockup)
            )
        }
    }

    data class DataIntent(
        val routeId: Long,
        val isMockup: Boolean
    ) : Serializable
}