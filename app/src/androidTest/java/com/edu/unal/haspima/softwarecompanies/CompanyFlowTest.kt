package com.edu.unal.haspima.softwarecompanies

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.haspima.softwarecompanies.data.CompanyDbHelper
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class CompanyFlowTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FreshDatabaseRule() as TestRule?).around(composeRule)

    private fun string(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    private fun field(label: Int) = composeRule.onNode(hasSetTextAction() and hasText(string(label)))

    @Test
    fun createFilterAndDeleteCompany() {
        composeRule.onNodeWithContentDescription(string(R.string.action_add_company)).performClick()

        // Saving without required data shows validation errors.
        composeRule.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.error_name_required)).performScrollTo().assertIsDisplayed()

        field(R.string.field_name).performTextInput("Acme Software")
        field(R.string.field_website).performTextInput("acme.co")
        field(R.string.field_email).performTextInput("info@acme.co")
        composeRule.onNodeWithText(string(R.string.classification_software_factory)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()

        // Lands on the detail screen.
        composeRule.onNodeWithText("info@acme.co").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.action_back)).performClick()

        // Listed, and survives a matching filter but not a non-matching one.
        composeRule.onNodeWithText("Acme Software").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.classification_consulting)).performClick()
        composeRule.onNodeWithText(string(R.string.empty_no_matches)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_clear_filters)).performClick()
        composeRule.onNodeWithText(string(R.string.search_by_name)).performTextInput("acme")
        composeRule.onNodeWithText("Acme Software").assertIsDisplayed()

        // Delete asks for confirmation; cancelling keeps the company.
        composeRule.onAllNodesWithContentDescription(string(R.string.action_delete))[0].performClick()
        composeRule.onNodeWithText(string(R.string.delete_dialog_message, "Acme Software")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_cancel)).performClick()
        composeRule.onNodeWithText("Acme Software").assertIsDisplayed()

        composeRule.onAllNodesWithContentDescription(string(R.string.action_delete))[0].performClick()
        composeRule.onNodeWithText(string(R.string.action_delete)).performClick()
        composeRule.onNodeWithText(string(R.string.empty_no_matches)).assertIsDisplayed()
    }

    private class FreshDatabaseRule : TestRule {
        override fun apply(base: Statement, description: Description) = object : Statement() {
            override fun evaluate() {
                InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(CompanyDbHelper.DATABASE_NAME)
                base.evaluate()
            }
        }
    }
}
