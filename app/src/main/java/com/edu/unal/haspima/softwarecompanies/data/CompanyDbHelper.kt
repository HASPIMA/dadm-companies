package com.edu.unal.haspima.softwarecompanies.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Schema:
 *
 *   companies(id PK, name, website_url, phone, email, products_and_services)
 *   company_classifications(company_id FK -> companies.id, classification)
 *
 * Classifications live in their own table because a company can have more than one.
 */
class CompanyDbHelper(context: Context, name: String? = DATABASE_NAME) :
    SQLiteOpenHelper(context, name, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${Companies.TABLE} (
                ${Companies.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${Companies.NAME} TEXT NOT NULL,
                ${Companies.WEBSITE_URL} TEXT NOT NULL DEFAULT '',
                ${Companies.PHONE} TEXT NOT NULL DEFAULT '',
                ${Companies.EMAIL} TEXT NOT NULL DEFAULT '',
                ${Companies.PRODUCTS_AND_SERVICES} TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE ${CompanyClassifications.TABLE} (
                ${CompanyClassifications.COMPANY_ID} INTEGER NOT NULL
                    REFERENCES ${Companies.TABLE}(${Companies.ID}) ON DELETE CASCADE,
                ${CompanyClassifications.CLASSIFICATION} TEXT NOT NULL,
                PRIMARY KEY (${CompanyClassifications.COMPANY_ID}, ${CompanyClassifications.CLASSIFICATION})
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_companies_name ON ${Companies.TABLE}(${Companies.NAME})")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${CompanyClassifications.TABLE}")
        db.execSQL("DROP TABLE IF EXISTS ${Companies.TABLE}")
        onCreate(db)
    }

    object Companies {
        const val TABLE = "companies"
        const val ID = "id"
        const val NAME = "name"
        const val WEBSITE_URL = "website_url"
        const val PHONE = "phone"
        const val EMAIL = "email"
        const val PRODUCTS_AND_SERVICES = "products_and_services"
    }

    object CompanyClassifications {
        const val TABLE = "company_classifications"
        const val COMPANY_ID = "company_id"
        const val CLASSIFICATION = "classification"
    }

    companion object {
        const val DATABASE_NAME = "companies.db"
        const val DATABASE_VERSION = 1
    }
}
