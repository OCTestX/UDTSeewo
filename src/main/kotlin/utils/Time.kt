package utils

import java.time.LocalDate
import java.time.LocalTime

val currentTime: String get() {
    val date = LocalDate.now()
    val time = LocalTime.now().toString().replace(":", "-")
    return "$date - $time"
}