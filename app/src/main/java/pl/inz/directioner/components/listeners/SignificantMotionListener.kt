package pl.inz.directioner.components.listeners

import android.content.Context
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener

class SignificantMotionListener(val context: Context, val foo : () -> Unit) : TriggerEventListener() {

    override fun onTrigger(p0: TriggerEvent?) {
        foo()
    }
}