package pl.inz.directioner.db.models

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne
import java.util.*

@Entity
data class MyLocation(
    @Id var id: Long = 0,
    var lat: Double? = null,
    var lon: Double? = null,
    var alt: Double? = null,
    var date: Date? = null
) {
    lateinit var route: ToOne<Route>
}