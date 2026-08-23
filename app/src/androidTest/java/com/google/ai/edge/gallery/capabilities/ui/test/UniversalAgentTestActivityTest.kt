package com.google.ai.edge.gallery.capabilities.ui.test

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UniversalAgentTestActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<UniversalAgentTestActivity>()

    @Test
    fun testActivityDisplaysCoreElements() {
        composeTestRule.onNodeWithTag("name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("password_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submit_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("option_switch").assertIsDisplayed()
    }

    @Test
    fun testFormInteraction() {
        composeTestRule.onNodeWithTag("name_input").performTextInput("Bob")
        composeTestRule.onNodeWithTag("password_input").performTextInput("secret")
        composeTestRule.onNodeWithTag("submit_button").performClick()
    }
}
