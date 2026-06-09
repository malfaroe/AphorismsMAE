package com.mae.aphorisms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AphorismsViewModel(aphorisms: List<String>) : ViewModel() {

    companion object {
        val FALLBACK_APHORISMS = listOf(
            "A beginning is invisible.",
            "A decision changes the world.",
            "Attention is the prime tool of any line of craft.",
            "Begin with the possible and move gradually towards the impossible.",
            "Craft is a universal language.",
            "Discipline is a vehicle for joy.",
            "Everything we are is revealed in our playing.",
            "Gradual transitions take place suddenly.",
            "How we hold our pick is how we live our life.",
            "In tuning a note we are tuning ourselves.",
            "It's the recovery that matters.",
            "Make better mistakes.",
            "Music is Silence, singing.",
            "Nothing worthwhile is achieved suddenly.",
            "One note, struck truly, is a symphony.",
            "Quality spreads.",
            "Remember to play.",
            "The Key To It All: the quality of our attention.",
            "Trust the process.",
            "Honour sufficiency."
        )
    }

    private val _current = MutableStateFlow("")
    val current: StateFlow<String> = _current

    private val _counter = MutableStateFlow("")
    val counter: StateFlow<String> = _counter

    private val _navigationEnabled = MutableStateFlow(aphorisms.size >= 2)
    val navigationEnabled: StateFlow<Boolean> = _navigationEnabled

    var isTransitioning = false
        private set

    private var shuffledList: List<String> = fisherYates(aphorisms)
    private var currentIndex: Int = 0

    init {
        updateDisplay()
    }

    fun advance(delta: Int) {
        if (isTransitioning) return
        val list = shuffledList
        if (list.size < 2) return

        isTransitioning = true
        viewModelScope.launch {
            var idx = currentIndex + delta
            if (idx >= list.size) {
                shuffledList = fisherYates(list)
                idx = 0
            } else if (idx < 0) {
                idx = list.size - 1
            }
            currentIndex = idx
            updateDisplay()
            delay(300)
            isTransitioning = false
        }
    }

    private fun updateDisplay() {
        val list = shuffledList
        if (list.isEmpty()) {
            _current.value = "Honour necessity. Honour sufficiency."
            _counter.value = ""
            _navigationEnabled.value = false
            return
        }
        val idx = currentIndex.coerceIn(0, list.size - 1)
        _current.value = list[idx]
        _counter.value = if (list.size >= 2) "${idx + 1} / ${list.size}" else ""
        _navigationEnabled.value = list.size >= 2
    }

    private fun fisherYates(input: List<String>): List<String> {
        val list = input.toMutableList()
        for (i in list.size - 1 downTo 1) {
            val j = (Math.random() * (i + 1)).toInt()
            val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        }
        return list
    }
}
