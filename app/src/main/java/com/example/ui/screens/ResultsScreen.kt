package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PricingType
import com.example.model.SuggestedTool
import com.example.model.ToolCategory
import com.example.model.ToolSortOrder
import com.example.ui.ToolSuggestorUiState
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    state: ToolSuggestorUiState,
    filteredTools: List<SuggestedTool>,
    letterCounts: Map<Char, Int>,
    onBackToInput: () -> Unit,
    onPricingSelected: (PricingType) -> Unit,
    onCategorySelected: (ToolCategory) -> Unit,
    onLetterSelected: (String?) -> Unit,
    onSortOrderSelected: (ToolSortOrder) -> Unit,
    onSearchChanged: (String) -> Unit,
    onBookmarkToggled: (String) -> Unit,
    onOpenGuide: (SuggestedTool) -> Unit,
    onCloseGuide: () -> Unit,
    onShowBlueprint: (Boolean) -> Unit,
    onRefreshMarketTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analysis = state.analysisResult ?: return
    val totalToolsCount = analysis.tools.size
    val unpaidCount = analysis.tools.count { it.pricing == PricingType.UNPAID }
    val paidCount = analysis.tools.count { it.pricing == PricingType.PAID }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = analysis.projectTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "All Tools A to Z • Complete Market Directory",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToInput,
                        modifier = Modifier.testTag("back_to_input_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Project Input"
                        )
                    }
                },
                actions = {
                    // Refresh market radar button
                    IconButton(
                        onClick = onRefreshMarketTools,
                        enabled = !state.isMarketRefreshing,
                        modifier = Modifier.testTag("refresh_market_btn")
                    ) {
                        if (state.isMarketRefreshing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Market Tools",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Blueprint export button
                    IconButton(
                        onClick = { onShowBlueprint(true) },
                        modifier = Modifier.testTag("export_blueprint_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Export Blueprint",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // 1. Project Overview & Architecture Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "RECOMMENDED ARCHITECTURE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FreeGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "100% Student Free Tier Possible",
                                    color = FreeGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = analysis.projectOverview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = analysis.recommendedArchitecture,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (state.marketSyncTime.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = state.marketSyncTime,
                                    fontSize = 11.sp,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Filter Section (As specifically requested: Primary Paid / Unpaid filter + Sub options for Code, Frontend, Backend, Deploy, etc.)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // A. Pricing Options: All vs Unpaid (Free) vs Paid
                    PricingToggleBar(
                        selectedPricing = state.selectedPricingFilter,
                        onPricingSelected = onPricingSelected,
                        unpaidCount = unpaidCount,
                        paidCount = paidCount,
                        totalCount = totalToolsCount
                    )

                    // B. Sub-options: Category filters (Horizontal scroll chips)
                    Column {
                        Text(
                            text = "CATEGORY SUB-OPTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ToolCategory.values().forEach { category ->
                                val categoryCount = analysis.tools.count { tool ->
                                    val matchCategory = if (category == ToolCategory.ALL) true else tool.category == category
                                    val matchPricing = when (state.selectedPricingFilter) {
                                        PricingType.ALL -> true
                                        PricingType.UNPAID -> tool.pricing == PricingType.UNPAID
                                        PricingType.PAID -> tool.pricing == PricingType.PAID
                                    }
                                    matchCategory && matchPricing
                                }
                                CategoryChip(
                                    category = category,
                                    isSelected = state.selectedCategoryFilter == category,
                                    onClick = { onCategorySelected(category) },
                                    count = categoryCount
                                )
                            }
                        }
                    }

                    // C. A to Z Alphabetical Directory Bar
                    AlphabetIndexBar(
                        selectedLetter = state.selectedLetterFilter,
                        onLetterSelected = onLetterSelected,
                        letterCounts = letterCounts,
                        totalCount = totalToolsCount
                    )

                    // D. Sort order selector
                    SortOrderSelector(
                        selectedSortOrder = state.selectedSortOrder,
                        onSortOrderSelected = onSortOrderSelected
                    )

                    // E. Search within suggested tools
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = {
                            Text(
                                "Search tools (e.g., Cursor, Supabase, GPT-6, Next.js)...",
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // 3. Filter Status Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pricingLabel = when (state.selectedPricingFilter) {
                        PricingType.ALL -> "All Pricing"
                        PricingType.UNPAID -> "Free / Unpaid Only"
                        PricingType.PAID -> "Paid / Subscriptions Only"
                    }

                    val categoryLabel = state.selectedCategoryFilter.label
                    val letterLabel = if (state.selectedLetterFilter != null) " • Letter '${state.selectedLetterFilter}'" else ""
                    val sortLabel = if (state.selectedSortOrder != ToolSortOrder.RELEVANCE) " • ${state.selectedSortOrder.label}" else ""

                    Text(
                        text = "Showing ${filteredTools.size} tools ($pricingLabel • $categoryLabel$letterLabel$sortLabel)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (state.selectedPricingFilter != PricingType.ALL ||
                        state.selectedCategoryFilter != ToolCategory.ALL ||
                        state.selectedLetterFilter != null ||
                        state.selectedSortOrder != ToolSortOrder.RELEVANCE ||
                        state.searchQuery.isNotBlank()
                    ) {
                        TextButton(
                            onClick = {
                                onPricingSelected(PricingType.ALL)
                                onCategorySelected(ToolCategory.ALL)
                                onLetterSelected(null)
                                onSortOrderSelected(ToolSortOrder.RELEVANCE)
                                onSearchChanged("")
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Reset Filters", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 4. List of Suggested Tools
            if (filteredTools.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No tools match this filter combination",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try selecting 'All Pricing' or 'All Categories' to see the full combined toolchain.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    onPricingSelected(PricingType.ALL)
                                    onCategorySelected(ToolCategory.ALL)
                                    onSearchChanged("")
                                }
                            ) {
                                Text("Show All Combined Tools")
                            }
                        }
                    }
                }
            } else {
                items(filteredTools, key = { it.id }) { tool ->
                    ToolItemCard(
                        tool = tool,
                        isBookmarked = state.bookmarkedIds.contains(tool.id),
                        onBookmarkToggle = { onBookmarkToggled(tool.id) },
                        onOpenGuide = { onOpenGuide(tool) }
                    )
                }
            }
        }
    }

    // Active Starter Guide Dialog
    if (state.activeGuideTool != null) {
        StarterGuideDialog(
            tool = state.activeGuideTool,
            onDismiss = onCloseGuide
        )
    }

    // Blueprint Export Dialog
    if (state.showBlueprintDialog) {
        val blueprintMarkdown = remember(analysis) {
            buildString {
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
                appendLine("### Complete Recommended Tools List")
                appendLine()
                analysis.tools.groupBy { it.category }.forEach { (category, tools) ->
                    appendLine("#### ${category.label}")
                    tools.forEach { tool ->
                        val priceBadge = if (tool.pricing == PricingType.UNPAID) "🟢 [FREE / UNPAID]" else "🟣 [PAID]"
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

        BlueprintExportDialog(
            markdownBlueprint = blueprintMarkdown,
            onDismiss = { onShowBlueprint(false) }
        )
    }
}
