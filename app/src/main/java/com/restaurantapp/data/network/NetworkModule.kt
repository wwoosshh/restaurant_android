package com.restaurantapp.data.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.restaurantapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    
    private var apiService: ApiService? = null
    private var authInterceptor: AuthInterceptor? = null
    
    // 전역 초기화 메서드 - MainActivity에서 호출 필수
    fun initialize(context: Context) {
        authInterceptor = AuthInterceptor(context.applicationContext)
    }
    
    // ApiService 인스턴스 제공
    fun getApiService(): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: createApiService().also { apiService = it }
        }
    }
    
    // AuthInterceptor 인스턴스 제공
    fun getAuthInterceptor(): AuthInterceptor {
        return authInterceptor ?: throw IllegalStateException(
            "NetworkModule이 초기화되지 않았습니다. MainActivity에서 NetworkModule.initialize(context)를 호출하세요."
        )
    }
    
    private fun createApiService(): ApiService {
        return createRetrofit().create(ApiService::class.java)
    }
    
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(createGson()))
            .build()
    }
    
    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        // AuthInterceptor 추가 (인증 토큰 자동 첨부)
        authInterceptor?.let { interceptor ->
            builder.addInterceptor(interceptor)
        }
        
        // 디버그 모드에서만 로깅 인터셉터 추가
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        return builder.build()
    }
    
    private fun createGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .setLenient()
            .create()
    }
    
    // 네트워크 연결 상태 확인 헬퍼
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as android.net.ConnectivityManager
        
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }
    
    // 인스턴스 재설정 (테스트용)
    fun reset() {
        apiService = null
        authInterceptor = null
    }
}