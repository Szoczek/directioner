package pl.inz.directioner.db

import android.content.Context
import io.objectbox.BoxStore
import pl.inz.directioner.db.models.MyObjectBox

class ObjectBox {
    private lateinit var boxStore: BoxStore

    fun init(context: Context): BoxStore {
        boxStore = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()

        return boxStore
    }
}
