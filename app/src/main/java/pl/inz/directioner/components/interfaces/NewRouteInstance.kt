package pl.inz.directioner.components.interfaces

import android.content.Context
import android.content.Intent

interface NewRouteInstance {
    fun newInstance(context: Context, no: Int, name: String): Intent
}