package com.example.estacionj6.data.remote

import android.util.Log
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val BASE_URL = "http://192.168.1.13/"
    private const val TAG = "SMART_HUB_NET"

    // Logger estándar de OkHttp
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request: Request = chain.request()

            // Log manual directo a la consola de la URL enviada
            Log.d(TAG, "--> ENVIANDO PETICIÓN A: ${request.url}")

            val newRequest = request.newBuilder()
                .header("Connection", "close")
                .build()

            val response = chain.proceed(newRequest)

            Log.d(TAG, "<-- RESPUESTA DE: ${response.request.url} | Código: ${response.code}")
            response
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val api: EspHomeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EspHomeApi::class.java)
    }

    private const val NAVIDROME_BASE_URL = "http://192.168.1.30:4533/"

    val navidromeApi: NavidromeApi by lazy {
        Retrofit.Builder()
            .baseUrl(NAVIDROME_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NavidromeApi::class.java)
    }

    private const val RADIO_BASE_URL = "https://de1.api.radio-browser.info/"

    val radioApi: RadioApi by lazy {
        Retrofit.Builder()
            .baseUrl(RADIO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioApi::class.java)
    }

    private const val PLEX_BASE_URL = "http://192.168.1.30:32400/"

    val plexApi: PlexApi by lazy {
        Retrofit.Builder()
            .baseUrl(PLEX_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlexApi::class.java)
    }
}