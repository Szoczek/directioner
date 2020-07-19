package pl.inz.directioner.ui.route.learn

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.example.compass.Compass
import com.example.compass.SOTW
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.patloew.rxlocation.RxLocation
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_learn_route.*
import pl.inz.directioner.R
import pl.inz.directioner.components.BaseActivity
import pl.inz.directioner.components.interfaces.NewRouteInstance
import pl.inz.directioner.db.models.MyLocation
import pl.inz.directioner.db.models.Route
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class LearnRouteActivity : BaseActivity(), OnMapReadyCallback {
    private val dataIntent: DataIntent by lazy {
        intent.getSerializableExtra(
            ARG_LEARN_ROUTE_DATA_INTENT
        ) as DataIntent
    }
    private lateinit var mMap: GoogleMap
    private lateinit var mRoute: Route
    private var locationUpdatedStarted = false
    private lateinit var rxLocation: RxLocation
    private lateinit var mCompass: Compass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRoute = createRoute(dataIntent.no, dataIntent.name)
        setContentView(R.layout.activity_learn_route)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.learnRouteMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        initOnSwipeListener(this, this.learnRouteListener)
        initTextToSpeech(this)
        rxLocation = RxLocation(this)
        mCompass = Compass(this)

        initUI()
        introduction()
        setSubscriptions()
    }

    private fun initUI() {
        this.showMapLearn.setOnClickListener {
            if (this.learnRouteListener.isVisible) {
                this.learnRouteListener.visibility = View.GONE
                this.showMapLearn.setText(R.string.hide_map)
            } else {
                this.learnRouteListener.visibility = View.VISIBLE
                this.showMapLearn.setText(R.string.show_map)
            }
        }
    }

    private fun introduction() {
        makeVoiceToast(
            R.string.map_learn_activity_introduction
        ).doOnComplete {
            makeVoiceToast(R.string.map_learn_activity_introduction_msg).subscribe()
        }.subscribe()
            .addTo(subscriptions)
    }

    private var currentAzimuth: Pair<Int, SOTW>? = null
    private fun setSubscriptions() {
        mCompass.azimuthChangedSubject
            .debounce(5000, TimeUnit.MILLISECONDS)
            .subscribe {
                when {
                    currentAzimuth == null -> {
                        currentAzimuth = it
                    }
                    currentAzimuth!!.second != it.second -> {
                        onSignificantMotion()
                        currentAzimuth = it
                    }
                }
            }.addTo(subscriptions)
        mCompass.start()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
    }

    override fun doubleClick(): Boolean {
        if (locationUpdatedStarted) {
            makeVoiceToast(R.string.route_learning_stopped).doOnComplete {
                stopLocationUpdates()
            }.subscribe()
                .addTo(subscriptions)
        } else {
            makeVoiceToast(R.string.route_learning_started).doOnComplete {
                startLocationUpdates()
            }.subscribe()
                .addTo(subscriptions)
        }
        return true
    }

    override fun longClick() {
        makeVoiceToast(R.string.activity_closing).doOnComplete {
            if (locationUpdatedStarted)
                stopLocationUpdates()

            finish()
        }.subscribe()
            .addTo(subscriptions)
    }

    private fun stopLocationUpdates() {
        val box: Box<Route> = db.boxFor()
        box.put(mRoute)
        locationUpdatedStarted = false
    }

    @SuppressLint("MissingPermission")
    private fun onSignificantMotion() {
        rxLocation.location().lastLocation()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe {
                updateLocation(it, false)
            }.addTo(subscriptions)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
        }

        rxLocation.location().updates(request)
            .subscribeOn(Schedulers.io())
            .debounce(30, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                updateLocation(it, true)
            }.addTo(subscriptions)

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

        return distance[0] > 10f
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
