package com.example.ai

sealed interface AiCoachUiState {
    data object Idle : AiCoachUiState
    data object Loading : AiCoachUiState
    data class Streaming(val text: String) : AiCoachUiState
    data class Success(val text: String) : AiCoachUiState
    data class Error(val message: String) : AiCoachUiState
}
