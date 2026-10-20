package com.edu.unal.haspima.softwarecompanies.ui

import androidx.annotation.StringRes
import com.edu.unal.haspima.softwarecompanies.R
import com.edu.unal.haspima.softwarecompanies.data.Classification

@get:StringRes
val Classification.labelRes: Int
    get() = when (this) {
        Classification.CONSULTING -> R.string.classification_consulting
        Classification.CUSTOM_DEVELOPMENT -> R.string.classification_custom_development
        Classification.SOFTWARE_FACTORY -> R.string.classification_software_factory
    }
