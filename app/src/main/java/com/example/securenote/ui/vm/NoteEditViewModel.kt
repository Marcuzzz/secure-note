package com.example.securenote.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.securenote.SecureNoteApp
import com.example.securenote.data.AppContainer
import com.example.securenote.data.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteEditState(
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val loading: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

class NoteEditViewModel(
    private val container: AppContainer,
    noteId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditState(id = noteId, loading = noteId != 0L))
    val state: StateFlow<NoteEditState> = _state.asStateFlow()

    init {
        if (noteId != 0L) {
            viewModelScope.launch {
                val n = container.session.repository().findById(noteId)
                if (n != null) {
                    _state.value = NoteEditState(
                        id = n.id,
                        title = n.title,
                        body = n.body,
                        username = n.username,
                        password = n.password,
                        url = n.url,
                        loading = false,
                    )
                } else {
                    _state.value = _state.value.copy(loading = false)
                }
            }
        }
    }

    fun update(block: NoteEditState.() -> NoteEditState) { _state.value = _state.value.block() }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            container.session.repository().save(
                Note(
                    id = s.id,
                    title = s.title.ifBlank { "Untitled" },
                    body = s.body,
                    username = s.username,
                    password = s.password,
                    url = s.url,
                )
            )
            _state.value = s.copy(saved = true)
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == 0L) { _state.value = s.copy(deleted = true); return }
        viewModelScope.launch {
            val n = container.session.repository().findById(s.id) ?: return@launch
            container.session.repository().delete(n)
            _state.value = s.copy(deleted = true)
        }
    }

    companion object {
        const val ARG_ID = "noteId"

        fun factory(noteId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SecureNoteApp
                return NoteEditViewModel(app.container, noteId) as T
            }
        }
    }
}
