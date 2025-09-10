package com.restaurantapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "restaurants")
data class Restaurant(
    @PrimaryKey 
    val id: String,
    
    val name: String,
    val mainCategory: String, // 상위 카테고리 (한식, 중식, 일식 등)
    val subCategory: String? = null, // 하위 카테고리 (김치찜, 짜장면 등)
    val category: String, // 기존 호환성을 위해 유지 (mainCategory와 동일)
    val address: String,
    val phone: String? = null,
    val rating: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isBookmarked: Boolean = false,
    val isFavorite: Boolean = false,
    val priceRange: String? = null,
    val openingHours: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 서버 응답용 DTO
data class RestaurantDto(
    @SerializedName("_id") val id: String,
    val name: String,
    val category: String,
    val mainCategory: String? = null,
    val subCategory: String? = null,
    val address: String,
    val phone: String? = null,
    val rating: Double = 0.0,
    val location: LocationDto,
    val priceRange: String? = null,
    val openingHours: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val reviewCount: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

data class LocationDto(
    val type: String = "Point",
    val coordinates: List<Double> // [longitude, latitude]
)

// DTO를 Entity로 변환
fun RestaurantDto.toRestaurant(): Restaurant {
    return Restaurant(
        id = this.id,
        name = this.name,
        mainCategory = this.mainCategory ?: this.category, // 신규 필드가 없으면 기존 category 사용
        subCategory = this.subCategory,
        category = this.category, // 기존 호환성 유지
        address = this.address,
        phone = this.phone,
        rating = this.rating,
        latitude = this.location.coordinates[1], // latitude
        longitude = this.location.coordinates[0], // longitude
        priceRange = this.priceRange,
        openingHours = this.openingHours,
        imageUrl = this.imageUrl,
        description = this.description,
        reviewCount = this.reviewCount
    )
}

fun List<RestaurantDto>.toRestaurantList(): List<Restaurant> {
    return this.map { it.toRestaurant() }
}