package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CppTipViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CppTipViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = uiState.themeMode) {
                CppTotwApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CppTotwApp(viewModel: CppTipViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onDismissStatusMessage()
        }
    }

    if (showSettings) {
        SettingsDialog(
            uiState = uiState,
            onDismiss = { showSettings = false },
            onThemeSelected = { mode -> viewModel.onSetThemeMode(mode) },
            onCheckForUpdates = { viewModel.checkUpdatesAndResync() }
        )
    }

    if (uiState.selectedTip != null) {
        DetailScreen(
            tip = uiState.selectedTip!!,
            allTips = uiState.filteredTips.ifEmpty { uiState.allTips },
            isLoading = uiState.isLoadingTipDetails,
            onBack = { viewModel.onClearSelectedTip() },
            onBookmarkToggle = { viewModel.onToggleBookmark(it) },
            onDownloadTip = { tipId -> viewModel.downloadSingleTip(tipId) },
            onSaveNotes = { id, notes -> viewModel.onSaveUserNotes(id, notes) },
            onNavigateToTip = { viewModel.onSelectTip(it) }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.testTag("top_bar_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "C++",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tip of the Week",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Online / Offline Status Dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8))
                            )
                        }
                    },
                    actions = {
                        // Random Tip Button (🎲)
                        IconButton(
                            onClick = { viewModel.onRollRandomTip() },
                            modifier = Modifier.testTag("top_bar_random_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = "Random Tip",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            HomeScreen(
                uiState = uiState,
                onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                onFilterSelected = { viewModel.onFilterSelected(it) },
                onTipClick = { viewModel.onSelectTip(it) },
                onBookmarkToggle = { viewModel.onToggleBookmark(it) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
