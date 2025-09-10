package com.restaurantapp.data.repository

import android.content.Context
import android.util.Log
import com.restaurantapp.data.local.RestaurantDao
import com.restaurantapp.data.model.*
import com.restaurantapp.data.network.ApiService
import com.restaurantapp.data.network.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class RestaurantRepository(
    private val restaurantDao: RestaurantDao,
    private val apiService: ApiService,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "RestaurantRepository"
    }
    
    // 서버 우선 + 로컬 백업 방식으로 모든 맛집 조회
    fun getAllRestaurants(): Flow<List<Restaurant>> = flow {
        try {
            if (NetworkModule.isNetworkAvailable(context)) {
                // 온라인: 서버에서 최신 데이터 가져오기
                val response = apiService.getAllRestaurants()
                if (response.isSuccessful && response.body()?.success == true) {
                    val restaurantDtos = response.body()?.data ?: emptyList()
                    var restaurants = restaurantDtos.toRestaurantList()
                    
                    // 사용자 북마크/즐겨찾기 상태 동기화
                    restaurants = syncUserPreferences(restaurants)
                    
                    // 로컬 DB에 동기화
                    restaurantDao.deleteAllRestaurants()
                    restaurantDao.insertRestaurants(restaurants)
                    
                    emit(restaurants)
                    Log.d(TAG, "서버에서 ${restaurants.size}개 맛집 데이터 로드 및 동기화 완료")
                } else {
                    // 서버 오류 시 로컬 데이터 사용
                    restaurantDao.getAllRestaurants().collect { localRestaurants ->
                        emit(localRestaurants)
                        Log.w(TAG, "서버 오류로 로컬 데이터 사용: ${localRestaurants.size}개")
                    }
                }
            } else {
                // 오프라인: 로컬 데이터만 사용
                restaurantDao.getAllRestaurants().collect { localRestaurants ->
                    emit(localRestaurants)
                    Log.d(TAG, "오프라인 모드: 로컬 데이터 ${localRestaurants.size}개 사용")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAllRestaurants 오류", e)
            // 예외 발생 시 로컬 데이터 사용
            restaurantDao.getAllRestaurants().collect { localRestaurants ->
                emit(localRestaurants)
            }
        }
    }
    
    // 특정 맛집 조회
    suspend fun getRestaurantById(id: String): Restaurant? {
        return try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.getRestaurantById(id)
                if (response.isSuccessful && response.body()?.success == true) {
                    val restaurantDto = response.body()?.data
                    var restaurant = restaurantDto?.toRestaurant()
                    restaurant?.let {
                        // 사용자 북마크/즐겨찾기 상태 동기화
                        val syncedRestaurants = syncUserPreferences(listOf(it))
                        val syncedRestaurant = syncedRestaurants.firstOrNull() ?: it
                        
                        restaurantDao.insertRestaurant(syncedRestaurant)
                        Log.d(TAG, "서버에서 맛집 상세 정보 로드: ${syncedRestaurant.name}")
                        restaurant = syncedRestaurant
                    }
                    restaurant
                } else {
                    restaurantDao.getRestaurantById(id)
                }
            } else {
                restaurantDao.getRestaurantById(id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRestaurantById 오류", e)
            restaurantDao.getRestaurantById(id)
        }
    }
    
    // 맛집 검색 (서버 우선)
    fun searchRestaurants(query: String): Flow<List<Restaurant>> = flow {
        try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.searchRestaurants(query)
                if (response.isSuccessful && response.body()?.success == true) {
                    val restaurantDtos = response.body()?.data ?: emptyList()
                    var restaurants = restaurantDtos.toRestaurantList()
                    
                    // 사용자 북마크/즐겨찾기 상태 동기화
                    restaurants = syncUserPreferences(restaurants)
                    
                    // 검색 결과를 로컬에 저장 (캐싱)
                    restaurants.forEach { restaurantDao.insertRestaurant(it) }
                    
                    emit(restaurants)
                    Log.d(TAG, "서버 검색 결과: ${restaurants.size}개 (쿼리: $query)")
                } else {
                    // 서버 검색 실패 시 로컬 검색
                    restaurantDao.searchRestaurants(query).collect { localResults ->
                        emit(localResults)
                        Log.w(TAG, "로컬 검색 사용: ${localResults.size}개 (쿼리: $query)")
                    }
                }
            } else {
                // 오프라인 시 로컬 검색만
                restaurantDao.searchRestaurants(query).collect { localResults ->
                    emit(localResults)
                    Log.d(TAG, "오프라인 검색: ${localResults.size}개 (쿼리: $query)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchRestaurants 오류", e)
            restaurantDao.searchRestaurants(query).collect { emit(it) }
        }
    }
    
    // 주변 맛집 검색
    suspend fun getNearbyRestaurants(lat: Double, lng: Double, radius: Double = 5.0): List<Restaurant> {
        return try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.getNearbyRestaurants(lat, lng, radius)
                if (response.isSuccessful && response.body()?.success == true) {
                    val restaurantDtos = response.body()?.data ?: emptyList()
                    var restaurants = restaurantDtos.toRestaurantList()
                    
                    // 사용자 북마크/즐겨찾기 상태 동기화
                    restaurants = syncUserPreferences(restaurants)
                    
                    // 주변 맛집 결과 캐싱
                    restaurants.forEach { restaurantDao.insertRestaurant(it) }
                    
                    Log.d(TAG, "주변 맛집 검색 결과: ${restaurants.size}개")
                    restaurants
                } else {
                    // 서버 실패 시 로컬 근사 검색
                    restaurantDao.getNearbyRestaurants(lat, lng, 20)
                }
            } else {
                restaurantDao.getNearbyRestaurants(lat, lng, 20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getNearbyRestaurants 오류", e)
            restaurantDao.getNearbyRestaurants(lat, lng, 20)
        }
    }
    
    // 북마크 토글 (서버 우선 동작)
    suspend fun toggleBookmark(restaurantId: String): Boolean {
        return try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.toggleBookmark(restaurantId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val isBookmarked = response.body()?.data?.get("isBookmarked") ?: false
                    
                    // 로컬 DB 동기화
                    restaurantDao.updateBookmarkStatus(restaurantId, isBookmarked)
                    
                    Log.d(TAG, "북마크 토글 완료: $restaurantId = $isBookmarked")
                    isBookmarked
                } else {
                    // 서버 실패 시 로컬만 업데이트
                    val restaurant = restaurantDao.getRestaurantById(restaurantId)
                    val newStatus = !(restaurant?.isBookmarked ?: false)
                    restaurantDao.updateBookmarkStatus(restaurantId, newStatus)
                    Log.w(TAG, "서버 실패로 로컬만 북마크 업데이트: $restaurantId = $newStatus")
                    newStatus
                }
            } else {
                // 오프라인 시 로컬만 업데이트
                val restaurant = restaurantDao.getRestaurantById(restaurantId)
                val newStatus = !(restaurant?.isBookmarked ?: false)
                restaurantDao.updateBookmarkStatus(restaurantId, newStatus)
                Log.d(TAG, "오프라인 북마크 토글: $restaurantId = $newStatus")
                newStatus
            }
        } catch (e: Exception) {
            Log.e(TAG, "toggleBookmark 오류", e)
            false
        }
    }
    
    // 즐겨찾기 토글 (서버 우선 동작)
    suspend fun toggleFavorite(restaurantId: String): Boolean {
        return try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.toggleFavorite(restaurantId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val isFavorite = response.body()?.data?.get("isFavorite") ?: false
                    
                    // 로컬 DB 동기화
                    restaurantDao.updateFavoriteStatus(restaurantId, isFavorite)
                    
                    Log.d(TAG, "즐겨찾기 토글 완료: $restaurantId = $isFavorite")
                    isFavorite
                } else {
                    // 서버 실패 시 로컬만 업데이트
                    val restaurant = restaurantDao.getRestaurantById(restaurantId)
                    val newStatus = !(restaurant?.isFavorite ?: false)
                    restaurantDao.updateFavoriteStatus(restaurantId, newStatus)
                    Log.w(TAG, "서버 실패로 로컬만 즐겨찾기 업데이트: $restaurantId = $newStatus")
                    newStatus
                }
            } else {
                // 오프라인 시 로컬만 업데이트
                val restaurant = restaurantDao.getRestaurantById(restaurantId)
                val newStatus = !(restaurant?.isFavorite ?: false)
                restaurantDao.updateFavoriteStatus(restaurantId, newStatus)
                Log.d(TAG, "오프라인 즐겨찾기 토글: $restaurantId = $newStatus")
                newStatus
            }
        } catch (e: Exception) {
            Log.e(TAG, "toggleFavorite 오류", e)
            false
        }
    }
    
    // 북마크된 맛집 목록 (서버에서 동기화)
    fun getBookmarkedRestaurantsFromServer(): Flow<List<Restaurant>> = flow {
        try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.getBookmarkedRestaurants()
                if (response.isSuccessful && response.body()?.success == true) {
                    val restaurantDtos = response.body()?.data ?: emptyList()
                    val restaurants = restaurantDtos.toRestaurantList()
                    
                    // 서버에서 받은 북마크 목록으로 로컬 동기화
                    val localBookmarks = restaurantDao.getBookmarkedRestaurantsSync()
                    localBookmarks.forEach { local ->
                        if (!restaurants.any { it.id == local.id }) {
                            restaurantDao.updateBookmarkStatus(local.id, false)
                        }
                    }
                    
                    restaurants.forEach { restaurant ->
                        val updatedRestaurant = restaurant.copy(isBookmarked = true)
                        restaurantDao.insertRestaurant(updatedRestaurant)
                    }
                    
                    emit(restaurants)
                    Log.d(TAG, "서버에서 북마크 목록 동기화: ${restaurants.size}개")
                } else {
                    restaurantDao.getBookmarkedRestaurants().collect { emit(it) }
                }
            } else {
                restaurantDao.getBookmarkedRestaurants().collect { emit(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getBookmarkedRestaurantsFromServer 오류", e)
            restaurantDao.getBookmarkedRestaurants().collect { emit(it) }
        }
    }
    
    // 즐겨찾기 맛집 목록 (서버에서 동기화)
    fun getFavoriteRestaurantsFromServer(): Flow<List<Restaurant>> = flow {
        try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.getFavoriteRestaurants()
                if (response.isSuccessful && response.body()?.success == true) {
                    val restaurantDtos = response.body()?.data ?: emptyList()
                    val restaurants = restaurantDtos.toRestaurantList()
                    
                    // 서버에서 받은 즐겨찾기 목록으로 로컬 동기화
                    val localFavorites = restaurantDao.getFavoriteRestaurantsSync()
                    localFavorites.forEach { local ->
                        if (!restaurants.any { it.id == local.id }) {
                            restaurantDao.updateFavoriteStatus(local.id, false)
                        }
                    }
                    
                    restaurants.forEach { restaurant ->
                        val updatedRestaurant = restaurant.copy(isFavorite = true)
                        restaurantDao.insertRestaurant(updatedRestaurant)
                    }
                    
                    emit(restaurants)
                    Log.d(TAG, "서버에서 즐겨찾기 목록 동기화: ${restaurants.size}개")
                } else {
                    restaurantDao.getFavoriteRestaurants().collect { emit(it) }
                }
            } else {
                restaurantDao.getFavoriteRestaurants().collect { emit(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFavoriteRestaurantsFromServer 오류", e)
            restaurantDao.getFavoriteRestaurants().collect { emit(it) }
        }
    }
    
    // 로컬 북마크 목록 (빠른 조회용)
    fun getBookmarkedRestaurants(): Flow<List<Restaurant>> = restaurantDao.getBookmarkedRestaurants()
    
    // 로컬 즐겨찾기 목록 (빠른 조회용)
    fun getFavoriteRestaurants(): Flow<List<Restaurant>> = restaurantDao.getFavoriteRestaurants()
    
    // 카테고리별 맛집 조회 (기존 호환성용)
    fun getRestaurantsByCategory(category: String): Flow<List<Restaurant>> = restaurantDao.getRestaurantsByCategory(category)
    
    // 상위 카테고리별 맛집 조회 (새로운 시스템)
    fun getRestaurantsByMainCategory(mainCategory: String): Flow<List<Restaurant>> = flow {
        try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.getRestaurantsByMainCategory(mainCategory)
                if (response.isSuccessful && response.body()?.success == true) {
                    val restaurantDtos = response.body()?.data ?: emptyList()
                    var restaurants = restaurantDtos.toRestaurantList()
                    
                    // 사용자 북마크/즐겨찾기 상태 동기화
                    restaurants = syncUserPreferences(restaurants)
                    
                    // 캐싱
                    restaurants.forEach { restaurantDao.insertRestaurant(it) }
                    
                    emit(restaurants)
                    Log.d(TAG, "서버에서 $mainCategory 카테고리 맛집 조회: ${restaurants.size}개")
                } else {
                    // 서버 실패 시 로컬 데이터 사용
                    restaurantDao.getRestaurantsByMainCategory(mainCategory).collect { emit(it) }
                }
            } else {
                // 오프라인 시 로컬 데이터만
                restaurantDao.getRestaurantsByMainCategory(mainCategory).collect { emit(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRestaurantsByMainCategory 오류", e)
            restaurantDao.getRestaurantsByMainCategory(mainCategory).collect { emit(it) }
        }
    }
    
    // 하위 카테고리별 맛집 조회
    fun getRestaurantsBySubCategory(mainCategory: String, subCategory: String): Flow<List<Restaurant>> = flow {
        try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.getRestaurantsBySubCategory(mainCategory, subCategory)
                if (response.isSuccessful && response.body()?.success == true) {
                    val restaurantDtos = response.body()?.data ?: emptyList()
                    var restaurants = restaurantDtos.toRestaurantList()
                    
                    // 사용자 북마크/즐겨찾기 상태 동기화
                    restaurants = syncUserPreferences(restaurants)
                    
                    emit(restaurants)
                    Log.d(TAG, "서버에서 $mainCategory > $subCategory 하위카테고리 맛집 조회: ${restaurants.size}개")
                } else {
                    // 서버 실패 시 로컬 데이터 사용
                    restaurantDao.getRestaurantsBySubCategory(mainCategory, subCategory).collect { emit(it) }
                }
            } else {
                // 오프라인 시 로컬 데이터만
                restaurantDao.getRestaurantsBySubCategory(mainCategory, subCategory).collect { emit(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRestaurantsBySubCategory 오류", e)
            restaurantDao.getRestaurantsBySubCategory(mainCategory, subCategory).collect { emit(it) }
        }
    }
    
    // 특정 카테고리 내 검색
    fun searchInCategory(categoryName: String, query: String): Flow<List<Restaurant>> = flow {
        try {
            if (NetworkModule.isNetworkAvailable(context)) {
                val response = apiService.searchInCategory(categoryName, query)
                if (response.isSuccessful && response.body()?.success == true) {
                    val restaurantDtos = response.body()?.data ?: emptyList()
                    var restaurants = restaurantDtos.toRestaurantList()
                    
                    // 사용자 북마크/즐겨찾기 상태 동기화
                    restaurants = syncUserPreferences(restaurants)
                    
                    emit(restaurants)
                    Log.d(TAG, "$categoryName 내 검색 결과: ${restaurants.size}개 (쿼리: $query)")
                } else {
                    // 서버 실패 시 로컬 검색
                    restaurantDao.searchInCategory(categoryName, query).collect { emit(it) }
                }
            } else {
                // 오프라인 시 로컬 검색만
                restaurantDao.searchInCategory(categoryName, query).collect { emit(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchInCategory 오류", e)
            restaurantDao.searchInCategory(categoryName, query).collect { emit(it) }
        }
    }
    
    // 고평점 맛집 조회
    fun getHighRatedRestaurants(minRating: Double = 4.0): Flow<List<Restaurant>> = restaurantDao.getHighRatedRestaurants(minRating)
    
    // 사용자 북마크/즐겨찾기 상태 동기화
    private suspend fun syncUserPreferences(restaurants: List<Restaurant>): List<Restaurant> {
        return try {
            if (NetworkModule.isNetworkAvailable(context)) {
                // 서버에서 사용자의 북마크 및 즐겨찾기 목록 조회
                val bookmarksResponse = apiService.getBookmarkedRestaurants()
                val favoritesResponse = apiService.getFavoriteRestaurants()
                
                val bookmarkedIds = if (bookmarksResponse.isSuccessful && bookmarksResponse.body()?.success == true) {
                    bookmarksResponse.body()?.data?.map { it.id } ?: emptyList()
                } else {
                    emptyList()
                }
                
                val favoriteIds = if (favoritesResponse.isSuccessful && favoritesResponse.body()?.success == true) {
                    favoritesResponse.body()?.data?.map { it.id } ?: emptyList()
                } else {
                    emptyList()
                }
                
                // 맛집 데이터에 사용자 상태 반영
                restaurants.map { restaurant ->
                    restaurant.copy(
                        isBookmarked = bookmarkedIds.contains(restaurant.id),
                        isFavorite = favoriteIds.contains(restaurant.id)
                    )
                }
            } else {
                // 오프라인 시 로컬 데이터 사용
                restaurants
            }
        } catch (e: Exception) {
            Log.e(TAG, "사용자 상태 동기화 오류", e)
            restaurants
        }
    }
}