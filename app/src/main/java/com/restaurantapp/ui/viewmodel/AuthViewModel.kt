package com.restaurantapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.restaurantapp.data.model.AuthResponse
import com.restaurantapp.data.model.User
import com.restaurantapp.data.network.NetworkModule
import com.restaurantapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: AuthRepository
    
    // UI 상태 관리
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    // 인증 상태
    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn
    
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    // 인증 응답
    private val _authResponse = MutableLiveData<AuthResponse?>()
    val authResponse: LiveData<AuthResponse?> = _authResponse
    
    // 이메일 인증 관련
    private val _isEmailVerified = MutableLiveData<Boolean>()
    val isEmailVerified: LiveData<Boolean> = _isEmailVerified
    
    // 사용자 통계
    private val _userStats = MutableLiveData<Map<String, Int>>()
    val userStats: LiveData<Map<String, Int>> = _userStats
    
    init {
        // Repository 초기화
        val apiService = NetworkModule.getApiService()
        repository = AuthRepository(apiService, application)
        
        // 로그인 상태 확인
        checkLoginStatus()
    }
    
    // 로그인 상태 확인
    private fun checkLoginStatus() {
        val isLoggedIn = repository.isLoggedIn()
        _isLoggedIn.value = isLoggedIn
        
        if (isLoggedIn) {
            // 로그인된 상태라면 사용자 프로필 로드
            loadUserProfile()
        }
    }
    
    // 회원가입
    fun register(username: String, email: String, password: String) {
        if (!validateRegistrationInput(username, email, password)) {
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.register(username, email, password)
                
                result.onSuccess { authResponse ->
                    _authResponse.value = authResponse
                    _currentUser.value = authResponse.user
                    _isLoggedIn.value = true
                    _isEmailVerified.value = authResponse.user.isEmailVerified
                    _successMessage.value = "회원가입이 완료되었습니다"
                    _isLoading.value = false
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "회원가입 중 오류가 발생했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 로그인
    fun login(email: String, password: String) {
        if (!validateLoginInput(email, password)) {
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.login(email, password)
                
                result.onSuccess { authResponse ->
                    _authResponse.value = authResponse
                    _currentUser.value = authResponse.user
                    _isLoggedIn.value = true
                    _isEmailVerified.value = authResponse.user.isEmailVerified
                    _successMessage.value = "로그인이 완료되었습니다"
                    _isLoading.value = false
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "로그인 중 오류가 발생했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 이메일 인증
    fun verifyEmail(email: String, code: String) {
        if (email.isBlank() || code.isBlank()) {
            _errorMessage.value = "이메일과 인증 코드를 모두 입력해주세요"
            return
        }
        
        if (code.length != 6 || !code.all { it.isDigit() }) {
            _errorMessage.value = "인증 코드는 6자리 숫자여야 합니다"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.verifyEmail(email, code)
                
                result.onSuccess { message ->
                    _isEmailVerified.value = true
                    _successMessage.value = message
                    _isLoading.value = false
                    
                    // 사용자 프로필 새로고침
                    loadUserProfile()
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "이메일 인증 중 오류가 발생했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 토큰 갱신
    fun refreshToken() {
        viewModelScope.launch {
            try {
                val result = repository.refreshToken()
                
                result.onSuccess { authResponse ->
                    _authResponse.value = authResponse
                    _currentUser.value = authResponse.user
                    _successMessage.value = "토큰이 갱신되었습니다"
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    // 토큰 갱신 실패 시 로그아웃 처리
                    logout()
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "토큰 갱신 중 오류가 발생했습니다: ${e.message}"
                logout()
            }
        }
    }
    
    // 로그아웃
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.logout()
                
                result.onSuccess { message ->
                    clearUserData()
                    _successMessage.value = message
                    _isLoading.value = false
                }.onFailure { exception ->
                    // 로그아웃은 실패해도 로컬 데이터 삭제
                    clearUserData()
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                
            } catch (e: Exception) {
                clearUserData()
                _errorMessage.value = "로그아웃 중 오류가 발생했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 사용자 프로필 로드
    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val result = repository.getUserProfile()
                
                result.onSuccess { user ->
                    _currentUser.value = user
                    _isEmailVerified.value = user.isEmailVerified
                }.onFailure { exception ->
                    _errorMessage.value = "프로필 정보를 불러오는데 실패했습니다: ${exception.message}"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "프로필 로드 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }
    
    // 사용자 프로필 업데이트
    fun updateUserProfile(updates: Map<String, String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.updateUserProfile(updates)
                
                result.onSuccess { user ->
                    _currentUser.value = user
                    _successMessage.value = "프로필이 업데이트되었습니다"
                    _isLoading.value = false
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "프로필 업데이트 중 오류가 발생했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 계정 삭제
    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.deleteAccount()
                
                result.onSuccess { message ->
                    clearUserData()
                    _successMessage.value = message
                    _isLoading.value = false
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "계정 삭제 중 오류가 발생했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // 사용자 통계 로드
    fun loadUserStats() {
        viewModelScope.launch {
            try {
                val result = repository.getUserStats()
                
                result.onSuccess { stats ->
                    _userStats.value = stats
                }.onFailure { exception ->
                    _errorMessage.value = "통계 정보를 불러오는데 실패했습니다: ${exception.message}"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "통계 로드 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }
    
    // 회원가입 입력 검증
    private fun validateRegistrationInput(username: String, email: String, password: String): Boolean {
        return when {
            username.isBlank() -> {
                _errorMessage.value = "사용자명을 입력해주세요"
                false
            }
            username.length < 2 || username.length > 30 -> {
                _errorMessage.value = "사용자명은 2자 이상 30자 이하여야 합니다"
                false
            }
            email.isBlank() -> {
                _errorMessage.value = "이메일을 입력해주세요"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _errorMessage.value = "올바른 이메일 형식이 아닙니다"
                false
            }
            password.isBlank() -> {
                _errorMessage.value = "비밀번호를 입력해주세요"
                false
            }
            password.length < 6 -> {
                _errorMessage.value = "비밀번호는 최소 6자 이상이어야 합니다"
                false
            }
            !password.any { it.isLetter() } || !password.any { it.isDigit() } -> {
                _errorMessage.value = "비밀번호는 영문자와 숫자를 포함해야 합니다"
                false
            }
            else -> true
        }
    }
    
    // 로그인 입력 검증
    private fun validateLoginInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _errorMessage.value = "이메일을 입력해주세요"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _errorMessage.value = "올바른 이메일 형식이 아닙니다"
                false
            }
            password.isBlank() -> {
                _errorMessage.value = "비밀번호를 입력해주세요"
                false
            }
            else -> true
        }
    }
    
    // 사용자 데이터 초기화
    private fun clearUserData() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _authResponse.value = null
        _isEmailVerified.value = false
        _userStats.value = emptyMap()
    }
    
    // 에러 메시지 클리어
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    // 성공 메시지 클리어
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    // 현재 토큰 확인
    fun getCurrentToken(): String? {
        return repository.getToken()
    }
    
    // ViewModel이 클리어될 때 정리 작업
    override fun onCleared() {
        super.onCleared()
        clearErrorMessage()
        clearSuccessMessage()
    }
}