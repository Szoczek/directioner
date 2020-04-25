package pl.inz.directioner.components.listeners

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

abstract class OnSwipeListener(private val context: Context) : View.OnTouchListener {

    companion object {
        const val SWIPE_THRESHOLD = 50
        const val SWIPE_VELOCITY_THRESHOLD = 15
    }

    private val detector = GestureDetector(context, GestureListener())

    override fun onTouch(view: View, event: MotionEvent) = detector.onTouchEvent(event)

    abstract fun onRightSwipe()
    abstract fun onLeftSwipe()
    abstract fun onDownSwipe()
    abstract fun onUpSwipe()
    abstract fun onLongClick()
    abstract fun onDoubleTap(): Boolean

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent) = true
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            return onDoubleTap()
        }

        override fun onLongPress(e: MotionEvent?) {
            onLongClick()
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float)
                : Boolean {

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (abs(diffX) < SWIPE_THRESHOLD && abs(diffY) < SWIPE_THRESHOLD)
                return false

            if (abs(velocityX) < SWIPE_VELOCITY_THRESHOLD && abs(velocityY) < SWIPE_VELOCITY_THRESHOLD)
                return false

            if (abs(diffX) > abs(diffY))
                if (diffX > 0)
                    onRightSwipe()
                else
                    onLeftSwipe()
            else
                if (diffY < 0)
                    onUpSwipe()
                else
                    onDownSwipe()
            return true
        }
    }
}