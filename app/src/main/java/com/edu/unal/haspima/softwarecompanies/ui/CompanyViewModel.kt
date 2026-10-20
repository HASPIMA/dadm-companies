package com.edu.unal.haspima.softwarecompanies.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edu.unal.haspima.softwarecompanies.R
import com.edu.unal.haspima.softwarecompanies.data.Classification
import com.edu.unal.haspima.softwarecompanies.data.Company
import com.edu.unal.haspima.softwarecompanies.data.CompanyDbHelper
import com.edu.unal.haspima.softwarecompanies.data.CompanyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object List : Screen
    data class Detail(val company: Company) : Screen
    /** [company] is null when creating a new one. */
    data class Form(val company: Company?) : Screen
}

class CompanyViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = CompanyDbHelper(application)
    private val repository = CompanyRepository(dbHelper)

    private val _backStack = MutableStateFlow<List<Screen>>(listOf(Screen.List))
    val screen: StateFlow<Screen> =
        _backStack.map { it.last() }.stateIn(viewModelScope, SharingStarted.Eagerly, Screen.List)

    private val _nameQuery = MutableStateFlow("")
    val nameQuery: StateFlow<String> = _nameQuery.asStateFlow()

    private val _classificationFilter = MutableStateFlow<Set<Classification>>(emptySet())
    val classificationFilter: StateFlow<Set<Classification>> = _classificationFilter.asStateFlow()

    /** Bumped after every write so the list query re-runs. */
    private val dataVersion = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val companies: StateFlow<List<Company>?> =
        combine(_nameQuery, _classificationFilter, dataVersion) { query, filter, _ -> query to filter }
            .mapLatest { (query, filter) -> repository.search(query, filter) }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = Channel<Int>(Channel.BUFFERED)
    /** One-off user messages (string resource ids) for a snackbar. */
    val messages = _messages.receiveAsFlow()

    fun onNameQueryChange(query: String) {
        _nameQuery.value = query
    }

    fun toggleClassificationFilter(classification: Classification) {
        _classificationFilter.update { if (classification in it) it - classification else it + classification }
    }

    fun clearFilters() {
        _nameQuery.value = ""
        _classificationFilter.value = emptySet()
    }

    fun openDetail(company: Company) = push(Screen.Detail(company))

    fun openCreate() = push(Screen.Form(null))

    fun openEdit(company: Company) = push(Screen.Form(company))

    /** Returns false when already at the root, so the caller can let the system handle back. */
    fun navigateBack(): Boolean {
        if (_backStack.value.size <= 1) return false
        _backStack.update { it.dropLast(1) }
        return true
    }

    fun save(company: Company) {
        viewModelScope.launch {
            val isNew = company.id == 0L
            val saved = withContext(Dispatchers.IO) {
                if (isNew) {
                    company.copy(id = repository.create(company))
                } else {
                    repository.update(company)
                    repository.findById(company.id) ?: company
                }
            }
            dataVersion.update { it + 1 }
            // Replace the form (and the stale detail, when editing) with the fresh detail.
            _backStack.update { stack ->
                val withoutForm = stack.dropLast(1)
                val base = if (!isNew && withoutForm.lastOrNull() is Screen.Detail) withoutForm.dropLast(1) else withoutForm
                base + Screen.Detail(saved)
            }
            send(if (isNew) R.string.message_company_created else R.string.message_company_updated)
        }
    }

    fun delete(company: Company) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(company.id) }
            dataVersion.update { it + 1 }
            _backStack.value = listOf(Screen.List)
            send(R.string.message_company_deleted)
        }
    }

    private fun push(screen: Screen) = _backStack.update { it + screen }

    private fun send(@StringRes message: Int) {
        _messages.trySend(message)
    }

    override fun onCleared() {
        dbHelper.close()
    }
}
