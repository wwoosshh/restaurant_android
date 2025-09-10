package com.restaurantapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.restaurantapp.data.local.AppDatabase
import com.restaurantapp.data.model.Restaurant
import com.restaurantapp.data.network.NetworkModule
import com.restaurantapp.data.repository.RestaurantRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RestaurantViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: RestaurantRepository
    
    // UI 상태 관리
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    // 검색 관련
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow("전체")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    
    // 맛집 데이터
    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> = _restaurants
    
    private val _selectedRestaurant = MutableLiveData<Restaurant?>()
    val selectedRestaurant: LiveData<Restaurant?> = _selectedRestaurant
    
    // 북마크 및 즐겨찾기
    private val _bookmarkedRestaurants = MutableLiveData<List<Restaurant>>()
    val bookmarkedRestaurants: LiveData<List<Restaurant>> = _bookmarkedRestaurants
    
    private val _favoriteRestaurants = MutableLiveData<List<Restaurant>>()
    val favoriteRestaurants: LiveData<List<Restaurant>> = _favoriteRestaurants
    
    init {
        // Repository 초기화
        val database = AppDatabase.getDatabase(application)
        val apiService = NetworkModule.getApiService()
        repository = RestaurantRepository(
            database.restaurantDao(),
            apiService,
            application
        )
        
        // 초기 데이터 로드
        loadAllRestaurants()
    }
    
    // 모든 맛집 로드
    fun loadAllRestaurants() {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCategory.value = "전체"
            try {
                repository.getAllRestaurants().collect { restaurantList ->
                    _restaurants.value = restaurantList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "맛집 목록을 불러오는데 실패했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 검색 실행
    fun searchRestaurants(query: String) {
        if (query.isBlank()) {
            loadAllRestaurants()
            return
        }
        
        _searchQuery.value = query
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.searchRestaurants(query).collect { searchResults ->
                    _restaurants.value = searchResults
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "검색 중 오류가 발생했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 주변 맛집 검색
    fun searchNearbyRestaurants(latitude: Double, longitude: Double, radius: Double = 5.0) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val nearbyRestaurants = repository.getNearbyRestaurants(latitude, longitude, radius)
                _restaurants.value = nearbyRestaurants
                _successMessage.value = "${nearbyRestaurants.size}개의 주변 맛집을 찾았습니다"
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "주변 맛집 검색 중 오류가 발생했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 특정 맛집 상세 조회
    fun getRestaurantById(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val restaurant = repository.getRestaurantById(id)
                _selectedRestaurant.value = restaurant
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "맛집 정보를 불러오는데 실패했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 북마크 토글 (CLAUDE.md 규칙 준수: 서버 우선)
    fun toggleBookmark(restaurantId: String) {
        viewModelScope.launch {
            try {
                val isBookmarked = repository.toggleBookmark(restaurantId)
                val message = if (isBookmarked) "북마크에 추가되었습니다" else "북마크에서 제거되었습니다"
                _successMessage.value = message
                
                // 현재 표시중인 전체 목록에서 해당 맛집의 북마크 상태 업데이트
                _restaurants.value?.let { currentList ->
                    val updatedList = currentList.map { restaurant ->
                        if (restaurant.id == restaurantId) {
                            restaurant.copy(isBookmarked = isBookmarked)
                        } else {
                            restaurant
                        }
                    }
                    _restaurants.value = updatedList
                }
                
                // 현재 선택된 맛집의 북마크 상태 업데이트
                _selectedRestaurant.value?.let { restaurant ->
                    if (restaurant.id == restaurantId) {
                        _selectedRestaurant.value = restaurant.copy(isBookmarked = isBookmarked)
                    }
                }
                
                // 북마크 목록 새로고침
                loadBookmarkedRestaurants()
                
            } catch (e: Exception) {
                _errorMessage.value = "북마크 처리 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }
    
    // 즐겨찾기 토글 (CLAUDE.md 규칙 준수: 서버 우선)
    fun toggleFavorite(restaurantId: String) {
        viewModelScope.launch {
            try {
                val isFavorite = repository.toggleFavorite(restaurantId)
                val message = if (isFavorite) "즐겨찾기에 추가되었습니다" else "즐겨찾기에서 제거되었습니다"
                _successMessage.value = message
                
                // 현재 표시중인 전체 목록에서 해당 맛집의 즐겨찾기 상태 업데이트
                _restaurants.value?.let { currentList ->
                    val updatedList = currentList.map { restaurant ->
                        if (restaurant.id == restaurantId) {
                            restaurant.copy(isFavorite = isFavorite)
                        } else {
                            restaurant
                        }
                    }
                    _restaurants.value = updatedList
                }
                
                // 현재 선택된 맛집의 즐겨찾기 상태 업데이트
                _selectedRestaurant.value?.let { restaurant ->
                    if (restaurant.id == restaurantId) {
                        _selectedRestaurant.value = restaurant.copy(isFavorite = isFavorite)
                    }
                }
                
                // 즐겨찾기 목록 새로고침
                loadFavoriteRestaurants()
                
            } catch (e: Exception) {
                _errorMessage.value = "즐겨찾기 처리 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }
    
    // 북마크 목록 로드 (서버에서 동기화)
    fun loadBookmarkedRestaurants() {
        viewModelScope.launch {
            try {
                repository.getBookmarkedRestaurantsFromServer().collect { bookmarks ->
                    _bookmarkedRestaurants.value = bookmarks
                }
            } catch (e: Exception) {
                _errorMessage.value = "북마크 목록을 불러오는데 실패했습니다: ${e.message}"
                // 서버 실패 시 로컬 데이터 사용
                repository.getBookmarkedRestaurants().collect { localBookmarks ->
                    _bookmarkedRestaurants.value = localBookmarks
                }
            }
        }
    }
    
    // 즐겨찾기 목록 로드 (서버에서 동기화)
    fun loadFavoriteRestaurants() {
        viewModelScope.launch {
            try {
                repository.getFavoriteRestaurantsFromServer().collect { favorites ->
                    _favoriteRestaurants.value = favorites
                }
            } catch (e: Exception) {
                _errorMessage.value = "즐겨찾기 목록을 불러오는데 실패했습니다: ${e.message}"
                // 서버 실패 시 로컬 데이터 사용
                repository.getFavoriteRestaurants().collect { localFavorites ->
                    _favoriteRestaurants.value = localFavorites
                }
            }
        }
    }
    
    // 고평점 맛집 로드
    fun loadHighRatedRestaurants(minRating: Double = 4.0) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getHighRatedRestaurants(minRating).collect { highRatedRestaurants ->
                    _restaurants.value = highRatedRestaurants
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "고평점 맛집을 불러오는데 실패했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 검색어 업데이트
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    // 선택된 맛집 클리어
    fun clearSelectedRestaurant() {
        _selectedRestaurant.value = null
    }
    
    // 에러 메시지 클리어
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    // 성공 메시지 클리어
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    // 카테고리별 필터링
    fun filterByCategory(category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCategory.value = category
            try {
                if (category == "전체") {
                    // 전체 카테고리일 때는 모든 레스토랑 로드
                    repository.getAllRestaurants().collect { allRestaurants ->
                        _restaurants.value = allRestaurants
                        _isLoading.value = false
                    }
                } else {
                    // 특정 카테고리 필터링
                    repository.getRestaurantsByCategory(category).collect { filteredRestaurants ->
                        _restaurants.value = filteredRestaurants
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "카테고리별 맛집을 불러오는데 실패했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 데이터 새로고침 - 현재 선택된 카테고리 유지
    fun refreshData() {
        val currentCategory = _selectedCategory.value
        if (currentCategory == "전체" || currentCategory.isEmpty()) {
            loadAllRestaurants()
        } else {
            filterByCategory(currentCategory)
        }
        loadBookmarkedRestaurants()
        loadFavoriteRestaurants()
    }
    
    // 현재 선택된 카테고리 가져오기
    fun getCurrentCategory(): String {
        return _selectedCategory.value
    }
    
    // 상위 카테고리별 필터링 (새로운 카테고리 시스템용)
    fun filterByMainCategory(mainCategory: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCategory.value = mainCategory
            try {
                repository.getRestaurantsByMainCategory(mainCategory).collect { restaurants ->
                    _restaurants.value = restaurants
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "카테고리별 맛집을 불러오는데 실패했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 하위 카테고리별 필터링
    fun filterBySubCategory(mainCategory: String, subCategory: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCategory.value = "$mainCategory > $subCategory"
            try {
                repository.getRestaurantsBySubCategory(mainCategory, subCategory).collect { restaurants ->
                    _restaurants.value = restaurants
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "하위카테고리별 맛집을 불러오는데 실패했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 특정 카테고리 내에서 검색
    fun searchInCategory(categoryName: String, query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _searchQuery.value = query
            _selectedCategory.value = "$categoryName 검색: $query"
            try {
                repository.searchInCategory(categoryName, query).collect { restaurants ->
                    _restaurants.value = restaurants
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "카테고리 내 검색에 실패했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // ViewModel이 클리어될 때 정리 작업
    override fun onCleared() {
        super.onCleared()
        clearSelectedRestaurant()
        clearErrorMessage()
        clearSuccessMessage()
    }
}