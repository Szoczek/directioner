package pl.inz.directioner.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.objectbox.BoxStore
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import pl.inz.directioner.App
import pl.inz.directioner.R
import pl.inz.directioner.components.services.RxTextToSpeechService
import pl.inz.directioner.components.interfaces.SwipeListener
import pl.inz.directioner.components.listeners.OnSwipeListener
import pl.inz.directioner.utils.RQ_ACCESS_FINE_LOCATION_PERMISSION
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), SwipeListener {
    private lateinit var textToSpeech: RxTextToSpeechService
    private lateinit var app: App

    protected lateinit var db: BoxStore
    protected lateinit var locationClient: FusedLocationProviderClient
    val subscriptions = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTextToSpeech(this)
        app = this.application as App
        db = app.db()
        initLocationService()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.dispose()
    }

    private fun initLocationService() {
        if (checkGpsPermission()) {
            requestLocationPermissions()
            makeVoiceToast(R.string.location_permission_msg).doOnComplete {
                requestLocationPermissions()
            }.subscribe()
                .addTo(subscriptions)
        }
    }

    protected fun initTextToSpeech(context: Context) {
        textToSpeech = RxTextToSpeechService(context)
    }

    fun makeVoiceToast(id: Int): Observable<Boolean> {
        val text = resources.getString(id)
        return this.textToSpeech.speak(text)
            .debounce(200, TimeUnit.MILLISECONDS)
    }

    private fun openLocationService() {
        locationClient =
            LocationServices.getFusedLocationProviderClient(this)
    }

    private fun checkGpsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            RQ_ACCESS_FINE_LOCATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            RQ_ACCESS_FINE_LOCATION_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openLocationService()
                } else {
                    makeVoiceToast(R.string.location_permission_denied_msg).doOnComplete {
                        finish()
                        exitProcess(0)
                    }.subscribe()
                        .addTo(subscriptions)
                }
            }
        }
    }

    override fun initOnSwipeListener(context: Context, view: View) {
        view.setOnTouchListener(object : OnSwipeListener(context) {
            override fun onDoubleTap(): Boolean {
                return doubleClick()
            }

            override fun onDownSwipe() {
                downSwipe()
            }

            override fun onUpSwipe() {
                upSwipe()
            }

            override fun onRightSwipe() {
                rightSwipe()
            }

            override fun onLeftSwipe() {
                leftSwipe()
            }

            override fun onLongClick() {
                longClick()
            }
        })
    }

    override fun doubleClick(): Boolean {
        Log.e("Click", "DoubleClick")
        return true
    }

    override fun downSwipe() {
        Log.e("Swipe", "DownSwipe")
    }

    override fun upSwipe() {
        Log.e("Swipe", "UpSwipe")
    }

    override fun rightSwipe() {
        Log.e("Swipe", "RightSwipe")
    }

    override fun leftSwipe() {
        Log.e("Swipe", "LeftSwipe")
    }

    override fun longClick() {
        Log.e("Click", "LongClick")
    }
}

