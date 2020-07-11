package com.example.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.reactivex.rxjava3.subjects.PublishSubject

class Compass(context: Context) : SensorEventListener {
    private var sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val mGravity = FloatArray(3)
    private val mGeomagnetic = FloatArray(3)
    private val mRotationMatrix = FloatArray(9)
    private val mInclinationMatrix = FloatArray(9)

    private var azimuth = 0f
    private var azimuthFix = 0f

    val azimuthChangedSubject: PublishSubject<Pair<Int, SOTW>> = PublishSubject.create()

    fun start() {
        sensorManager.registerListener(
            this, gSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        sensorManager.registerListener(
            this, mSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f
        synchronized(this) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0]
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1]
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2]
            }

            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0]
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1]
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2]
            }

            val success = SensorManager.getRotationMatrix(
                mRotationMatrix, mInclinationMatrix, mGravity,
                mGeomagnetic
            )
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(mRotationMatrix, orientation)
                azimuth =
                    Math.toDegrees(orientation[0].toDouble()).toFloat() // orientation
                azimuth = (azimuth + azimuthFix + 360) % 360

                azimuthChangedSubject.onNext(SOTWFormatter.format(azimuth))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
