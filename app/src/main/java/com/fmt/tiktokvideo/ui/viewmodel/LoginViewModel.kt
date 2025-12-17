package com.fmt.tiktokvideo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fmt.tiktokvideo.http.ApiService
import com.fmt.tiktokvideo.http.URLs
import com.fmt.tiktokvideo.model.LoginResult
import com.fmt.tiktokvideo.model.dto.LoginParam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginState())
    val uiState = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching {
                val loginResult =
                    ApiService.get().login(URLs.LOGIN_URL, LoginParam(username, password))
                if (loginResult.success) {
                    _uiState.value = LoginState(loginResult = loginResult.data)
                } else {
                    _uiState.value =
                        _uiState.value.copy(isLoading = false, error = loginResult.message)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }
}

data class LoginState(
    val loginResult: LoginResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)