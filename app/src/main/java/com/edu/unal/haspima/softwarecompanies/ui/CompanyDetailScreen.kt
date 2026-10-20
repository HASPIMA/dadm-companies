package com.edu.unal.haspima.softwarecompanies.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edu.unal.haspima.softwarecompanies.R
import com.edu.unal.haspima.softwarecompanies.data.Company

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDetailScreen(
    company: Company,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    fun open(uri: String) = runCatching { uriHandler.openUri(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(company.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Field(R.string.field_website, company.websiteUrl) {
                val url = company.websiteUrl
                open(if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url")
            }
            Field(R.string.field_phone, company.phone) { open("tel:${company.phone}") }
            Field(R.string.field_email, company.email) { open("mailto:${company.email}") }
            Field(R.string.field_products_and_services, company.productsAndServices)

            Column {
                Text(stringResource(R.string.field_classification), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    company.classifications.sortedBy { it.ordinal }.forEach {
                        AssistChip(onClick = {}, label = { Text(stringResource(it.labelRes)) })
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        DeleteCompanyDialog(
            company = company,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Field(@StringRes label: Int, value: String, onClick: (() -> Unit)? = null) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        if (value.isBlank()) {
            Text(stringResource(R.string.not_provided), color = MaterialTheme.colorScheme.outline)
        } else if (onClick != null) {
            Text(
                value,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onClick),
            )
        } else {
            Text(value)
        }
    }
}
