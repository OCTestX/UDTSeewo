package utils

import java.io.File

val String.linux get() = this.replace("\\", "/")
val File.linux get() = this.absolutePath.linux