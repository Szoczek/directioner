package pl.inz.directioner

import android.app.Application
import io.objectbox.BoxStore
import pl.inz.directioner.db.ObjectBox

class App : Application() {
    private var db: BoxStore? = null

    override fun onCreate() {
        super.onCreate()
        db = ObjectBox().init(this)
    }

    fun db(): BoxStore {
        if (db == null)
            db = ObjectBox().init(this)

        return db!!
    }
}