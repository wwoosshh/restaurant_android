package com.restaurantapp.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.restaurantapp.R
import com.restaurantapp.data.model.CategoryData
import com.restaurantapp.databinding.FragmentHomeBinding
import com.restaurantapp.ui.adapter.RestaurantAdapter
import com.restaurantapp.ui.category.CategoryDetailActivity
import com.restaurantapp.ui.viewmodel.AuthViewModel
import com.restaurantapp.ui.viewmodel.RestaurantViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    
    companion object {
        private const val TAG = "HomeFragment"
    }
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    // ViewModel을 Activity와 공유
    private val restaurantViewModel: RestaurantViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var restaurantAdapter: RestaurantAdapter
    
    // 현재 뷰 모드 추적
    private enum class ViewMode { ALL, BOOKMARKS, FAVORITES }
    private var currentViewMode = ViewMode.ALL
    
    // 현재 선택된 카테고리 추적
    private var currentSelectedCategory = "전체"
    
    // 확장 카테고리 표시 상태
    private var isExpanded = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupChipListeners()
        setupExpandedCategories()
        
        Log.d(TAG, "HomeFragment 초기화 완료")
    }
    
    private fun setupRecyclerView() {
        restaurantAdapter = RestaurantAdapter(
            onItemClick = { restaurant ->
                // 맛집 상세 화면으로 이동 (향후 구현)
                Log.d(TAG, "맛집 클릭: ${restaurant.name}")
                Toast.makeText(requireContext(), "맛집 상세: ${restaurant.name}", Toast.LENGTH_SHORT).show()
            },
            onBookmarkClick = { restaurant ->
                // 로그인 상태 확인
                if (authViewModel.isLoggedIn.value == true) {
                    restaurantViewModel.toggleBookmark(restaurant.id)
                    Log.d(TAG, "북마크 토글: ${restaurant.name}")
                } else {
                    // 로그인 안 된 상태에서는 프로필 화면으로 리다이렉트
                    Toast.makeText(requireContext(), "북마크 기능을 사용하려면 로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                    (requireActivity() as? NavigationHost)?.navigateToProfile()
                }
            },
            onFavoriteClick = { restaurant ->
                // 로그인 상태 확인
                if (authViewModel.isLoggedIn.value == true) {
                    restaurantViewModel.toggleFavorite(restaurant.id)
                    Log.d(TAG, "즐겨찾기 토글: ${restaurant.name}")
                } else {
                    // 로그인 안 된 상태에서는 프로필 화면으로 리다이렉트
                    Toast.makeText(requireContext(), "즐겨찾기 기능을 사용하려면 로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                    (requireActivity() as? NavigationHost)?.navigateToProfile()
                }
            }
        )
        
        binding.recyclerViewRestaurants.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = restaurantAdapter
        }
    }
    
    private fun setupObservers() {
        // 맛집 목록 관찰 - 전체 모드일 때만 표시
        restaurantViewModel.restaurants.observe(viewLifecycleOwner) { restaurants ->
            if (currentViewMode == ViewMode.ALL) {
                restaurantAdapter.submitList(restaurants)
                binding.textViewResultCount.text = "총 ${restaurants.size}개의 맛집"
            }
            Log.d(TAG, "맛집 목록 업데이트: ${restaurants.size}개")
        }
        
        // 북마크 목록 관찰 - 북마크 모드일 때만 표시
        restaurantViewModel.bookmarkedRestaurants.observe(viewLifecycleOwner) { bookmarks ->
            if (currentViewMode == ViewMode.BOOKMARKS) {
                restaurantAdapter.submitList(bookmarks)
                binding.textViewResultCount.text = "북마크 ${bookmarks.size}개"
            }
            Log.d(TAG, "북마크 목록 업데이트: ${bookmarks.size}개")
        }
        
        // 즐겨찾기 목록 관찰 - 즐겨찾기 모드일 때만 표시
        restaurantViewModel.favoriteRestaurants.observe(viewLifecycleOwner) { favorites ->
            if (currentViewMode == ViewMode.FAVORITES) {
                restaurantAdapter.submitList(favorites)
                binding.textViewResultCount.text = "즐겨찾기 ${favorites.size}개"
            }
            Log.d(TAG, "즐겨찾기 목록 업데이트: ${favorites.size}개")
        }
        
        // 로딩 상태 관찰
        restaurantViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        
        // 에러 메시지 관찰
        restaurantViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                restaurantViewModel.clearErrorMessage()
                Log.e(TAG, "RestaurantViewModel 오류: $it")
            }
        }
        
        // 성공 메시지 관찰
        restaurantViewModel.successMessage.observe(viewLifecycleOwner) { successMessage ->
            successMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                restaurantViewModel.clearSuccessMessage()
                Log.d(TAG, "RestaurantViewModel 성공: $it")
            }
        }
        
        // 로그인 상태에 따른 버튼 활성화
        authViewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            binding.buttonBookmarks.isEnabled = isLoggedIn
            binding.buttonFavorites.isEnabled = isLoggedIn
            
            if (!isLoggedIn && (currentViewMode == ViewMode.BOOKMARKS || currentViewMode == ViewMode.FAVORITES)) {
                // 로그아웃 시 전체 모드로 전환
                currentViewMode = ViewMode.ALL
                restaurantViewModel.loadAllRestaurants()
                // 전체 모드로 복귀
            }
        }
    }
    
    private fun setupClickListeners() {
        // 전체보기 버튼 클릭
        binding.buttonAllRestaurants.setOnClickListener {
            currentViewMode = ViewMode.ALL
            currentSelectedCategory = "전체"
            restaurantViewModel.loadAllRestaurants()
            binding.editTextSearch.text.clear() // 검색어 초기화
            Log.d(TAG, "전체보기 모드 전환")
        }
        
        // 검색 버튼 클릭
        binding.buttonSearch.setOnClickListener {
            val query = binding.editTextSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                restaurantViewModel.searchRestaurants(query)
                currentViewMode = ViewMode.ALL
                Log.d(TAG, "검색 실행: $query")
            } else {
                Toast.makeText(requireContext(), "검색어를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 북마크 버튼 클릭
        binding.buttonBookmarks.setOnClickListener {
            if (authViewModel.isLoggedIn.value == true) {
                currentViewMode = ViewMode.BOOKMARKS
                restaurantViewModel.loadBookmarkedRestaurants()
                // 북마크/즐겨찾기 모드로 전환
                Log.d(TAG, "북마크 모드 전환")
            } else {
                Toast.makeText(requireContext(), "북마크를 보려면 로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                (requireActivity() as? NavigationHost)?.navigateToProfile()
            }
        }
        
        // 즐겨찾기 버튼 클릭
        binding.buttonFavorites.setOnClickListener {
            if (authViewModel.isLoggedIn.value == true) {
                currentViewMode = ViewMode.FAVORITES
                restaurantViewModel.loadFavoriteRestaurants()
                // 북마크/즐겨찾기 모드로 전환
                Log.d(TAG, "즐겨찾기 모드 전환")
            } else {
                Toast.makeText(requireContext(), "즐겨찾기를 보려면 로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                (requireActivity() as? NavigationHost)?.navigateToProfile()
            }
        }
        
        // 새로고침 버튼 클릭
        binding.buttonRefresh.setOnClickListener {
            when (currentViewMode) {
                ViewMode.ALL -> {
                    // 현재 선택된 카테고리에 따라 새로고침
                    if (currentSelectedCategory == "전체") {
                        restaurantViewModel.loadAllRestaurants()
                    } else {
                        restaurantViewModel.filterByCategory(currentSelectedCategory)
                    }
                }
                ViewMode.BOOKMARKS -> restaurantViewModel.loadBookmarkedRestaurants()
                ViewMode.FAVORITES -> restaurantViewModel.loadFavoriteRestaurants()
            }
            Log.d(TAG, "새로고침 실행 - 모드: $currentViewMode, 카테고리: $currentSelectedCategory")
        }
        
        // EditText 엔터 키 처리
        binding.editTextSearch.setOnEditorActionListener { _, _, _ ->
            binding.buttonSearch.performClick()
            true
        }
    }
    
    private fun setupChipListeners() {
        // 메인 카테고리 버튼들 - 카테고리 상세 화면으로 이동 (필터링 X, 네비게이션만)
        binding.chipKorean.setOnClickListener {
            navigateToCategoryDetail("한식")
        }
        
        binding.chipChinese.setOnClickListener {
            navigateToCategoryDetail("중식")
        }
        
        binding.chipJapanese.setOnClickListener {
            navigateToCategoryDetail("일식")
        }
        
        binding.chipWestern.setOnClickListener {
            navigateToCategoryDetail("양식")
        }
        
        binding.chipChicken.setOnClickListener {
            navigateToCategoryDetail("치킨")
        }
        
        // 더보기 버튼
        binding.chipExpand.setOnClickListener {
            isExpanded = !isExpanded
            binding.chipGroupExpandedCategories.visibility = if (isExpanded) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.chipExpand.text = if (isExpanded) "접기" else "더보기"
            Log.d(TAG, "카테고리 확장: $isExpanded")
        }
        
    }
    
    // 확장 카테고리 동적 생성 (네비게이션용)
    private fun setupExpandedCategories() {
        val expandedCategories = CategoryData.EXPANDED_CATEGORIES
        
        for (categoryName in expandedCategories) {
            val chip = layoutInflater.inflate(R.layout.chip_assist, binding.chipGroupExpandedCategories, false) as Chip
            chip.text = categoryName
            chip.setOnClickListener {
                navigateToCategoryDetail(categoryName)
            }
            binding.chipGroupExpandedCategories.addView(chip)
        }
    }
    
    // 카테고리 상세 화면으로 이동
    private fun navigateToCategoryDetail(categoryName: String) {
        val intent = Intent(requireContext(), CategoryDetailActivity::class.java)
        intent.putExtra(CategoryDetailActivity.EXTRA_CATEGORY_NAME, categoryName)
        startActivity(intent)
        Log.d(TAG, "$categoryName 카테고리 상세 화면으로 이동")
    }
    
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // NavigationHost 인터페이스 정의
    interface NavigationHost {
        fun navigateToProfile()
    }
}