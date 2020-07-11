package pl.inz.directioner.api.models

enum class Manouvers(val value: String) {
    TURN_RIGHT("turn-right"),
    TURN_LEFT("turn-left"),
    STRAIGHT("straight"),
    CROSS_ROAD("cross-road")
}