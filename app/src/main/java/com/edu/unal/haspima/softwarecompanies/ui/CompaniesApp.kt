package com.edu.unal.haspima.softwarecompanies.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CompaniesApp(viewModel: CompanyViewModel = viewModel()) {
    val screen by viewModel.screen.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val nameQuery by viewModel.nameQuery.collectAsState()
    val classificationFilter by viewModel.classificationFilter.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    BackHandler(enabled = screen != Screen.List) { viewModel.navigateBack() }

    when (val current = screen) {
        Screen.List -> CompanyListScreen(
            companies = companies,
            nameQuery = nameQuery,
            classificationFilter = classificationFilter,
            snackbarHostState = snackbarHostState,
            onNameQueryChange = viewModel::onNameQueryChange,
            onToggleClassification = viewModel::toggleClassificationFilter,
            onClearFilters = viewModel::clearFilters,
            onCompanyClick = viewModel::openDetail,
            onAddClick = viewModel::openCreate,
            onDelete = viewModel::delete,
        )
        is Screen.Detail -> CompanyDetailScreen(
            company = current.company,
            snackbarHostState = snackbarHostState,
            onBack = { viewModel.navigateBack() },
            onEdit = { viewModel.openEdit(current.company) },
            onDelete = { viewModel.delete(current.company) },
        )
        is Screen.Form -> CompanyFormScreen(
            company = current.company,
            onBack = { viewModel.navigateBack() },
            onSave = viewModel::save,
        )
    }
}
