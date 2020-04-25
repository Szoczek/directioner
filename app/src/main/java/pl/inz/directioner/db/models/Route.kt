package pl.inz.directioner.db.models

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany

@Entity
data class Route(
    @Id var id: Long = 0,
    var name: String? = null,
    var no: Int? = null
) {
    @Backlink(to = "route")
    lateinit var locations: ToMany<MyLocation>
}