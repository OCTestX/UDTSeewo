package app.webserver.utils

//import io.ktor.http.*
//import io.ktor.server.application.*
//import io.ktor.server.response.*

//suspend inline fun ApplicationCall.requestParmeters(vararg args: Pair<String, String?>, hasNull: (str: String) -> Unit = { throw NullPointerException(it) }) {
//    var str = ""
//    for (arg in args) {
//        if (arg.second.isNullOrEmpty()) {
//            str += "${arg.first} is null; "
//        }
//    }
//    if (str.isNotEmpty()) {
//        respondText(str, status = HttpStatusCode.BadRequest)
//        hasNull(str)
//    }
//}