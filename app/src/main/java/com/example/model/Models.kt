package com.example.model

enum class ToolCategory(val label: String, val shortName: String) {
    ALL("All Categories", "All"),
    CODE("Coding & AI Assist", "Code"),
    FRONTEND("Frontend UI", "Frontend"),
    BACKEND("Backend & API", "Backend"),
    DATABASE("Database & Storage", "Database"),
    DEPLOY("Deploy & Cloud", "Deploy"),
    AI_MODELS("AI Models & LLMs", "AI Models")
}

enum class PricingType(val label: String, val badgeText: String) {
    ALL("All Pricing", "All"),
    UNPAID("Unpaid / Free", "Free / Unpaid"),
    PAID("Paid / Subscription", "Paid")
}

enum class ToolSortOrder(val label: String) {
    RELEVANCE("Relevance"),
    ALPHABETICAL_ASC("A → Z"),
    ALPHABETICAL_DESC("Z → A"),
    PRICE_FREE_FIRST("Free First"),
    PRICE_PAID_FIRST("Paid First")
}

data class SuggestedTool(
    val id: String,
    val name: String,
    val tagline: String,
    val category: ToolCategory,
    val pricing: PricingType,
    val pricingDetails: String,
    val whyFitsProject: String,
    val keyFeatures: List<String>,
    val studentTip: String,
    val isNewOrTrending: Boolean = false,
    val releaseBadge: String = "",
    val starterGuide: String = "",
    val starterCommand: String = "",
    val officialUrl: String = "",
    val isBookmarked: Boolean = false,
    val popularityRank: String = "", // e.g. "#1 Most Popular in 2025/2026"
    val isRealTimeLiveAnalysis: Boolean = true
) {
    val initialLetter: Char
        get() {
            val cleaned = name.trim().removePrefix("0x").removePrefix("0X")
            val firstChar = cleaned.firstOrNull { it.isLetter() } ?: name.trim().firstOrNull() ?: '#'
            return firstChar.uppercaseChar()
        }
}

data class ProjectAnalysis(
    val projectTitle: String,
    val projectOverview: String,
    val recommendedArchitecture: String,
    val studentCostSummary: String,
    val tools: List<SuggestedTool>,
    val timestamp: Long = System.currentTimeMillis(),
    val isLiveInternetAnalyzed: Boolean = true,
    val marketStatusBadge: String = "Real-Time Internet Analysis Active"
)

data class SampleProject(
    val title: String,
    val description: String,
    val categoryHint: String
)
