package com.restaurantapp.ui.category

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.restaurantapp.R
import com.restaurantapp.data.model.CategoryData
import com.restaurantapp.databinding.ActivityCategoryDetailBinding
import com.restaurantapp.ui.adapter.RestaurantAdapter
import com.restaurantapp.ui.viewmodel.RestaurantViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryDetailActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CategoryDetailActivity"
        const val EXTRA_CATEGORY_NAME = "category_name"
    }
    
    private lateinit var binding: ActivityCategoryDetailBinding
    private val restaurantViewModel: RestaurantViewModel by viewModels()
    private lateinit var restaurantAdapter: RestaurantAdapter
    
    private var categoryName: String = ""
    private var currentSubCategory: String = "전체"
    private var isSearchMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get category name from intent
        categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: ""
        if (categoryName.isEmpty()) {
            finish()
            return
        }
        
        setupToolbar()
        setupRecyclerView()
        setupSubCategories()
        setupObservers()
        setupClickListeners()
        
        // Load restaurants for this category
        loadCategoryRestaurants()
        
        Log.d(TAG, "CategoryDetailActivity 초기화 완료 - 카테고리: $categoryName")
    }
    
    private fun setupToolbar() {
        binding.toolbar.title = categoryName
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        restaurantAdapter = RestaurantAdapter(
            onItemClick = { restaurant ->
                // 맛집 상세 화면으로 이동 (향후 구현)
                Log.d(TAG, "맛집 클릭: ${restaurant.name}")
                Toast.makeText(this, "맛집 상세: ${restaurant.name}", Toast.LENGTH_SHORT).show()
            },
            onBookmarkClick = { restaurant ->
                restaurantViewModel.toggleBookmark(restaurant.id)
                Log.d(TAG, "북마크 토글: ${restaurant.name}")
            },
            onFavoriteClick = { restaurant ->
                restaurantViewModel.toggleFavorite(restaurant.id)
                Log.d(TAG, "즐겨찾기 토글: ${restaurant.name}")
            }
        )
        
        binding.recyclerViewRestaurants.apply {
            layoutManager = LinearLayoutManager(this@CategoryDetailActivity)
            adapter = restaurantAdapter
        }
    }
    
    private fun setupSubCategories() {
        val category = CategoryData.getCategory(categoryName)
        if (category != null && category.hasSubCategories) {
            // Add subcategory chips dynamically
            for (subCategory in category.subCategories) {
                val chip = layoutInflater.inflate(R.layout.chip_filter, binding.chipGroupSubCategories, false) as Chip
                chip.text = subCategory
                chip.setOnClickListener {
                    selectSubCategory(subCategory, chip)
                }
                binding.chipGroupSubCategories.addView(chip)
            }
        } else {
            // Hide subcategory scroll view if no subcategories
            binding.scrollViewSubCategories.visibility = View.GONE
        }
    }
    
    private fun setupObservers() {
        // 맛집 목록 관찰
        restaurantViewModel.restaurants.observe(this) { restaurants ->
            restaurantAdapter.submitList(restaurants)
            binding.textViewResultCount.text = when {
                isSearchMode -> "검색 결과 ${restaurants.size}개"
                currentSubCategory == "전체" -> "$categoryName ${restaurants.size}개의 맛집"
                else -> "$currentSubCategory ${restaurants.size}개의 맛집"
            }
            Log.d(TAG, "맛집 목록 업데이트: ${restaurants.size}개")
        }
        
        // 로딩 상태 관찰
        restaurantViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        
        // 에러 메시지 관찰
        restaurantViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                restaurantViewModel.clearErrorMessage()
                Log.e(TAG, "RestaurantViewModel 오류: $it")
            }
        }
        
        // 성공 메시지 관찰
        restaurantViewModel.successMessage.observe(this) { successMessage ->
            successMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                restaurantViewModel.clearSuccessMessage()
                Log.d(TAG, "RestaurantViewModel 성공: $it")
            }
        }
    }
    
    private fun setupClickListeners() {
        // Search button in toolbar
        binding.buttonSearch.setOnClickListener {
            toggleSearchMode()
        }
        
        // All subcategories chip
        binding.chipAllSubCategories.setOnClickListener {
            selectSubCategory("전체", binding.chipAllSubCategories)
        }
        
        // Execute search button
        binding.buttonExecuteSearch.setOnClickListener {
            val query = binding.editTextSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchInCategory(query)
            } else {
                Toast.makeText(this, "검색어를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Search EditText enter key
        binding.editTextSearch.setOnEditorActionListener { _, _, _ ->
            binding.buttonExecuteSearch.performClick()
            true
        }
    }
    
    private fun selectSubCategory(subCategory: String, selectedChip: Chip) {
        // 이미 선택된 하위카테고리를 다시 누르면 무시
        if (currentSubCategory == subCategory && selectedChip.isChecked) {
            return
        }
        
        // 모든 하위카테고리 칩 선택 해제
        clearSubCategorySelections()
        
        // 선택된 칩만 체크
        selectedChip.isChecked = true
        currentSubCategory = subCategory
        isSearchMode = false
        binding.layoutSearch.visibility = View.GONE
        
        // 하위카테고리에 따른 데이터 로드
        if (subCategory == "전체") {
            loadCategoryRestaurants()
        } else {
            filterBySubCategory(subCategory)
        }
        
        Log.d(TAG, "$categoryName > $subCategory 선택")
    }
    
    private fun clearSubCategorySelections() {
        binding.chipAllSubCategories.isChecked = false
        for (i in 0 until binding.chipGroupSubCategories.childCount) {
            val chip = binding.chipGroupSubCategories.getChildAt(i) as? Chip
            chip?.isChecked = false
        }
    }
    
    private fun toggleSearchMode() {
        isSearchMode = !isSearchMode
        binding.layoutSearch.visibility = if (isSearchMode) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        if (!isSearchMode) {
            // 검색 모드 종료 시 현재 선택된 하위카테고리로 복귀
            if (currentSubCategory == "전체") {
                loadCategoryRestaurants()
            } else {
                filterBySubCategory(currentSubCategory)
            }
        }
    }
    
    private fun loadCategoryRestaurants() {
        restaurantViewModel.filterByMainCategory(categoryName)
    }
    
    private fun filterBySubCategory(subCategory: String) {
        restaurantViewModel.filterBySubCategory(categoryName, subCategory)
    }
    
    private fun searchInCategory(query: String) {
        restaurantViewModel.searchInCategory(categoryName, query)
        isSearchMode = true
        clearSubCategorySelections()
        Log.d(TAG, "$categoryName 내 검색: $query")
    }
}