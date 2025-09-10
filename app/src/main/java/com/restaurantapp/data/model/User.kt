package com.restaurantapp.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id") val id: String,
    val username: String,
    val email: String,
    val isEmailVerified: Boolean = false,
    val bookmarks: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String
)

// 인증 관련 요청/응답 모델
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

// 서버 응답 공통 래퍼
data class ServerResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null
)