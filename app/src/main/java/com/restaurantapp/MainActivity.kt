package com.restaurantapp

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.restaurantapp.databinding.ActivityMainBinding
import com.restaurantapp.ui.fragment.HomeFragment
import com.restaurantapp.ui.fragment.MapFragment
import com.restaurantapp.ui.fragment.ProfileFragment
import com.restaurantapp.ui.viewmodel.AuthViewModel
import com.restaurantapp.ui.viewmodel.RestaurantViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), com.restaurantapp.ui.fragment.HomeFragment.NavigationHost {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var binding: ActivityMainBinding
    
    // ViewModels
    private val restaurantViewModel: RestaurantViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    // Fragments
    private val homeFragment = HomeFragment()
    private val mapFragment = MapFragment()
    private val profileFragment = ProfileFragment()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ViewBinding 설정
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // ActionBar 설정
        supportActionBar?.apply {
            title = "맛집 추천"
            setDisplayShowHomeEnabled(true)
        }
        
        setupBottomNavigation()
        
        // 초기 Fragment 설정
        if (savedInstanceState == null) {
            loadFragment(homeFragment)
            binding.bottomNavigation.selectedItemId = R.id.navigation_home
        }
        
        Log.d(TAG, "MainActivity 초기화 완료")
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(homeFragment)
                    supportActionBar?.title = "맛집 추천"
                    true
                }
                R.id.navigation_map -> {
                    loadFragment(mapFragment)
                    supportActionBar?.title = "지도"
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(profileFragment)
                    supportActionBar?.title = "프로필"
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
    
    // HomeFragment에서 프로필 화면으로 이동하기 위한 인터페이스 구현
    override fun navigateToProfile() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_profile
        loadFragment(profileFragment)
        supportActionBar?.title = "프로필"
        Log.d(TAG, "프로필 화면으로 이동")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity onDestroy")
    }
}