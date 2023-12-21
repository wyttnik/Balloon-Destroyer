package com.example.shootthemall.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShootingViewModel: ViewModel() {

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score
    val _timeForCounter = MutableLiveData(0L)
    var timeForCounter: LiveData<Long> = _timeForCounter

    fun counter() {
        viewModelScope.launch(Dispatchers.Main) {
            _timeForCounter.value = 10000L
            while (_timeForCounter.value!! > 0) {
                delay(1000L)
                _timeForCounter.value = _timeForCounter.value!! - 1000
            }
            // timer finished
        }
    }

    fun increaseScore(value: Int) {
        _score.value = _score.value?.plus(value)
    }
}