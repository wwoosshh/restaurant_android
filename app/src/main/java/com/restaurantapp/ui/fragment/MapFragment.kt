package com.restaurantapp.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.restaurantapp.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : Fragment() {
    
    companion object {
        private const val TAG = "MapFragment"
    }
    
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "MapFragment 초기화 완료")
        
        // 향후 Google Maps 또는 다른 지도 서비스 구현 예정
        // 현재는 플레이스홀더로 동작
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}