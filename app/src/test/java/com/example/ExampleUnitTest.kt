package com.example

import com.example.data.GeminiService
import com.example.data.MarketToolCatalog
import com.example.model.PricingType
import com.example.model.ToolCategory
import com.example.model.ToolSortOrder
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun marketCatalog_coversAlphabetAtoZ() {
        val tools = MarketToolCatalog.marketTools
        assertTrue("Catalog should have over 50 tools", tools.size >= 50)

        // Verify that initial letters cover A to Z
        val letters = tools.map { it.initialLetter }.toSet()
        ('A'..'Z').forEach { char ->
            assertTrue("Catalog should contain tools starting with '$char'", letters.contains(char))
        }

        // Verify pricing presence: both UNPAID and PAID
        val unpaidCount = tools.count { it.pricing == PricingType.UNPAID }
        val paidCount = tools.count { it.pricing == PricingType.PAID }
        assertTrue("Catalog should have unpaid tools", unpaidCount > 10)
        assertTrue("Catalog should have paid tools", paidCount > 5)

        // Verify categories coverage
        ToolCategory.values().filter { it != ToolCategory.ALL }.forEach { category ->
            val count = tools.count { it.category == category }
            assertTrue("Catalog should have tools for category $category", count > 0)
        }
    }

    @Test
    fun marketCatalog_containsLatestReleasedTools() {
        val tools = MarketToolCatalog.marketTools
        val toolIds = tools.map { it.id }.toSet()
        assertTrue("Catalog must contain GPT-6 Astra", toolIds.contains("gpt_6_astra"))
        assertTrue("Catalog must contain Claude 3.7 Sonnet", toolIds.contains("claude_3_7_sonnet"))
        assertTrue("Catalog must contain Kimi AI", toolIds.contains("kimi_ai"))
    }

    @Test
    fun smartAnalysis_returnsComprehensiveToolsWithTailoredContext() {
        val service = GeminiService()
        val analysis = service.synthesizeSmartAnalysis("AI Doctor symptom diagnosis mobile application", null)

        assertNotNull(analysis)
        assertTrue("Should return extensive tool list", analysis.tools.size >= 40)
        assertTrue("Title should be extracted", analysis.projectTitle.isNotBlank())
        
        // Check that mobile and AI tools have elevated scores
        val topToolIds = analysis.tools.take(10).map { it.id }
        assertTrue("Should prioritize mobile or AI tools", topToolIds.any { it == "flutter" || it == "gemini_flash" || it == "fastapi" || it == "firebase_free" })
    }
}

