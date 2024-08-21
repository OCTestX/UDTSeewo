package repository

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object Http {
    private val basicBuilder = OkHttpClient.Builder()
        .hostnameVerifier { _, _ -> true }
        .sslSocketFactory(createInsecureSslSocketFactory(), TrustAllCerts())
        .callTimeout(25, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
    val client = basicBuilder
        .build()
    val downloadDBClient = basicBuilder.readTimeout(5, TimeUnit.MINUTES).build()
    //每秒上传字节数(750kbps)
    private const val uploadSpeed = 750
    fun createUploadFileDataClient(size: Long): OkHttpClient =
        basicBuilder.writeTimeout(
            size / (uploadSpeed * 1000), TimeUnit.SECONDS
        ).build()
    private fun createInsecureSslSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(TrustAllCerts())
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }

    class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
}