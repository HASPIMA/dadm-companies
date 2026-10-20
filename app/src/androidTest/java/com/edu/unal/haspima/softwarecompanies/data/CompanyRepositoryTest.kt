package com.edu.unal.haspima.softwarecompanies.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompanyRepositoryTest {

    private lateinit var dbHelper: CompanyDbHelper
    private lateinit var repository: CompanyRepository

    @Before
    fun setUp() {
        // A null name gives an in-memory database.
        dbHelper = CompanyDbHelper(InstrumentationRegistry.getInstrumentation().targetContext, name = null)
        repository = CompanyRepository(dbHelper)
    }

    @After
    fun tearDown() = dbHelper.close()

    @Test
    fun createAndFindById() {
        val id = repository.create(company("Acme", Classification.CONSULTING, Classification.SOFTWARE_FACTORY))

        val found = repository.findById(id)!!

        assertEquals("Acme", found.name)
        assertEquals(setOf(Classification.CONSULTING, Classification.SOFTWARE_FACTORY), found.classifications)
    }

    @Test
    fun updateReplacesFieldsAndClassifications() {
        val id = repository.create(company("Acme", Classification.CONSULTING))

        repository.update(company("Acme Labs", Classification.CUSTOM_DEVELOPMENT).copy(id = id, email = "hi@acme.co"))

        val found = repository.findById(id)!!
        assertEquals("Acme Labs", found.name)
        assertEquals("hi@acme.co", found.email)
        assertEquals(setOf(Classification.CUSTOM_DEVELOPMENT), found.classifications)
    }

    @Test
    fun deleteRemovesCompanyAndItsClassifications() {
        val id = repository.create(company("Acme", Classification.CONSULTING))

        assertTrue(repository.delete(id))

        assertNull(repository.findById(id))
        dbHelper.readableDatabase.rawQuery("SELECT COUNT(*) FROM company_classifications", null).use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun searchFiltersByNameAndClassification() {
        repository.create(company("Acme Consulting", Classification.CONSULTING))
        repository.create(company("Acme Factory", Classification.SOFTWARE_FACTORY))
        repository.create(company("Globex", Classification.CONSULTING, Classification.CUSTOM_DEVELOPMENT))

        assertEquals(listOf("Acme Consulting", "Acme Factory", "Globex"), repository.search().names())
        assertEquals(listOf("Acme Consulting", "Acme Factory"), repository.search("acme").names())
        assertEquals(
            listOf("Acme Consulting", "Globex"),
            repository.search(classifications = setOf(Classification.CONSULTING)).names(),
        )
        assertEquals(
            listOf("Acme Consulting"),
            repository.search("acme", setOf(Classification.CONSULTING)).names(),
        )
        assertEquals(
            listOf("Acme Factory", "Globex"),
            repository.search(classifications = setOf(Classification.SOFTWARE_FACTORY, Classification.CUSTOM_DEVELOPMENT)).names(),
        )
    }

    @Test
    fun searchTreatsLikeWildcardsLiterally() {
        repository.create(company("100% Code", Classification.CONSULTING))
        repository.create(company("1000 Devs", Classification.CONSULTING))

        assertEquals(listOf("100% Code"), repository.search("0%").names())
    }

    private fun List<Company>.names() = map { it.name }

    private fun company(name: String, vararg classifications: Classification) = Company(
        name = name,
        websiteUrl = "https://example.com",
        phone = "+57 300 000 0000",
        email = "contact@example.com",
        productsAndServices = "Software",
        classifications = classifications.toSet(),
    )
}
