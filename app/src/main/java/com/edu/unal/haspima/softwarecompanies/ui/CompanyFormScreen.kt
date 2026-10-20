package com.edu.unal.haspima.softwarecompanies.ui

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.edu.unal.haspima.softwarecompanies.R
import com.edu.unal.haspima.softwarecompanies.data.Classification
import com.edu.unal.haspima.softwarecompanies.data.Company

/** Creates a company when [company] is null, otherwise edits it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyFormScreen(company: Company?, onBack: () -> Unit, onSave: (Company) -> Unit) {
    var name by rememberSaveable { mutableStateOf(company?.name.orEmpty()) }
    var websiteUrl by rememberSaveable { mutableStateOf(company?.websiteUrl.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(company?.phone.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(company?.email.orEmpty()) }
    var productsAndServices by rememberSaveable { mutableStateOf(company?.productsAndServices.orEmpty()) }
    // Stored as enum names so the state survives process death.
    var classificationNames by rememberSaveable {
        mutableStateOf(company?.classifications?.map { it.name }.orEmpty())
    }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val classifications = Classification.entries.filter { it.name in classificationNames }.toSet()
    val errors = FormErrors(
        name = if (name.isBlank()) R.string.error_name_required else null,
        websiteUrl = if (websiteUrl.isNotBlank() && !Patterns.WEB_URL.matcher(websiteUrl.trim()).matches()) R.string.error_invalid_url else null,
        phone = if (phone.isNotBlank() && !Patterns.PHONE.matcher(phone.trim()).matches()) R.string.error_invalid_phone else null,
        email = if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) R.string.error_invalid_email else null,
        classification = if (classifications.isEmpty()) R.string.error_classification_required else null,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (company == null) R.string.title_new_company else R.string.title_edit_company)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormField(name, { name = it }, R.string.field_name, errors.name.takeIf { showErrors })
            FormField(websiteUrl, { websiteUrl = it }, R.string.field_website, errors.websiteUrl.takeIf { showErrors }, KeyboardType.Uri)
            FormField(phone, { phone = it }, R.string.field_phone, errors.phone.takeIf { showErrors }, KeyboardType.Phone)
            FormField(email, { email = it }, R.string.field_email, errors.email.takeIf { showErrors }, KeyboardType.Email)
            OutlinedTextField(
                value = productsAndServices,
                onValueChange = { productsAndServices = it },
                label = { Text(stringResource(R.string.field_products_and_services)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Text(stringResource(R.string.field_classification), style = MaterialTheme.typography.titleSmall)
            Classification.entries.forEach { classification ->
                val checked = classification in classifications
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = checked, role = Role.Checkbox, onValueChange = {
                            classificationNames =
                                if (it) classificationNames + classification.name else classificationNames - classification.name
                        }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text(stringResource(classification.labelRes), Modifier.padding(start = 8.dp))
                }
            }
            if (showErrors && errors.classification != null) {
                Text(stringResource(errors.classification), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    showErrors = true
                    if (!errors.any) {
                        onSave(
                            Company(
                                id = company?.id ?: 0,
                                name = name,
                                websiteUrl = websiteUrl,
                                phone = phone,
                                email = email,
                                productsAndServices = productsAndServices,
                                classifications = classifications,
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

private data class FormErrors(
    @StringRes val name: Int?,
    @StringRes val websiteUrl: Int?,
    @StringRes val phone: Int?,
    @StringRes val email: Int?,
    @StringRes val classification: Int?,
) {
    val any get() = listOf(name, websiteUrl, phone, email, classification).any { it != null }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    @StringRes error: Int?,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
