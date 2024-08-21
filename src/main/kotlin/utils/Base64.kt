package utils

import java.nio.charset.Charset
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

//// Kotlin object for Base64 encoding and decoding
//private object XBase64 {
//    // A constant string of 64 characters for the Base64 alphabet
//    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
//
//    // A function to encode a byte array to a Base64 string
//    fun encode(data: ByteArray): String {
//        // Initialize an empty string builder for the output
//        val output = StringBuilder()
//
//        // Loop through the data in chunks of 3 bytes
//        var i = 0
//        while (i < data.size) {
//            // Get the 3 bytes as an integer
//            val chunk = data[i].toInt() shl 16 or (data.getOrNull(i + 1)?.toInt() ?: 0 shl 8) or (data.getOrNull(i + 2)?.toInt() ?: 0)
//
//            // Encode the 3 bytes to 4 characters using the Base64 alphabet
//            for (j in 0..3) {
//                // Get the 6 bits for the character
//                val bits = chunk shr (18 - j * 6) and 0x3F
//
//                // Append the character to the output
//                output.append(ALPHABET[bits])
//            }
//
//            // Increment the index by 3
//            i += 3
//        }
//
//        // Replace the trailing 'A's with '='s for padding
//        val padding = data.size % 3
//        if (padding > 0) {
//            for (k in 0 until 3 - padding) {
//                output[output.length - k - 1] = '='
//            }
//        }
//        // Return the output as a string
//        return output.toString()
//    }
//
//    // A function to decode a Base64 string to a byte array
//    fun decode(data: String): ByteArray {
//        val byteList = mutableListOf<Byte>()
//        // Loop through the data in chunks of 4 characters
//        var i = 0
//        while (i < data.length) {
//            // Get the 4 characters as an integer
//            val chunk = ALPHABET.indexOf(data[i]) shl 18 or (ALPHABET.indexOf(data[i + 1]) shl 12) or (ALPHABET.indexOf(data[i + 2]) shl 6) or ALPHABET.indexOf(data[i + 3])
//
//            // Decode the 4 characters to 3 bytes
//            for (j in 0..2) {
//                // Get the 8 bits for the byte
//                val bits = chunk shr (16 - j * 8) and 0xFF
//
//                // Append the byte to the output if it is not padding
//                if (bits != 0) {
//                    byteList.add(bits.toByte())
//                }
//            }
//
//            // Increment the index by 4
//            i += 4
//        }
//        // Return the output as a byte array
//        return byteList.toByteArray()
//    }
//}


@OptIn(ExperimentalEncodingApi::class)
fun String.fromKCode64(): String =  String(Base64.decode(this.replace("#", "/")), Charset.forName("UTF-8"))
@OptIn(ExperimentalEncodingApi::class)
fun String.toKCode64(): String = Base64.encode(this.toByteArray(Charset.forName("UTF-8"))).replace("/", "#")