package com.restaurantapp.data.local

import androidx.room.*
import com.restaurantapp.data.model.Restaurant
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {
    
    // 기본 CRUD 작업
    @Query("SELECT * FROM restaurants ORDER BY updatedAt DESC")
    fun getAllRestaurants(): Flow<List<Restaurant>>
    
    @Query("SELECT * FROM restaurants WHERE id = :id")
    suspend fun getRestaurantById(id: String): Restaurant?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurant(restaurant: Restaurant)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurants(restaurants: List<Restaurant>)
    
    @Update
    suspend fun updateRestaurant(restaurant: Restaurant)
    
    @Delete
    suspend fun deleteRestaurant(restaurant: Restaurant)
    
    @Query("DELETE FROM restaurants")
    suspend fun deleteAllRestaurants()
    
    // 검색 기능
    @Query("SELECT * FROM restaurants WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY rating DESC")
    fun searchRestaurants(query: String): Flow<List<Restaurant>>
    
    @Query("SELECT * FROM restaurants WHERE category = :category ORDER BY rating DESC")
    fun getRestaurantsByCategory(category: String): Flow<List<Restaurant>>
    
    // 새로운 카테고리 시스템용 메서드들
    @Query("SELECT * FROM restaurants WHERE mainCategory = :mainCategory ORDER BY rating DESC")
    fun getRestaurantsByMainCategory(mainCategory: String): Flow<List<Restaurant>>
    
    @Query("SELECT * FROM restaurants WHERE mainCategory = :mainCategory AND subCategory = :subCategory ORDER BY rating DESC")
    fun getRestaurantsBySubCategory(mainCategory: String, subCategory: String): Flow<List<Restaurant>>
    
    @Query("SELECT * FROM restaurants WHERE mainCategory = :categoryName AND (name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' OR subCategory LIKE '%' || :query || '%') ORDER BY rating DESC")
    fun searchInCategory(categoryName: String, query: String): Flow<List<Restaurant>>
    
    // 북마크 관련
    @Query("SELECT * FROM restaurants WHERE isBookmarked = 1 ORDER BY updatedAt DESC")
    fun getBookmarkedRestaurants(): Flow<List<Restaurant>>
    
    @Query("UPDATE restaurants SET isBookmarked = :isBookmarked WHERE id = :restaurantId")
    suspend fun updateBookmarkStatus(restaurantId: String, isBookmarked: Boolean)
    
    // 즐겨찾기 관련
    @Query("SELECT * FROM restaurants WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteRestaurants(): Flow<List<Restaurant>>
    
    @Query("UPDATE restaurants SET isFavorite = :isFavorite WHERE id = :restaurantId")
    suspend fun updateFavoriteStatus(restaurantId: String, isFavorite: Boolean)
    
    // 위치 기반 검색 (간단한 거리 계산)
    @Query("SELECT * FROM restaurants ORDER BY (latitude - :userLat) * (latitude - :userLat) + (longitude - :userLng) * (longitude - :userLng) ASC LIMIT :limit")
    suspend fun getNearbyRestaurants(userLat: Double, userLng: Double, limit: Int = 20): List<Restaurant>
    
    // 평점별 정렬
    @Query("SELECT * FROM restaurants WHERE rating >= :minRating ORDER BY rating DESC, reviewCount DESC")
    fun getHighRatedRestaurants(minRating: Double = 4.0): Flow<List<Restaurant>>
    
    // 서버 동기화용 메서드 (Flow 없이 직접 데이터 반환)
    @Query("SELECT * FROM restaurants WHERE isBookmarked = 1")
    suspend fun getBookmarkedRestaurantsSync(): List<Restaurant>
    
    @Query("SELECT * FROM restaurants WHERE isFavorite = 1")
    suspend fun getFavoriteRestaurantsSync(): List<Restaurant>
    
    @Query("SELECT * FROM restaurants WHERE id IN (:restaurantIds)")
    suspend fun getRestaurantsByIds(restaurantIds: List<String>): List<Restaurant>
    
    // 통계 정보
    @Query("SELECT COUNT(*) FROM restaurants")
    suspend fun getRestaurantCount(): Int
    
    @Query("SELECT COUNT(*) FROM restaurants WHERE isBookmarked = 1")
    suspend fun getBookmarkCount(): Int
    
    @Query("SELECT COUNT(*) FROM restaurants WHERE isFavorite = 1")
    suspend fun getFavoriteCount(): Int
}