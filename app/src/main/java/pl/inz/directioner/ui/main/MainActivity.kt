package pl.inz.directioner.ui.main

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.equal
import kotlinx.android.synthetic.main.activity_main.*
import pl.inz.directioner.R
import pl.inz.directioner.components.BaseActivity
import pl.inz.directioner.db.models.Route
import pl.inz.directioner.db.models.Route_
import pl.inz.directioner.ui.route.RouteActivity
import pl.inz.directioner.ui.route.learn.LearnRouteActivity
import pl.inz.directioner.utils.RQ_LEARN_ROUTE_ACTIVITY
import pl.inz.directioner.utils.RQ_ROUTE_ACTIVITY
import java.util.*

class MainActivity : BaseActivity(), TextToSpeech.OnInitListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        initOnSwipeListener(this, this.mainContainer)
        initTextToSpeech(this, this)
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            val result = txtToSpeech.setLanguage(Locale("pl_PL"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                txtToSpeech.language = Locale.ENGLISH
                makeVoiceToast(R.string.language_not_supported_en, null)
            } else
                doubleClick()
        } else
            Log.e("error", "Initialization Failed!")
    }

    override fun doubleClick(): Boolean {
        Log.e("Tap", "Double tap")
        makeVoiceToast(R.string.main_activity_introduction, object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                makeVoiceToast(R.string.main_activity_introduction_msg, null)
            }

            override fun onError(utteranceId: String?) {
            }

            override fun onStart(utteranceId: String?) {
            }
        })
        return true
    }

    override fun downSwipe() {
        Log.e("Swipe", "Down swipe")
        makeVoiceToast(R.string.one, null)

        val route = getRouteByNo(1)
        if (route == null)
            startRouteActivity(null, 1, "Jeden")
        else
            startRouteActivity(route, null, null)
    }

    override fun upSwipe() {
        Log.e("Swipe", "Up swipe")
        makeVoiceToast(R.string.two, null)

        val route = getRouteByNo(2)
        if (route == null)
            startRouteActivity(null, 2, "Dwa")
        else
            startRouteActivity(route, null, null)
    }

    override fun rightSwipe() {
        Log.e("Swipe", "Right swipe")
        makeVoiceToast(R.string.three, null)

        val route = getRouteByNo(3)
        if (route == null)
            startRouteActivity(null, 3, "Trzy")
        else
            startRouteActivity(route, null, null)
    }

    override fun leftSwipe() {
        Log.e("Swipe", "Left swipe")
        makeVoiceToast(R.string.four, null)

        val route = getRouteByNo(4)
        if (route == null)
            startRouteActivity(null, 4, "Cztery")
        else
            startRouteActivity(route, null, null)
    }

    override fun longClick() {
        Log.e("Click", "Long click")
        makeVoiceToast(R.string.long_click, null)
    }

    private fun startRouteActivity(route: Route?, no: Int?, name: String?) {
        if (route == null)
            startActivityForResult(
                LearnRouteActivity.newInstance(this, no!!, name!!),
                RQ_LEARN_ROUTE_ACTIVITY
            )
        else
            startActivityForResult(
                RouteActivity.newInstance(this, route),
                RQ_ROUTE_ACTIVITY
            )
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
