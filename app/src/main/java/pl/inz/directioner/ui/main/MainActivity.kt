package pl.inz.directioner.ui.main

import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.equal
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import pl.inz.directioner.R
import pl.inz.directioner.components.BaseActivity
import pl.inz.directioner.db.models.MyLocation
import pl.inz.directioner.db.models.Route
import pl.inz.directioner.db.models.Route_
import pl.inz.directioner.ui.route.RouteActivity
import pl.inz.directioner.ui.route.learn.LearnRouteActivity
import pl.inz.directioner.utils.RQ_LEARN_ROUTE_ACTIVITY
import pl.inz.directioner.utils.RQ_ROUTE_ACTIVITY
import java.util.*

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        initOnSwipeListener(this, this.listener)
        initTextToSpeech(this)
    }

    override fun doubleClick(): Boolean {
        Log.e("Tap", "Double tap")
        makeVoiceToast(R.string.main_activity_introduction).doOnComplete {
            makeVoiceToast(R.string.main_activity_introduction_msg)
                .subscribe()
        }.subscribe()
            .addTo(subscriptions)

        return true
    }

    override fun onStart() {
        super.onStart()
        doubleClick()
    }

    override fun downSwipe() {
        Log.e("Swipe", "Down swipe")
        makeVoiceToast(R.string.one).subscribe()
            .addTo(subscriptions)

        val route = getRoute()
        if (route == null)
            startRouteActivity(null, 1, "Jeden")
        else
            startRouteActivity(route, null, null)
    }

    override fun upSwipe() {
        Log.e("Swipe", "Up swipe")
        makeVoiceToast(R.string.two).subscribe()
            .addTo(subscriptions)

        val route = getRouteByNo(2)
        if (route == null)
            startRouteActivity(null, 2, "Dwa")
        else
            startRouteActivity(route, null, null)
    }

    override fun rightSwipe() {
        Log.e("Swipe", "Right swipe")
        makeVoiceToast(R.string.three).subscribe()
            .addTo(subscriptions)

        val route = getRouteByNo(3)
        if (route == null)
            startRouteActivity(null, 3, "Trzy")
        else
            startRouteActivity(route, null, null)
    }

    override fun leftSwipe() {
        Log.e("Swipe", "Left swipe")
        makeVoiceToast(R.string.four).subscribe()
            .addTo(subscriptions)

        val route = getRouteByNo(4)
        if (route == null)
            startRouteActivity(null, 4, "Cztery")
        else
            startRouteActivity(route, null, null)
    }

    override fun longClick() {
        Log.e("Click", "Long click")
    }

    private fun startRouteActivity(route: Route?, no: Int?, name: String?) {
        if (route == null)
            startActivityForResult(
                LearnRouteActivity.newInstance(this, no!!, name!!),
                RQ_LEARN_ROUTE_ACTIVITY
            )
        else
            startActivityForResult(
                RouteActivity.newInstance(this, route, false),
                RQ_ROUTE_ACTIVITY
            )
    }

    private fun getRoute(): Route {
        val box: Box<Route> = db.boxFor()

        val fRoute = Route()
        fRoute.no = 1
        fRoute.name = "Jeden"

        //Mockup data
        var mWaypoints = listOf(
            LatLng(37.422196, -122.091758),
            LatLng(37.415204, -122.092855),
            LatLng(37.411724, -122.093737),
            LatLng(37.411953, -122.097885)
        )
        mWaypoints.forEach {
            var tmp = MyLocation(
                alt = 0.0,
                lat = it.latitude,
                lon = it.longitude,
                date = Date()
            )

            tmp.route.target = fRoute
        }

        box.put(fRoute)

        return fRoute
    }

    private fun getRouteByNo(no: Int): Route? {
        val box: Box<Route> = this.db.boxFor()

        val route = box.query()
            .equal(Route_.no, no)
            .build()
            .findFirst()

        route ?: return null
        if (route.locations.isNullOrEmpty()) {
            box.remove(route)
            return null
        }
        return route
    }
}
