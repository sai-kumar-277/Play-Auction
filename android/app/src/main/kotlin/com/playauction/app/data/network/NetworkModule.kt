package com.playauction.app.data.network

import com.google.gson.Gson
import com.playauction.app.data.local.SessionDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicUrlInterceptor @Inject constructor() : Interceptor {
    @Volatile
    private var hostUrl: String? = null

    fun setBaseUrl(url: String) {
        val trimmed = url.trim().let { if (it.endsWith("/")) it else "$it/" }
        hostUrl = trimmed
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val custom = hostUrl?.toHttpUrlOrNull()
        if (custom != null) {
            val newUrl = request.url.newBuilder()
                .scheme(custom.scheme)
                .host(custom.host)
                .port(custom.port)
                .build()
            request = request.newBuilder().url(newUrl).build()
        }
        return chain.proceed(request)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val DEFAULT_BASE_URL = com.playauction.app.BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionDataStore: SessionDataStore,
        dynamicUrlInterceptor: DynamicUrlInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = sessionDataStore.getTokenBlocking()
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        val baseUrl = try {
            Class.forName("com.playauction.app.BuildConfig")
                .getField("BASE_URL").get(null) as? String
                ?: DEFAULT_BASE_URL
        } catch (e: Exception) {
            DEFAULT_BASE_URL
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
