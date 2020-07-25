package pl.inz.directioner.components.interfaces

import android.content.Context
import android.content.Intent
import pl.inz.directioner.db.models.Route

interface RouteInstance {
    fun newInstance(context: Context, route: Route, isMockup: Boolean): Intent
}