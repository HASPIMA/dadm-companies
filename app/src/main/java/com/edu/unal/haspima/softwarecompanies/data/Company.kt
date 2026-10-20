package com.edu.unal.haspima.softwarecompanies.data

data class Company(
    val id: Long = 0,
    val name: String,
    val websiteUrl: String,
    val phone: String,
    val email: String,
    val productsAndServices: String,
    val classifications: Set<Classification>,
)
