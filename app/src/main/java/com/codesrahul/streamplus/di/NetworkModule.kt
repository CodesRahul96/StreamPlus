package com.codesrahul.streamplus.di
 
import com.codesrahul.streamplus.data.settings.SettingsRepository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.CookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.ConnectionSpec
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import java.security.SecureRandom
import java.security.cert.X509Certificate
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(settingsRepository: SettingsRepository): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        
        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "IPTVSmartersPlayer")
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .addInterceptor(com.codesrahul.streamplus.data.remote.DynamicUrlInterceptor())
            .addInterceptor(com.codesrahul.streamplus.data.network.CrossDomainForwardingInterceptor())
            .cookieJar(SharedCookieJar)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(false)
            .followSslRedirects(false)

        // Always enable for now to simplify troubleshooting
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        builder.connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // We use a dummy base URL since the actual Xtream Server URLs are dynamic per user playlist.
        // Retrofit requires a base URL to build. We will pass a dynamic URL using an Interceptor 
        // or by using @Url in Retrofit endpoints if needed, but for simplicity here we just use 
        // the base url from the user's stored playlist when making requests.
        // For dynamic domains in Retrofit, it's easier to use a placeholder.
        return Retrofit.Builder()
            .baseUrl("http://localhost/") // Placeholder, intercepted/overridden at runtime
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideXtreamApiService(retrofit: Retrofit): com.codesrahul.streamplus.data.remote.XtreamApiService {
        return retrofit.create(com.codesrahul.streamplus.data.remote.XtreamApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideXtreamRepository(
        apiService: com.codesrahul.streamplus.data.remote.XtreamApiService
    ): com.codesrahul.streamplus.domain.repository.XtreamRepository {
        return com.codesrahul.streamplus.data.repository.XtreamRepositoryImpl(apiService)
    }

    private object SharedCookieJar : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            synchronized(cookieStore) {
                cookieStore[url.host] = cookies
            }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return synchronized(cookieStore) {
                cookieStore[url.host] ?: listOf()
            }
        }
    }
}
