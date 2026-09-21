package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TOOLSUGGESTOR", appName)
  }

  @Test
  fun `paid code filter returns powerful code generation tools`() {
    val catalog = com.example.data.MarketToolCatalog.marketTools
    val paidCodeTools = catalog.filter {
      it.category == com.example.model.ToolCategory.CODE && it.pricing == com.example.model.PricingType.PAID
    }

    // Verify multiple high-efficiency paid code generation tools are present
    org.junit.Assert.assertTrue("Should have multiple paid code tools", paidCodeTools.size >= 5)
    val names = paidCodeTools.map { it.name }
    org.junit.Assert.assertTrue(names.any { it.contains("Ox Alpha", ignoreCase = true) || it.contains("0xAlpha", ignoreCase = true) })
    org.junit.Assert.assertTrue(names.any { it.contains("Devin AI", ignoreCase = true) })
    org.junit.Assert.assertTrue(names.any { it.contains("Bolt.new", ignoreCase = true) })
    org.junit.Assert.assertTrue(names.any { it.contains("Cursor Pro", ignoreCase = true) })
    org.junit.Assert.assertTrue(names.any { it.contains("Claude Code", ignoreCase = true) })
    org.junit.Assert.assertTrue(names.any { it.contains("Augment Code", ignoreCase = true) })
    org.junit.Assert.assertTrue(names.any { it.contains("Replit Agent", ignoreCase = true) })
    org.junit.Assert.assertTrue(names.any { it.contains("Qodo Gen", ignoreCase = true) })
  }

  @Test
  fun `smart synthesis includes Ox Alpha and live analysis metadata`() {
    val service = com.example.data.GeminiService()
    val analysis = service.synthesizeSmartAnalysis("An automated hospital telemedicine queue system", null)
    val codeTools = analysis.tools.filter { it.category == com.example.model.ToolCategory.CODE }
    val paidCode = codeTools.filter { it.pricing == com.example.model.PricingType.PAID }
    
    org.junit.Assert.assertTrue(paidCode.any { it.name.contains("Ox Alpha") })
    org.junit.Assert.assertTrue(analysis.isLiveInternetAnalyzed)
  }
}
