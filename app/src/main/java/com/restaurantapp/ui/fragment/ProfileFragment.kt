package com.restaurantapp.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.restaurantapp.databinding.FragmentProfileBinding
import com.restaurantapp.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    
    companion object {
        private const val TAG = "ProfileFragment"
    }
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    // ViewModel을 Activity와 공유
    private val authViewModel: AuthViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
        
        Log.d(TAG, "ProfileFragment 초기화 완료")
    }
    
    private fun setupObservers() {
        // 로그인 상태 관찰
        authViewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            updateUI(isLoggedIn)
        }
        
        // 현재 사용자 정보 관찰
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.textViewUsername.text = it.username
                binding.textViewEmail.text = it.email
                binding.textViewEmail.visibility = View.VISIBLE
            }
        }
        
        // 인증 에러 메시지 관찰
        authViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), "인증 오류: $it", Toast.LENGTH_LONG).show()
                authViewModel.clearErrorMessage()
                Log.e(TAG, "AuthViewModel 오류: $it")
            }
        }
        
        // 인증 성공 메시지 관찰
        authViewModel.successMessage.observe(viewLifecycleOwner) { successMessage ->
            successMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                authViewModel.clearSuccessMessage()
                Log.d(TAG, "AuthViewModel 성공: $it")
            }
        }
        
        // 사용자 통계 관찰
        authViewModel.userStats.observe(viewLifecycleOwner) { stats ->
            binding.textViewBookmarkCount.text = stats["bookmarks"]?.toString() ?: "0"
            binding.textViewFavoriteCount.text = stats["favorites"]?.toString() ?: "0"
        }
    }
    
    private fun setupClickListeners() {
        // 로그인 버튼
        binding.buttonLogin.setOnClickListener {
            showLoginDialog()
        }
        
        // 회원가입 버튼
        binding.buttonRegister.setOnClickListener {
            showRegisterDialog()
        }
        
        // 로그아웃 버튼
        binding.buttonLogout.setOnClickListener {
            authViewModel.logout()
            Log.d(TAG, "로그아웃 실행")
        }
        
        // 프로필 수정 버튼
        binding.buttonEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
        
        // 비밀번호 변경 버튼
        binding.buttonChangePassword.setOnClickListener {
            Toast.makeText(requireContext(), "비밀번호 변경 기능은 곧 구현 예정입니다", Toast.LENGTH_SHORT).show()
        }
        
        // 계정 삭제 버튼
        binding.buttonDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }
    
    private fun updateUI(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            binding.layoutLoggedOut.visibility = View.GONE
            binding.layoutLoggedIn.visibility = View.VISIBLE
            
            // 사용자 통계 로드
            authViewModel.loadUserStats()
        } else {
            binding.layoutLoggedOut.visibility = View.VISIBLE
            binding.layoutLoggedIn.visibility = View.GONE
            binding.textViewUsername.text = "로그인이 필요합니다"
            binding.textViewEmail.visibility = View.GONE
        }
    }
    
    private fun showLoginDialog() {
        val editTextEmail = EditText(requireContext()).apply {
            hint = "이메일"
            setText("tester@example.com") // 테스트용 기본값
        }
        val editTextPassword = EditText(requireContext()).apply {
            hint = "비밀번호"
            setText("test123456") // 테스트용 기본값
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(editTextEmail)
            addView(editTextPassword)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("로그인")
            .setView(layout)
            .setPositiveButton("로그인") { _, _ ->
                val email = editTextEmail.text.toString().trim()
                val password = editTextPassword.text.toString().trim()
                
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    authViewModel.login(email, password)
                    Log.d(TAG, "로그인 시도: $email")
                } else {
                    Toast.makeText(requireContext(), "이메일과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .setNeutralButton("회원가입") { _, _ ->
                showRegisterDialog()
            }
            .show()
    }
    
    private fun showRegisterDialog() {
        val editTextUsername = EditText(requireContext()).apply {
            hint = "사용자명"
        }
        val editTextEmail = EditText(requireContext()).apply {
            hint = "이메일"
        }
        val editTextPassword = EditText(requireContext()).apply {
            hint = "비밀번호 (영문자+숫자 6자 이상)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(editTextUsername)
            addView(editTextEmail)
            addView(editTextPassword)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("회원가입")
            .setView(layout)
            .setPositiveButton("가입") { _, _ ->
                val username = editTextUsername.text.toString().trim()
                val email = editTextEmail.text.toString().trim()
                val password = editTextPassword.text.toString().trim()
                
                if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                    authViewModel.register(username, email, password)
                    Log.d(TAG, "회원가입 시도: $email")
                } else {
                    Toast.makeText(requireContext(), "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun showEditProfileDialog() {
        val currentUser = authViewModel.currentUser.value
        if (currentUser == null) {
            Toast.makeText(requireContext(), "사용자 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        val editTextUsername = EditText(requireContext()).apply {
            hint = "사용자명"
            setText(currentUser.username)
        }
        
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(editTextUsername)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("프로필 수정")
            .setView(layout)
            .setPositiveButton("저장") { _, _ ->
                val newUsername = editTextUsername.text.toString().trim()
                if (newUsername.isNotEmpty() && newUsername != currentUser.username) {
                    val updates = mapOf("username" to newUsername)
                    authViewModel.updateUserProfile(updates)
                    Log.d(TAG, "프로필 업데이트: $newUsername")
                } else {
                    Toast.makeText(requireContext(), "변경할 내용이 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("계정 삭제")
            .setMessage("정말로 계정을 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없으며, 모든 데이터가 영구적으로 삭제됩니다.")
            .setPositiveButton("삭제") { _, _ ->
                authViewModel.deleteAccount()
                Log.d(TAG, "계정 삭제 실행")
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}