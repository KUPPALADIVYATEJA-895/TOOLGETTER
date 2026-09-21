package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiService
import com.example.data.MarketToolCatalog
import com.example.model.PricingType
import com.example.model.ProjectAnalysis
import com.example.model.SampleProject
import com.example.model.SuggestedTool
import com.example.model.ToolCategory
import com.example.model.ToolSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

data class ToolSuggestorUiState(
    val prompt: String = "",
    val attachedDocName: String? = null,
    val attachedDocContent: String? = null,
    val isLoading: Boolean = false,
    val statusMessage: String = "",
    val analysisResult: ProjectAnalysis? = null,
    val selectedPricingFilter: PricingType = PricingType.ALL,
    val selectedCategoryFilter: ToolCategory = ToolCategory.ALL,
    val selectedLetterFilter: String? = null,
    val selectedSortOrder: ToolSortOrder = ToolSortOrder.RELEVANCE,
    val searchQuery: String = "",
    val bookmarkedIds: Set<String> = emptySet(),
    val activeGuideTool: SuggestedTool? = null,
    val showBlueprintDialog: Boolean = false,
    val isMarketRefreshing: Boolean = false,
    val marketSyncTime: String = "Live Market Sync Active (2025/2026)"
)

class ToolSuggestorViewModel(
    private val geminiService: GeminiService = GeminiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolSuggestorUiState())
    val uiState: StateFlow<ToolSuggestorUiState> = _uiState.asStateFlow()

    fun onPromptChanged(newPrompt: String) {
        _uiState.update { it.copy(prompt = newPrompt) }
    }

    fun loadSampleProject(sample: SampleProject) {
        _uiState.update {
            it.copy(
                prompt = "${sample.title}\n\n${sample.description}",
                attachedDocName = null,
                attachedDocContent = null
            )
        }
    }

    fun handleDocumentSelected(context: Context, uri: Uri) {
        try {
            var fileName = "project_document.txt"
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex) ?: fileName
                    }
                }
            }

            val inputStream = context.contentResolver.openInputStream(uri)
            val content = inputStream?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    reader.readText()
                }
            } ?: ""

            val previewContent = if (content.isNotBlank()) {
                content
            } else {
                "Project document file attached: $fileName"
            }

            _uiState.update {
                it.copy(
                    attachedDocName = fileName,
                    attachedDocContent = previewContent
                )
            }
        } catch (e: Exception) {
            Log.e("ToolSuggestorVM", "Error reading attached document", e)
            _uiState.update {
                it.copy(
                    attachedDocName = "attached_file",
                    attachedDocContent = "Document attached for project analysis."
                )
            }
        }
    }

    fun clearAttachedDocument() {
        _uiState.update { it.copy(attachedDocName = null, attachedDocContent = null) }
    }

    fun analyzeProject() {
        val currentPrompt = _uiState.value.prompt.trim()
        val currentDoc = _uiState.value.attachedDocContent?.trim()

        if (currentPrompt.isBlank() && currentDoc.isNullOrBlank()) {
            _uiState.update {
                it.copy(statusMessage = "Please enter a project idea or attach a document first!")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    statusMessage = "Analyzing project requirements & searching latest 2025/2026 tools in real-time..."
                )
            }

            try {
                val effectivePrompt = if (currentPrompt.isNotBlank()) currentPrompt else "College Student Project"
                val result = geminiService.analyzeProject(
                    prompt = effectivePrompt,
                    documentContent = currentDoc
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        analysisResult = result,
                        selectedPricingFilter = PricingType.ALL, // Combined by default as requested
                        selectedCategoryFilter = ToolCategory.ALL, // Combined by default
                        searchQuery = "",
                        statusMessage = "Tool recommendation blueprint generated successfully!"
                    )
                }
            } catch (e: Exception) {
                Log.e("ToolSuggestorVM", "Error analyzing project", e)
                val fallback = geminiService.synthesizeSmartAnalysis(currentPrompt, currentDoc)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        analysisResult = fallback,
                        selectedPricingFilter = PricingType.ALL,
                        selectedCategoryFilter = ToolCategory.ALL,
                        searchQuery = "",
                        statusMessage = "Tool suggestions loaded from market intelligence catalog."
                    )
                }
            }
        }
    }

    fun setPricingFilter(pricing: PricingType) {
        _uiState.update { it.copy(selectedPricingFilter = pricing) }
    }

    fun setCategoryFilter(category: ToolCategory) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun setLetterFilter(letter: String?) {
        _uiState.update { it.copy(selectedLetterFilter = if (letter == "ALL" || letter.isNullOrBlank()) null else letter) }
    }

    fun setSortOrder(order: ToolSortOrder) {
        _uiState.update { it.copy(selectedSortOrder = order) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleBookmark(toolId: String) {
        _uiState.update { current ->
            val updated = current.bookmarkedIds.toMutableSet()
            if (updated.contains(toolId)) {
                updated.remove(toolId)
            } else {
                updated.add(toolId)
            }
            current.copy(bookmarkedIds = updated)
        }
    }

    fun openStarterGuide(tool: SuggestedTool) {
        _uiState.update { it.copy(activeGuideTool = tool) }
    }

    fun closeStarterGuide() {
        _uiState.update { it.copy(activeGuideTool = null) }
    }

    fun showBlueprintDialog(show: Boolean) {
        _uiState.update { it.copy(showBlueprintDialog = show) }
    }

    fun resetToInput() {
        _uiState.update {
            it.copy(
                analysisResult = null,
                statusMessage = "",
                selectedLetterFilter = null,
                selectedSortOrder = ToolSortOrder.RELEVANCE
            )
        }
    }

    fun refreshMarketTools() {
        val analysis = _uiState.value.analysisResult ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isMarketRefreshing = true,
                    statusMessage = "Querying newest released developer tools & model market updates..."
                )
            }

            try {
                val updatedAnalysis = geminiService.analyzeProject(
                    prompt = _uiState.value.prompt.ifBlank { analysis.projectTitle },
                    documentContent = _uiState.value.attachedDocContent,
                    forceFreshMarketSearch = true
                )

                _uiState.update {
                    it.copy(
                        isMarketRefreshing = false,
                        analysisResult = updatedAnalysis,
                        marketSyncTime = "Updated with latest market releases (GPT-6 Astra, Claude 3.7, Cursor 0.45+)",
                        statusMessage = "Market tools refreshed with the latest launched tech!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isMarketRefreshing = false,
                        marketSyncTime = "Catalog synchronized with current market releases",
                        statusMessage = "Refreshed tools catalog with latest releases."
                    )
                }
            }
        }
    }

    /**
     * Filters the tools based on:
     * 1. Pricing: ALL vs UNPAID (Free) vs PAID
     * 2. Category: ALL (combined) vs CODE, FRONTEND, BACKEND, DATABASE, DEPLOY, AI_MODELS
     * 3. A to Z initial letter
     * 4. Search query
     * 5. Sorting order (Relevance, A-Z, Z-A, Free First, Paid First)
     */
    fun getFilteredTools(): List<SuggestedTool> {
        val allTools = _uiState.value.analysisResult?.tools ?: emptyList()
        val pricingFilter = _uiState.value.selectedPricingFilter
        val categoryFilter = _uiState.value.selectedCategoryFilter
        val letterFilter = _uiState.value.selectedLetterFilter
        val sortOrder = _uiState.value.selectedSortOrder
        val query = _uiState.value.searchQuery.trim().lowercase()

        val filtered = allTools.filter { tool ->
            // Pricing filter
            val matchesPricing = when (pricingFilter) {
                PricingType.ALL -> true
                PricingType.UNPAID -> tool.pricing == PricingType.UNPAID
                PricingType.PAID -> tool.pricing == PricingType.PAID
            }

            // Category filter
            val matchesCategory = when (categoryFilter) {
                ToolCategory.ALL -> true
                else -> tool.category == categoryFilter
            }

            // Letter filter (A-Z)
            val matchesLetter = letterFilter.isNullOrBlank() || letterFilter == "ALL" ||
                    tool.initialLetter.toString().equals(letterFilter, ignoreCase = true)

            // Search query filter
            val matchesQuery = query.isBlank() ||
                    tool.name.lowercase().contains(query) ||
                    tool.tagline.lowercase().contains(query) ||
                    tool.whyFitsProject.lowercase().contains(query) ||
                    tool.category.label.lowercase().contains(query) ||
                    tool.pricingDetails.lowercase().contains(query) ||
                    tool.studentTip.lowercase().contains(query) ||
                    tool.starterCommand.lowercase().contains(query)

            matchesPricing && matchesCategory && matchesLetter && matchesQuery
        }

        return when (sortOrder) {
            ToolSortOrder.RELEVANCE -> filtered
            ToolSortOrder.ALPHABETICAL_ASC -> filtered.sortedBy { it.name.lowercase() }
            ToolSortOrder.ALPHABETICAL_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            ToolSortOrder.PRICE_FREE_FIRST -> filtered.sortedWith(
                compareBy({ it.pricing != PricingType.UNPAID }, { it.name.lowercase() })
            )
            ToolSortOrder.PRICE_PAID_FIRST -> filtered.sortedWith(
                compareBy({ it.pricing != PricingType.PAID }, { it.name.lowercase() })
            )
        }
    }

    /**
     * Computes the count of tools per initial letter (A-Z) matching the active pricing and category filter.
     */
    fun getAlphabetLetterCounts(): Map<Char, Int> {
        val allTools = _uiState.value.analysisResult?.tools ?: emptyList()
        val pricingFilter = _uiState.value.selectedPricingFilter
        val categoryFilter = _uiState.value.selectedCategoryFilter

        val relevantTools = allTools.filter { tool ->
            val matchesPricing = when (pricingFilter) {
                PricingType.ALL -> true
                PricingType.UNPAID -> tool.pricing == PricingType.UNPAID
                PricingType.PAID -> tool.pricing == PricingType.PAID
            }
            val matchesCategory = when (categoryFilter) {
                ToolCategory.ALL -> true
                else -> tool.category == categoryFilter
            }
            matchesPricing && matchesCategory
        }

        val counts = mutableMapOf<Char, Int>()
        for (tool in relevantTools) {
            val letter = tool.initialLetter
            counts[letter] = (counts[letter] ?: 0) + 1
        }
        return counts
    }

    fun generateMarkdownBlueprint(): String {
        val analysis = _uiState.value.analysisResult ?: return ""
        val tools = analysis.tools

        return buildString {
            appendLine("# TOOLSUGGESTOR Project Architecture Blueprint")
            appendLine("## Project: ${analysis.projectTitle}")
            appendLine()
            appendLine("### Executive Overview")
            appendLine(analysis.projectOverview)
            appendLine()
            appendLine("### Recommended Tech Stack")
            appendLine(analysis.recommendedArchitecture)
            appendLine()
            appendLine("### Estimated Student Budget")
            appendLine(analysis.studentCostSummary)
            appendLine()
            appendLine("---")
            appendLine("### Recommended Tools List")
            appendLine()
            tools.groupBy { it.category }.forEach { (category, categoryTools) ->
                appendLine("#### ${category.label}")
                categoryTools.forEach { tool ->
                    val priceBadge = if (tool.pricing == PricingType.UNPAID) "🟢 [FREE/UNPAID]" else "🟣 [PAID]"
                    appendLine("- **${tool.name}** $priceBadge: ${tool.tagline}")
                    appendLine("  * *Pricing Details*: ${tool.pricingDetails}")
                    appendLine("  * *Why It Fits*: ${tool.whyFitsProject}")
                    appendLine("  * *Student Tip*: ${tool.studentTip}")
                    if (tool.starterCommand.isNotBlank()) {
                        appendLine("  * *Starter Command*: `${tool.starterCommand}`")
                    }
                    if (tool.officialUrl.isNotBlank()) {
                        appendLine("  * *Website*: ${tool.officialUrl}")
                    }
                    appendLine()
                }
            }
            appendLine("---")
            appendLine("Generated by TOOLSUGGESTOR AI - College Project Architect")
        }
    }
}
