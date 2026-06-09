package com.mae.aphorisms

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

class AphorismsViewModel(
    app: Application,
    private val state: SavedStateHandle
) : AndroidViewModel(app) {

    companion object {
        private const val KEY_LIST = "shuffled_list"
        private const val KEY_INDEX = "current_index"
        private const val TAG = "AphorismsMAE"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                val state = createSavedStateHandle()
                AphorismsViewModel(app, state)
            }
        }

        private val FALLBACK_APHORISMS = arrayListOf(
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

    private val _navigationEnabled = MutableStateFlow(true)
    val navigationEnabled: StateFlow<Boolean> = _navigationEnabled

    var isTransitioning = false
        private set

    private var shuffledList: ArrayList<String>
        get() = state[KEY_LIST] ?: ArrayList()
        set(v) { state[KEY_LIST] = v }

    private var currentIndex: Int
        get() = state[KEY_INDEX] ?: -1
        set(v) { state[KEY_INDEX] = v }

    init {
        if (shuffledList.isEmpty()) {
            load()
        } else {
            updateDisplay()
        }
    }

    private fun load() {
        val list = try {
            val json = getApplication<Application>().assets.open("aphorisms.json")
                .bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            ArrayList<String>().apply {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }.also { if (it.isEmpty()) throw IllegalStateException("empty") }
        } catch (e: Exception) {
            Log.w(TAG, "aphorisms.json unavailable, using fallback: ${e.message}")
            ArrayList(FALLBACK_APHORISMS)
        }

        shuffledList = fisherYates(list)
        currentIndex = 0
        _navigationEnabled.value = list.size >= 2
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

    private fun fisherYates(input: ArrayList<String>): ArrayList<String> {
        val list = ArrayList(input)
        for (i in list.size - 1 downTo 1) {
            val j = (Math.random() * (i + 1)).toInt()
            val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        }
        return list
    }
}
