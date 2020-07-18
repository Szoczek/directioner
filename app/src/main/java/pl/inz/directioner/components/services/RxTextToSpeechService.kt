package pl.inz.directioner.components.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import io.reactivex.Observable
import java.util.*

class RxTextToSpeechService(val context: Context) {
    private var audio: RxTTSObservableOnSubscribe? = null

    fun requestTTS(activity: Activity, requestCode: Int) {
        val checkTTSIntent = Intent()
        checkTTSIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        activity.startActivityForResult(checkTTSIntent, requestCode)
    }

    fun cancelCurrent() {
        if (audio != null) {
            audio!!.dispose()
            audio = null
        }
    }

    fun speak(textToRead: String): Observable<Boolean> {
        audio =
            RxTTSObservableOnSubscribe(
                context,
                textToRead,
                Locale("pl_PL")
            )
        return Observable.create(audio)
    }
}