package com.edu.unal.haspima.softwarecompanies.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edu.unal.haspima.softwarecompanies.R
import com.edu.unal.haspima.softwarecompanies.data.Classification
import com.edu.unal.haspima.softwarecompanies.data.Company

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyListScreen(
    companies: List<Company>?,
    nameQuery: String,
    classificationFilter: Set<Classification>,
    snackbarHostState: SnackbarHostState,
    onNameQueryChange: (String) -> Unit,
    onToggleClassification: (Classification) -> Unit,
    onClearFilters: () -> Unit,
    onCompanyClick: (Company) -> Unit,
    onAddClick: () -> Unit,
    onDelete: (Company) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Company?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_company))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Filters(nameQuery, classificationFilter, onNameQueryChange, onToggleClassification)

            when {
                companies == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                companies.isEmpty() -> EmptyState(
                    isFiltering = nameQuery.isNotBlank() || classificationFilter.isNotEmpty(),
                    onClearFilters = onClearFilters,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(companies, key = { it.id }) { company ->
                        CompanyCard(
                            company = company,
                            onClick = { onCompanyClick(company) },
                            onDeleteClick = { pendingDelete = company },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { company ->
        DeleteCompanyDialog(
            company = company,
            onConfirm = {
                pendingDelete = null
                onDelete(company)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun Filters(
    nameQuery: String,
    classificationFilter: Set<Classification>,
    onNameQueryChange: (String) -> Unit,
    onToggleClassification: (Classification) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = nameQuery,
            onValueChange = onNameQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_by_name)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (nameQuery.isNotEmpty()) {
                    IconButton(onClick = { onNameQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                    }
                }
            },
            singleLine = true,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Classification.entries.forEach { classification ->
                FilterChip(
                    selected = classification in classificationFilter,
                    onClick = { onToggleClassification(classification) },
                    label = { Text(stringResource(classification.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun CompanyCard(company: Company, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(company.name, style = MaterialTheme.typography.titleMedium)
                if (company.productsAndServices.isNotBlank()) {
                    Text(
                        company.productsAndServices,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    company.classifications.sortedBy { it.ordinal }.forEach {
                        SuggestionChip(onClick = onClick, label = { Text(stringResource(it.labelRes)) })
                    }
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun EmptyState(isFiltering: Boolean, onClearFilters: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(if (isFiltering) R.string.empty_no_matches else R.string.empty_no_companies),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (isFiltering) {
            TextButton(onClick = onClearFilters) { Text(stringResource(R.string.action_clear_filters)) }
        }
    }
}
