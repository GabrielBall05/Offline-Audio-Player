package com.example.offlineplayer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    protected val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    protected fun launchWithLoading(block: suspend () -> Unit) {
        _isLoading.value = true //Show loading screen
        viewModelScope.launch(Dispatchers.IO) {
            try { block() } //Perform given operation
            catch (e: Exception) { e.printStackTrace() }
            finally { _isLoading.value = false } //Hide loading screen
        }
    }

    protected fun launchWithoutLoading(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try { block() } //Perform given operation
            catch (e: Exception) { e.printStackTrace() }
        }
    }
}