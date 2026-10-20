package com.edu.unal.haspima.softwarecompanies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.edu.unal.haspima.softwarecompanies.ui.CompaniesApp
import com.edu.unal.haspima.softwarecompanies.ui.theme.SoftwareCompaniesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoftwareCompaniesTheme {
                CompaniesApp()
            }
        }
    }
}
