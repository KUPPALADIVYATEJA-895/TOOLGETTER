package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ToolSuggestorViewModel
import com.example.ui.screens.InputScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.theme.ToolSuggestorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ToolSuggestorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ToolSuggestorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ToolSuggestorApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ToolSuggestorApp(viewModel: ToolSuggestorViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredTools = viewModel.getFilteredTools()

    // Handle system back button when in results view to return to input
    if (uiState.analysisResult != null) {
        BackHandler {
            viewModel.resetToInput()
        }
    }

    AnimatedContent(
        targetState = uiState.analysisResult != null,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ScreenTransition"
    ) { hasAnalysis ->
        if (!hasAnalysis) {
            // First Page: Project Prompt or Document Input
            InputScreen(
                state = uiState,
                onPromptChanged = viewModel::onPromptChanged,
                onDocumentAttached = viewModel::handleDocumentSelected,
                onClearDocument = viewModel::clearAttachedDocument,
                onLoadSample = viewModel::loadSampleProject,
                onAnalyzeClicked = viewModel::analyzeProject
            )
        } else {
            // Second Page: Revealed Filter Section and Tools View
            ResultsScreen(
                state = uiState,
                filteredTools = filteredTools,
                letterCounts = viewModel.getAlphabetLetterCounts(),
                onBackToInput = viewModel::resetToInput,
                onPricingSelected = viewModel::setPricingFilter,
                onCategorySelected = viewModel::setCategoryFilter,
                onLetterSelected = viewModel::setLetterFilter,
                onSortOrderSelected = viewModel::setSortOrder,
                onSearchChanged = viewModel::setSearchQuery,
                onBookmarkToggled = viewModel::toggleBookmark,
                onOpenGuide = viewModel::openStarterGuide,
                onCloseGuide = viewModel::closeStarterGuide,
                onShowBlueprint = viewModel::showBlueprintDialog,
                onRefreshMarketTools = viewModel::refreshMarketTools
            )
        }
    }
}

