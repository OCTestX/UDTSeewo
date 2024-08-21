package utils

//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logger
import java.lang.reflect.Type

// DebugFlag
const val GsonTransformDebug = false

//val gson = Gson()
//fun Any?.toGson() = gson.toJson(this)
//inline fun <reified T> String?.fromGson(): T {
//    if (GsonTransformDebug) logger.debug("formGson(sourceJson: $this)")
//    return gson.fromJson(this, T::class.java)
//}
//inline fun <reified T> String?.fromGsonOfType(type: Type): T {
//    if (GsonTransformDebug) logger.debug("fromGsonOfType(type: $type, sourceJson: $this)")
//    return gson.fromJson(this, type)
//}
////
//inline fun <reified T> String?.fromGsonOrNull(): T? = gson.fromJson(this, T::class.java)
//
//object GsonType {
//    inline fun <reified T> listOf(canNull: Boolean = false): Type {
//        return if (canNull) object : TypeToken<List<T?>?>() {}.type
//        else object : TypeToken<List<T>>() {}.type
//    }
//    inline fun <reified K, reified V> mapOf(canNull: Boolean = false): Type {
//        return if (canNull) object : TypeToken<Map<K?, V?>?>() {}.type
//        else object : TypeToken<Map<K, V>>() {}.type
//    }
//}

val json = Json
inline fun <reified T> toGson(t: T): String {
    val jsonAndException = try {
        json.encodeToString(t) to null
    } catch (e: Throwable) {
        null to e
    }
    if (GsonTransformDebug) logger.debug("toGson(source: $t, json: ${jsonAndException.first})")
    return if (jsonAndException.first == null) throw jsonAndException.second!! else jsonAndException.first!!
}
inline fun <reified T> String.fromGson(): T {
    if (GsonTransformDebug) logger.debug("formGson(sourceJson: $this)")
    return json.decodeFromString(this)
}
inline fun <reified T> String.fromGsonOfType(type: Type): T {
    if (GsonTransformDebug) logger.debug("fromGsonOfType(type: $type, sourceJson: $this)")
    return json.decodeFromString(this)
}
//
//inline fun <reified T> String?.fromGsonOrNull(): T? = gson.fromJson(this, T::class.java)