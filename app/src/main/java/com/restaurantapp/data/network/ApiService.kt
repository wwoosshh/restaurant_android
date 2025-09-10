package com.restaurantapp.data.network

import com.restaurantapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // 인증 관련 API
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ServerResponse<AuthResponse>>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ServerResponse<AuthResponse>>
    
    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<ServerResponse<String>>
    
    @POST("auth/refresh")
    suspend fun refreshToken(): Response<ServerResponse<AuthResponse>>
    
    @POST("auth/logout")
    suspend fun logout(): Response<ServerResponse<String>>
    
    // 맛집 검색 및 조회 API
    @GET("restaurants")
    suspend fun getAllRestaurants(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("minRating") minRating: Double? = null
    ): Response<ServerResponse<List<RestaurantDto>>>
    
    @GET("restaurants/{id}")
    suspend fun getRestaurantById(@Path("id") id: String): Response<ServerResponse<RestaurantDto>>
    
    @GET("restaurants/search")
    suspend fun searchRestaurants(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ServerResponse<List<RestaurantDto>>>
    
    @GET("restaurants/nearby")
    suspend fun getNearbyRestaurants(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radius: Double = 5.0, // km
        @Query("limit") limit: Int = 20
    ): Response<ServerResponse<List<RestaurantDto>>>
    
    @GET("restaurants/categories")
    suspend fun getCategories(): Response<ServerResponse<List<String>>>
    
    // 새로운 카테고리 시스템 API
    @GET("restaurants/main-category/{mainCategory}")
    suspend fun getRestaurantsByMainCategory(
        @Path("mainCategory") mainCategory: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ServerResponse<List<RestaurantDto>>>
    
    @GET("restaurants/sub-category")
    suspend fun getRestaurantsBySubCategory(
        @Query("mainCategory") mainCategory: String,
        @Query("subCategory") subCategory: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ServerResponse<List<RestaurantDto>>>
    
    @GET("restaurants/search-in-category")
    suspend fun searchInCategory(
        @Query("categoryName") categoryName: String,
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ServerResponse<List<RestaurantDto>>>
    
    // 사용자 북마크 관리 API (JWT 토큰 필요)
    @POST("users/bookmarks/{restaurantId}")
    suspend fun toggleBookmark(@Path("restaurantId") restaurantId: String): Response<ServerResponse<Map<String, Boolean>>>
    
    @GET("users/bookmarks")
    suspend fun getBookmarkedRestaurants(): Response<ServerResponse<List<RestaurantDto>>>
    
    @DELETE("users/bookmarks/{restaurantId}")
    suspend fun removeBookmark(@Path("restaurantId") restaurantId: String): Response<ServerResponse<String>>
    
    // 사용자 즐겨찾기 관리 API (JWT 토큰 필요)
    @POST("users/favorites/{restaurantId}")
    suspend fun toggleFavorite(@Path("restaurantId") restaurantId: String): Response<ServerResponse<Map<String, Boolean>>>
    
    @GET("users/favorites")
    suspend fun getFavoriteRestaurants(): Response<ServerResponse<List<RestaurantDto>>>
    
    @DELETE("users/favorites/{restaurantId}")
    suspend fun removeFavorite(@Path("restaurantId") restaurantId: String): Response<ServerResponse<String>>
    
    // 사용자 북마크/즐겨찾기 상태 확인
    @GET("users/status/{restaurantId}")
    suspend fun getRestaurantStatus(@Path("restaurantId") restaurantId: String): Response<ServerResponse<Map<String, Boolean>>>
    
    // 사용자 프로필 관리 API
    @GET("users/profile")
    suspend fun getUserProfile(): Response<ServerResponse<User>>
    
    @PUT("users/profile")
    suspend fun updateUserProfile(@Body user: Map<String, String>): Response<ServerResponse<User>>
    
    @DELETE("users/account")
    suspend fun deleteAccount(): Response<ServerResponse<String>>
    
    // 리뷰 관련 API (향후 확장용)
    @GET("restaurants/{id}/reviews")
    suspend fun getRestaurantReviews(
        @Path("id") restaurantId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<ServerResponse<List<Any>>> // 리뷰 모델 정의 후 변경
    
    // 통계 API
    @GET("users/stats")
    suspend fun getUserStats(): Response<ServerResponse<Map<String, Any>>>
}