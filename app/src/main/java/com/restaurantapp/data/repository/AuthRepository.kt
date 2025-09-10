package com.restaurantapp.data.repository

import android.content.Context
import android.util.Log
import com.restaurantapp.data.model.*
import com.restaurantapp.data.network.ApiService
import com.restaurantapp.data.network.NetworkModule

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "AuthRepository"
    }
    
    private val authInterceptor = NetworkModule.getAuthInterceptor()
    
    // 회원가입
    suspend fun register(username: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val request = RegisterRequest(username, email, password)
            val response = apiService.register(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // JWT 토큰 저장
                authInterceptor.saveToken(authResponse.token)
                
                Log.d(TAG, "회원가입 성공: ${authResponse.user.username}")
                Result.success(authResponse)
            } else {
                val errorMessage = response.body()?.error ?: "회원가입 실패"
                Log.e(TAG, "회원가입 실패: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "회원가입 오류", e)
            Result.failure(e)
        }
    }
    
    // 로그인
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // JWT 토큰 저장
                authInterceptor.saveToken(authResponse.token)
                
                Log.d(TAG, "로그인 성공: ${authResponse.user.username}")
                Result.success(authResponse)
            } else {
                val errorMessage = response.body()?.error ?: "로그인 실패"
                Log.e(TAG, "로그인 실패: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "로그인 오류", e)
            Result.failure(e)
        }
    }
    
    // 이메일 인증
    suspend fun verifyEmail(email: String, code: String): Result<String> {
        return try {
            val request = VerifyEmailRequest(email, code)
            val response = apiService.verifyEmail(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val message = response.body()?.data ?: "이메일 인증 완료"
                Log.d(TAG, "이메일 인증 성공: $email")
                Result.success(message)
            } else {
                val errorMessage = response.body()?.error ?: "이메일 인증 실패"
                Log.e(TAG, "이메일 인증 실패: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "이메일 인증 오류", e)
            Result.failure(e)
        }
    }
    
    // 토큰 갱신
    suspend fun refreshToken(): Result<AuthResponse> {
        return try {
            val response = apiService.refreshToken()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // 새로운 JWT 토큰 저장
                authInterceptor.saveToken(authResponse.token)
                
                Log.d(TAG, "토큰 갱신 성공")
                Result.success(authResponse)
            } else {
                val errorMessage = response.body()?.error ?: "토큰 갱신 실패"
                Log.e(TAG, "토큰 갱신 실패: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "토큰 갱신 오류", e)
            Result.failure(e)
        }
    }
    
    // 로그아웃
    suspend fun logout(): Result<String> {
        return try {
            val response = apiService.logout()
            
            // 서버 응답과 관계없이 로컬 토큰 삭제
            authInterceptor.clearToken()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val message = response.body()?.data ?: "로그아웃 완료"
                Log.d(TAG, "로그아웃 성공")
                Result.success(message)
            } else {
                // 서버 오류가 있어도 로컬 토큰은 이미 삭제했으므로 성공 처리
                Log.w(TAG, "서버 로그아웃 실패했지만 로컬 토큰 삭제 완료")
                Result.success("로그아웃 완료")
            }
        } catch (e: Exception) {
            // 예외가 발생해도 로컬 토큰은 이미 삭제했으므로 성공 처리
            Log.e(TAG, "로그아웃 오류", e)
            Result.success("로그아웃 완료")
        }
    }
    
    // 사용자 프로필 조회
    suspend fun getUserProfile(): Result<User> {
        return try {
            val response = apiService.getUserProfile()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()?.data!!
                Log.d(TAG, "사용자 프로필 조회 성공: ${user.username}")
                Result.success(user)
            } else {
                val errorMessage = response.body()?.error ?: "프로필 조회 실패"
                Log.e(TAG, "프로필 조회 실패: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "프로필 조회 오류", e)
            Result.failure(e)
        }
    }
    
    // 사용자 프로필 업데이트
    suspend fun updateUserProfile(updates: Map<String, String>): Result<User> {
        return try {
            val response = apiService.updateUserProfile(updates)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()?.data!!
                Log.d(TAG, "사용자 프로필 업데이트 성공: ${user.username}")
                Result.success(user)
            } else {
                val errorMessage = response.body()?.error ?: "프로필 업데이트 실패"
                Log.e(TAG, "프로필 업데이트 실패: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "프로필 업데이트 오류", e)
            Result.failure(e)
        }
    }
    
    // 계정 삭제
    suspend fun deleteAccount(): Result<String> {
        return try {
            val response = apiService.deleteAccount()
            
            if (response.isSuccessful && response.body()?.success == true) {
                // 계정 삭제 성공 시 로컬 토큰도 삭제
                authInterceptor.clearToken()
                
                val message = response.body()?.data ?: "계정 삭제 완료"
                Log.d(TAG, "계정 삭제 성공")
                Result.success(message)
            } else {
                val errorMessage = response.body()?.error ?: "계정 삭제 실패"
                Log.e(TAG, "계정 삭제 실패: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "계정 삭제 오류", e)
            Result.failure(e)
        }
    }
    
    // 사용자 통계 조회
    suspend fun getUserStats(): Result<Map<String, Int>> {
        return try {
            val response = apiService.getUserStats()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val statsData = response.body()?.data
                
                // 서버 응답 구조에 맞게 변환
                val stats = mutableMapOf<String, Int>()
                
                // bookmarkCount와 favoriteCount만 추출
                val bookmarkCount = (statsData?.get("bookmarkCount") as? Number)?.toInt() ?: 0
                val favoriteCount = (statsData?.get("favoriteCount") as? Number)?.toInt() ?: 0
                
                stats["bookmarks"] = bookmarkCount
                stats["favorites"] = favoriteCount
                
                Log.d(TAG, "사용자 통계 조회 성공: $stats")
                Result.success(stats.toMap())
            } else {
                val errorMessage = response.body()?.error ?: "통계 조회 실패"
                Log.e(TAG, "통계 조회 실패: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "통계 조회 오류", e)
            Result.failure(e)
        }
    }
    
    // 로그인 상태 확인
    fun isLoggedIn(): Boolean {
        return authInterceptor.isLoggedIn()
    }
    
    // 저장된 토큰 확인
    fun getToken(): String? {
        return authInterceptor.getToken()
    }
    
    // 토큰 수동 삭제 (테스트용)
    fun clearToken() {
        authInterceptor.clearToken()
    }
}