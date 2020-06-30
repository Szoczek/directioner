package pl.inz.directioner.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.objectbox.BoxStore
import io.reactivex.rxjava3.disposables.CompositeDisposable
import pl.inz.directioner.App
import pl.inz.directioner.R
import pl.inz.directioner.components.interfaces.SwipeListener
import pl.inz.directioner.components.listeners.OnSwipeListener
import pl.inz.directioner.utils.RQ_ACCESS_FINE_LOCATION_PERMISSION
import java.util.*
import kotlin.system.exitProcess

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), SwipeListener {

    protected lateinit var txtToSpeech: TextToSpeech
    protected lateinit var db: BoxStore
    protected lateinit var app: App
    protected lateinit var locationClient: FusedLocationProviderClient
    val subscriptions = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTextToSpeech(this, null)
        app = this.application as App
        db = app.db()
        initLocationService()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.dispose()
        this.txtToSpeech.shutdown()
    }

    private fun initLocationService() {
        if (checkGpsPermission())
            openLocationService()
        else {
            requestLocationPermissions()
            makeVoiceToast(R.string.location_permission_msg, object : UtteranceProgressListener() {
                override fun onError(utteranceId: String?) {
                }

                override fun onStart(utteranceId: String?) {
                }

                override fun onDone(utteranceId: String?) {
                    requestLocationPermissions()
                }
            })
        }
    }

    protected fun initTextToSpeech(context: Context, listener: TextToSpeech.OnInitListener?) {
        if (listener == null)
            this.txtToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener {
                if (it == TextToSpeech.SUCCESS) {
                    val result = txtToSpeech.setLanguage(Locale("pl_PL"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        txtToSpeech.language = Locale.ENGLISH
                        makeVoiceToast(R.string.language_not_supported_en, null)
                    }
                } else
                    Log.e("error", "Initialization Failed!")
            })
        else
            this.txtToSpeech = TextToSpeech(context, listener)
    }

    fun makeVoiceToast(id: Int, listener: UtteranceProgressListener?) {
        val text = resources.getString(id)

        if (text.isBlank()) {
            val txt = resources.getString(R.string.content_not_available)
            txtToSpeech.speak(
                txt,
                TextToSpeech.QUEUE_FLUSH,
                null,
                UUID.randomUUID().toString()
            )
            return
        }
        txtToSpeech.setOnUtteranceProgressListener(listener)
        txtToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
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
                    makeVoiceToast(R.string.location_permission_denied_msg, object :
                        UtteranceProgressListener() {
                        override fun onError(utteranceId: String?) {
                        }

                        override fun onStart(utteranceId: String?) {
                        }

                        override fun onDone(utteranceId: String?) {
                            finish()
                            exitProcess(0)
                        }
                    })
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

