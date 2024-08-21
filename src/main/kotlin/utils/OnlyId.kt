package utils

import java.lang.System.nanoTime
import kotlin.random.Random

private val random = Random(nanoTime())
fun getOnlyId() = "${nanoTime()}XMC${random.nextLong()}"