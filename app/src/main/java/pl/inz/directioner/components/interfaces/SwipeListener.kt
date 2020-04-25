package pl.inz.directioner.components.interfaces

import android.content.Context
import android.view.View

interface SwipeListener {

    fun initOnSwipeListener(context: Context, view: View)
    fun downSwipe()
    fun upSwipe()
    fun leftSwipe()
    fun rightSwipe()
    fun longClick()
    fun doubleClick(): Boolean
}