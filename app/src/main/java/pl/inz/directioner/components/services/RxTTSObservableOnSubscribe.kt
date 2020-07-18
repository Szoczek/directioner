package pl.inz.directioner.components.services

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Cancellable
import pl.inz.directioner.R
import java.util.*

class RxTTSObservableOnSubscribe(
    val context: Context,
    private val text: String,
    private val locale: Locale
) : UtteranceProgressListener(), ObservableOnSubscribe<Boolean>,
    Disposable, Cancellable, TextToSpeech.OnInitListener {

    private var disposed = false
    private var emitter: ObservableEmitter<Boolean>? = null
    private var textToSpeech: TextToSpeech? = null

    override fun subscribe(e: ObservableEmitter<Boolean>) {
        this.emitter = e
        this.textToSpeech = TextToSpeech(context, this)
    }

    override fun dispose() {
        if (textToSpeech != null) {
            textToSpeech!!.setOnUtteranceProgressListener(null)
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
            textToSpeech = null
        }

        disposed = true
    }

    override fun isDisposed(): Boolean {
        return disposed
    }

    override fun cancel() {
        dispose()
    }

    override fun onInit(status: Int) {
        val languageCode = textToSpeech!!.setLanguage(locale)
        if (languageCode != TextToSpeech.LANG_MISSING_DATA && languageCode != TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech!!.setPitch(1f)
            textToSpeech!!.setSpeechRate(1.0f)
            textToSpeech!!.setOnUtteranceProgressListener(this)
            performSpeak(text)
        } else {
            textToSpeech!!.language = Locale.ENGLISH
            val txt = context.resources.getString(R.string.language_not_supported_en)
            performSpeak(txt)
        }
    }

    override fun onStart(utteranceId: String) {
    }

    override fun onDone(utteranceId: String) {
        this.emitter!!.onNext(true)
        this.emitter!!.onComplete()
    }

    override fun onError(utteranceId: String) {
        this.emitter!!.onError(Throwable("error TTS $utteranceId"))
    }

    private fun performSpeak(text: String) {
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "")
        textToSpeech!!.speak(text, TextToSpeech.QUEUE_ADD, params, uniqueId())
    }

    private fun uniqueId(): String {
        return UUID.randomUUID().toString()
    }
}