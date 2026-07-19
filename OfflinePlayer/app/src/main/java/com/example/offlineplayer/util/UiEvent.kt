package com.example.offlineplayer.util

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
}