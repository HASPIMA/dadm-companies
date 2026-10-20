package com.edu.unal.haspima.softwarecompanies.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.edu.unal.haspima.softwarecompanies.data.CompanyDbHelper.Companies
import com.edu.unal.haspima.softwarecompanies.data.CompanyDbHelper.CompanyClassifications

/** CRUD and search over the companies' directory. Calls block; run them off the main thread. */
class CompanyRepository(private val dbHelper: CompanyDbHelper) {

    fun create(company: Company): Long {
        val db = dbHelper.writableDatabase
        return db.inTransaction {
            val id = insertOrThrow(Companies.TABLE, null, company.toContentValues())
            insertClassifications(id, company.classifications)
            id
        }
    }

    fun update(company: Company): Boolean {
        val db = dbHelper.writableDatabase
        return db.inTransaction {
            val rows = update(
                Companies.TABLE,
                company.toContentValues(),
                "${Companies.ID} = ?",
                arrayOf(company.id.toString()),
            )
            if (rows > 0) {
                delete(
                    CompanyClassifications.TABLE,
                    "${CompanyClassifications.COMPANY_ID} = ?",
                    arrayOf(company.id.toString()),
                )
                insertClassifications(company.id, company.classifications)
            }
            rows > 0
        }
    }

    /** Classifications are removed by the ON DELETE CASCADE foreign key. */
    fun delete(id: Long): Boolean =
        dbHelper.writableDatabase.delete(
            Companies.TABLE,
            "${Companies.ID} = ?",
            arrayOf(id.toString()),
        ) > 0

    fun findById(id: Long): Company? =
        query("WHERE c.${Companies.ID} = ?", arrayOf(id.toString())).firstOrNull()

    /**
     * Lists companies ordered by name.
     *
     * @param nameQuery case-insensitive substring of the company name; blank matches all.
     * @param classifications keeps companies having at least one of these; empty matches all.
     */
    fun search(nameQuery: String = "", classifications: Set<Classification> = emptySet()): List<Company> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        val trimmed = nameQuery.trim()
        if (trimmed.isNotEmpty()) {
            conditions += "c.${Companies.NAME} LIKE ? ESCAPE '\\'"
            args += "%${escapeLike(trimmed)}%"
        }
        if (classifications.isNotEmpty()) {
            val placeholders = classifications.joinToString(",") { "?" }
            conditions += """
                EXISTS (
                    SELECT 1 FROM ${CompanyClassifications.TABLE} f
                    WHERE f.${CompanyClassifications.COMPANY_ID} = c.${Companies.ID}
                      AND f.${CompanyClassifications.CLASSIFICATION} IN ($placeholders)
                )
            """.trimIndent()
            args += classifications.map { it.name }
        }

        val where = if (conditions.isEmpty()) "" else "WHERE " + conditions.joinToString(" AND ")
        return query(where, args.toTypedArray())
    }

    private fun query(where: String, args: Array<String>): List<Company> {
        val sql = """
            SELECT c.*,
                   (SELECT GROUP_CONCAT(cc.${CompanyClassifications.CLASSIFICATION})
                      FROM ${CompanyClassifications.TABLE} cc
                     WHERE cc.${CompanyClassifications.COMPANY_ID} = c.${Companies.ID}) AS $CLASSIFICATIONS_ALIAS
              FROM ${Companies.TABLE} c
              $where
             ORDER BY c.${Companies.NAME} COLLATE NOCASE
        """.trimIndent()
        return dbHelper.readableDatabase.rawQuery(sql, args).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toCompany()) }
        }
    }

    private fun SQLiteDatabase.insertClassifications(companyId: Long, classifications: Set<Classification>) {
        for (classification in classifications) {
            insertOrThrow(
                CompanyClassifications.TABLE,
                null,
                ContentValues().apply {
                    put(CompanyClassifications.COMPANY_ID, companyId)
                    put(CompanyClassifications.CLASSIFICATION, classification.name)
                },
            )
        }
    }

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        try {
            val result = block()
            setTransactionSuccessful()
            return result
        } finally {
            endTransaction()
        }
    }

    private fun Company.toContentValues() = ContentValues().apply {
        put(Companies.NAME, name.trim())
        put(Companies.WEBSITE_URL, websiteUrl.trim())
        put(Companies.PHONE, phone.trim())
        put(Companies.EMAIL, email.trim())
        put(Companies.PRODUCTS_AND_SERVICES, productsAndServices.trim())
    }

    private fun Cursor.toCompany() = Company(
        id = getLong(getColumnIndexOrThrow(Companies.ID)),
        name = getString(getColumnIndexOrThrow(Companies.NAME)),
        websiteUrl = getString(getColumnIndexOrThrow(Companies.WEBSITE_URL)),
        phone = getString(getColumnIndexOrThrow(Companies.PHONE)),
        email = getString(getColumnIndexOrThrow(Companies.EMAIL)),
        productsAndServices = getString(getColumnIndexOrThrow(Companies.PRODUCTS_AND_SERVICES)),
        classifications = getString(getColumnIndexOrThrow(CLASSIFICATIONS_ALIAS))
            ?.split(',')
            ?.mapNotNull { value -> Classification.entries.find { it.name == value } }
            ?.toSet()
            .orEmpty(),
    )

    private fun escapeLike(value: String) =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private companion object {
        const val CLASSIFICATIONS_ALIAS = "classifications"
    }
}
