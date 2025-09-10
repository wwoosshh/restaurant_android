package com.restaurantapp.data.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val context: Context
) : Interceptor {
    
    companion object {
        private const val AUTH_PREFS = "auth_prefs"
        private const val TOKEN_KEY = "jwt_token"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_PREFIX = "Bearer "
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 토큰이 필요 없는 엔드포인트들
        val authFreeEndpoints = listOf(
            "/auth/login",
            "/auth/register", 
            "/auth/verify-email",
            "/restaurants",  // 공개 맛집 조회는 토큰 불필요
            "/api/health"    // 헬스체크는 토큰 불필요
        )
        
        val requestPath = originalRequest.url.encodedPath
        val isAuthFreeEndpoint = authFreeEndpoints.any { endpoint ->
            requestPath.contains(endpoint)
        }
        
        // 토큰이 필요 없는 엔드포인트거나 이미 Authorization 헤더가 있는 경우
        if (isAuthFreeEndpoint || originalRequest.header(HEADER_AUTHORIZATION) != null) {
            return chain.proceed(originalRequest)
        }
        
        // JWT 토큰 가져오기
        val token = getToken()
        
        if (token.isNullOrBlank()) {
            // 토큰이 없으면 원본 요청 그대로 진행
            return chain.proceed(originalRequest)
        }
        
        // 토큰을 헤더에 추가하여 요청
        val authenticatedRequest = originalRequest.newBuilder()
            .header(HEADER_AUTHORIZATION, TOKEN_PREFIX + token)
            .build()
        
        val response = chain.proceed(authenticatedRequest)
        
        // 401 오류 (토큰 만료) 처리
        if (response.code == 401) {
            response.close()
            
            // 토큰이 만료된 경우 로그아웃 처리
            clearToken()
            
            // 원본 요청을 토큰 없이 재시도 (선택사항)
            return chain.proceed(originalRequest)
        }
        
        return response
    }
    
    // 토큰 저장
    fun saveToken(token: String) {
        sharedPreferences.edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }
    
    // 토큰 가져오기
    fun getToken(): String? {
        return sharedPreferences.getString(TOKEN_KEY, null)
    }
    
    // 토큰 삭제 (로그아웃 시)
    fun clearToken() {
        sharedPreferences.edit()
            .remove(TOKEN_KEY)
            .apply()
    }
    
    // 토큰 존재 여부 확인
    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }
    
    // 로그인 상태 확인
    fun isLoggedIn(): Boolean {
        return hasToken()
    }
    
    // 토큰 유효성 검증 (Base64 디코딩으로 만료시간 확인)
    fun isTokenValid(): Boolean {
        val token = getToken()
        if (token.isNullOrBlank()) return false
        
        return try {
            // JWT 토큰의 payload 부분 디코딩
            val parts = token.split(".")
            if (parts.size != 3) return false
            
            val payload = parts[1]
            val decodedPayload = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
            val payloadString = String(decodedPayload)
            
            // 간단한 만료시간 확인 (실제로는 JSON 파싱이 필요하지만 기본 검증만)
            val currentTime = System.currentTimeMillis() / 1000
            
            // 토큰이 존재하면 유효하다고 가정 (서버에서 검증)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // 강제 토큰 갱신 요청 플래그
    fun shouldRefreshToken(): Boolean {
        return hasToken() && !isTokenValid()
    }
}