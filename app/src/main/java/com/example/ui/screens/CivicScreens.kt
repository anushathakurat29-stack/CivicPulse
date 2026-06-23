package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CitizenImpact
import com.example.data.model.CommunityIssue
import com.example.data.model.IssueComment
import com.example.ui.viewmodel.CivicViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TownHallMessage
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.CardBorderLight
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CivicMainScreen(viewModel: CivicViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeCitizen by viewModel.currentCitizen.collectAsStateWithLifecycle()
    val leaderboardValues by viewModel.leaderboard.collectAsStateWithLifecycle()

    var showProfileDialog by remember { mutableStateOf(false) }

    // Estimate current user's profile points
    val currentUserPoints = leaderboardValues.find { it.citizenName == activeCitizen }?.points ?: 0

    Scaffold(
        topBar = {
            CivicAppBar(
                citizenName = activeCitizen,
                citizenPoints = currentUserPoints,
                onProfileClick = { showProfileDialog = true },
                onLogoClick = { viewModel.navigateTo(Screen.Feed) }
            )
        },
        bottomBar = {
            CivicBottomNavigation(
                currentScreen = currentScreen,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        floatingActionButton = {
            if (currentScreen == Screen.Feed) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(Screen.Report) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("report_issue_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Report New Issue")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.Feed -> IssueFeedScreen(viewModel = viewModel)
                    Screen.Report -> ReportWidgetScreen(viewModel = viewModel)
                    is Screen.Detail -> IssueDetailScreen(viewModel = viewModel)
                    Screen.Leaderboard -> LeaderboardScreen(viewModel = viewModel)
                    Screen.Map -> CivicMapScreen(viewModel = viewModel)
                    Screen.Impact -> CivicImpactScreen(viewModel = viewModel)
                    is Screen.TownHall -> CivicTownHallScreen(
                        viewModel = viewModel,
                        petitionId = targetScreen.petitionId,
                        category = targetScreen.category,
                        location = targetScreen.location,
                        representativeName = targetScreen.representativeName
                    )
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileCustomizerDialog(
            currentName = activeCitizen,
            currentPoints = currentUserPoints,
            onDismiss = { showProfileDialog = false },
            onSave = { name ->
                viewModel.updateCitizenName(name)
                showProfileDialog = false
            }
        )
    }
}

// Custom Top Navigation Header
@Composable
fun CivicAppBar(
    citizenName: String,
    citizenPoints: Int,
    onProfileClick: () -> Unit,
    onLogoClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onLogoClick() }
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CivicPulse",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Collaborative Action Ledger",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Citizen Profile Button with Score Indicator
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier
                    .clickable { onProfileClick() }
                    .testTag("appbar_profile_capsule")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$citizenPoints pts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = citizenName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 80.dp)
                    )
                }
            }
        }
    }
}

// Custom bottom navigation bar
@Composable
fun CivicBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("civic_bottom_nav_bar")
    ) {
        val showFeed = currentScreen == Screen.Feed || currentScreen is Screen.Detail
        NavigationBarItem(
            selected = showFeed,
            onClick = { onNavigate(Screen.Feed) },
            icon = {
                Icon(
                    imageVector = if (showFeed) Icons.Default.Assignment else Icons.Outlined.Assignment,
                    contentDescription = "Issues list"
                )
            },
            label = { Text("Feed", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_feed_tab")
        )

        val showMap = currentScreen == Screen.Map
        NavigationBarItem(
            selected = showMap,
            onClick = { onNavigate(Screen.Map) },
            icon = {
                Icon(
                    imageVector = if (showMap) Icons.Default.Map else Icons.Outlined.Map,
                    contentDescription = "Map view"
                )
            },
            label = { Text("Map", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_map_tab")
        )

        val showReport = currentScreen == Screen.Report
        NavigationBarItem(
            selected = showReport,
            onClick = { onNavigate(Screen.Report) },
            icon = {
                Icon(
                    imageVector = if (showReport) Icons.Default.PostAdd else Icons.Outlined.PostAdd,
                    contentDescription = "Report Issue"
                )
            },
            label = { Text("Report", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_report_tab")
        )

        val showImpact = currentScreen == Screen.Impact
        NavigationBarItem(
            selected = showImpact,
            onClick = { onNavigate(Screen.Impact) },
            icon = {
                Icon(
                    imageVector = if (showImpact) Icons.Default.Assessment else Icons.Outlined.Assessment,
                    contentDescription = "Impact Insights"
                )
            },
            label = { Text("Impact", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_impact_tab")
        )

        val showLeaderboard = currentScreen == Screen.Leaderboard
        NavigationBarItem(
            selected = showLeaderboard,
            onClick = { onNavigate(Screen.Leaderboard) },
            icon = {
                Icon(
                    imageVector = if (showLeaderboard) Icons.Default.Leaderboard else Icons.Outlined.Leaderboard,
                    contentDescription = "Leaderboard"
                )
            },
            label = { Text("Board", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_leaderboard_tab")
        )
    }
}

// Issue Feed Screen list & filtering
@Composable
fun IssueFeedScreen(viewModel: CivicViewModel) {
    val items by viewModel.issues.collectAsStateWithLifecycle()
    val petitions by viewModel.microPetitions.collectAsStateWithLifecycle()
    val activeFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortByColumn.collectAsStateWithLifecycle()

    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Active Issues, 1 = Micro-Petitions

    val categories = listOf("Road", "Lighting", "Sanitation", "Utilities", "Environment", "Safety", "Other")

    val activeCount = items.count { it.status != "RESOLVED" }
    val pendingCount = items.count { it.status == "REPORTED" }
    val resolvedCount = items.count { it.status == "RESOLVED" }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Active count card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD3E4FF)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.filterByCategory(null) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%02d", activeCount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36)
                    )
                    Text(
                        text = "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = Color(0xFF001D36)
                    )
                }
            }

            // Pending count card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E2E6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%02d", pendingCount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                    Text(
                        text = "PENDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = Color(0xFF1B1B1F)
                    )
                }
            }

            // Resolved count card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC2EFD0)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%02d", resolvedCount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00210E)
                    )
                    Text(
                        text = "RESOLVED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = Color(0xFF00210E)
                    )
                }
            }
        }

        // Sub-Tab Switcher
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("feed_sub_tabs")
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Active Issues", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Assignment, contentDescription = "Issues list", modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Micro-Petitions", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Campaign, contentDescription = "Micro petitions", modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedSubTab == 0) {
            // Categories horizontal scroll list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "All" filter button
                    FilterChip(
                        selected = activeFilter == null,
                        onClick = { viewModel.filterByCategory(null) },
                        label = { Text("All Issues") },
                        modifier = Modifier.testTag("filter_all")
                    )

                    for (cat in categories) {
                        val isSelected = activeFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.filterByCategory(cat) },
                            label = { Text(cat) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("filter_$cat")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sort Selector bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${items.size} verified reports",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        TextButton(
                            onClick = {
                                val newSort = if (sortBy == "timestamp") "upvotes" else "timestamp"
                                viewModel.updateSort(newSort)
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (sortBy == "timestamp") "Latest First" else "Most Endorsed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (sortBy == "timestamp") Icons.Default.SwapVert else Icons.Default.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Empty state check
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflineBolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Community Issues Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Excellent! All local structures in your selected category are running at 100% efficiency. Report an issue to register a new report.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("issues_feed_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items, key = { it.id }) { issue ->
                        IssueCard(
                            issue = issue,
                            onCardClick = { viewModel.navigateTo(Screen.Detail(issue.id)) },
                            onValidateClick = { viewModel.voteOrValidate(issue.id) }
                        )
                    }
                }
            }
        } else {
            // Micro-Petitions and Escalated Digital Town Halls View
            CivicPetitionsScreen(viewModel = viewModel, petitions = petitions)
        }
    }
}

// Detailed single issue record card
@Composable
fun IssueCard(
    issue: CommunityIssue,
    onCardClick: () -> Unit,
    onValidateClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) CardBorderDark else CardBorderLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("issue_card_${issue.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Category Badge & Priority Cap
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Side Icon Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF4F3F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(issue.category),
                        contentDescription = null,
                        tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF5D5E67),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title & Subtitle Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = issue.category.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "#R-${issue.id.hashCode().coerceAtLeast(1000).toString().take(4)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    Text(
                        text = issue.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sentient Routing Hazard Badge
                if (issue.isImmediateSafetyHazard) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFBA1A1A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Immediate Hazard",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "HAZARD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description or Snippet
            Text(
                text = issue.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar and labels if it's in progress/resolving
            val progress = when (issue.status.uppercase()) {
                "REPORTED" -> 0.35f
                "VALIDATED" -> 0.65f
                "IN_PROGRESS" -> 0.85f
                "RESOLVED" -> 1.0f
                else -> 0.35f
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (issue.status == "RESOLVED") Color(0xFF006E1C) else MaterialTheme.colorScheme.primary,
                    trackColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEEF0F6)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Validating",
                        fontSize = 10.sp,
                        fontWeight = if (issue.status == "REPORTED") FontWeight.Bold else FontWeight.Normal,
                        color = if (issue.status == "REPORTED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Fixing",
                        fontSize = 10.sp,
                        fontWeight = if (issue.status == "IN_PROGRESS") FontWeight.Bold else FontWeight.Normal,
                        color = if (issue.status == "IN_PROGRESS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Closed",
                        fontSize = 10.sp,
                        fontWeight = if (issue.status == "RESOLVED") FontWeight.Bold else FontWeight.Normal,
                        color = if (issue.status == "RESOLVED") Color(0xFF006E1C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer row with Location & Endorsement action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isDark) MaterialTheme.colorScheme.error else Color(0xFF43474E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = issue.location,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PriorityBadge(issue.priority)

                    // Tone analysis chip
                    val toneColor = when (issue.citizenTone.uppercase()) {
                        "ANGRY" -> Color(0xFFFFDAD6)
                        "ANXIOUS" -> Color(0xFFFFDCC8)
                        "HELPFUL" -> Color(0xFFD1F1D6)
                        "URGENT" -> Color(0xFFFFD4D4)
                        else -> Color(0xFFE2E2E6)
                    }
                    val toneTextColor = when (issue.citizenTone.uppercase()) {
                        "ANGRY" -> Color(0xFF410002)
                        "ANXIOUS" -> Color(0xFF331400)
                        "HELPFUL" -> Color(0xFF003912)
                        "URGENT" -> Color(0xFFBA1A1A)
                        else -> Color(0xFF1B1B1F)
                    }
                    val toneEmoji = when (issue.citizenTone.uppercase()) {
                        "ANGRY" -> "😠"
                        "ANXIOUS" -> "😰"
                        "HELPFUL" -> "😊"
                        "URGENT" -> "⚡"
                        else -> "😐"
                    }
                    Box(
                        modifier = Modifier
                            .background(toneColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$toneEmoji ${issue.citizenTone}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = toneTextColor
                        )
                    }

                    // Endorsement count click
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFD3E4FF),
                        modifier = Modifier.clickable { onValidateClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Validate",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF001D36),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${issue.upvoteCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF001D36)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Priority Badge Style
@Composable
fun PriorityBadge(priority: String) {
    val (textColor, bgColor, text) = when (priority.uppercase()) {
        "HIGH" -> Triple(Color(0xFFBA1A1A), Color(0xFFFFDAD6), "URGENT")
        "MEDIUM" -> Triple(Color(0xFFB45309), Color(0xFFFEF3C7), "MEDIUM")
        else -> Triple(Color(0xFF006E1C), Color(0xFFC2EFD0), "NEW")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Status badge
@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status.uppercase()) {
        "REPORTED" -> Color(0xFF8B5CF6) to "Logged" // Violet
        "VALIDATED" -> Color(0xFF06B6D4) to "Endorsed" // Cyan
        "IN_PROGRESS" -> Color(0xFFF59E0B) to "In Progress" // Amber
        "RESOLVED" -> Color(0xFF10B981) to "Resolved" // Green
        else -> Color(0xFF64748B) to status
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Report Widget wizard
@Composable
fun ReportWidgetScreen(viewModel: CivicViewModel) {
    val titleState by viewModel.reportTitle.collectAsStateWithLifecycle()
    val descState by viewModel.reportDesc.collectAsStateWithLifecycle()
    val locationState by viewModel.reportLocation.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiDraftLoading.collectAsStateWithLifecycle()
    val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Report Civic Concern",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Tell us about a local municipal issue. The CivicResolve system will automatically draft resolution roadmaps and loop in public teams.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        )

        // Title text field
        OutlinedTextField(
            value = titleState,
            onValueChange = { viewModel.reportTitle.value = it },
            label = { Text("Issue Title") },
            placeholder = { Text("e.g., Clogged sewerage sewer at 5th St.") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_field_title"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
        )

        // Description
        OutlinedTextField(
            value = descState,
            onValueChange = { viewModel.reportDesc.value = it },
            label = { Text("Detailed Description") },
            placeholder = { Text("Provide details, dimensions, safety risks, or surrounding hazards.") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("report_field_description"),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
        )

        val attachedMediaType by viewModel.reportMediaType.collectAsStateWithLifecycle()
        val attachedMediaUrl by viewModel.reportMediaUrl.collectAsStateWithLifecycle()

        var showPhotoPickerDialog by remember { mutableStateOf(false) }
        var showVideoPickerDialog by remember { mutableStateOf(false) }

        // Capture & Upload Evidence Section
        if (attachedMediaType != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (attachedMediaType == "VIDEO") Icons.Default.Videocam else Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (attachedMediaType == "VIDEO") "Evidence Video Attached" else "Evidence Photo Attached",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = attachedMediaUrl ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFF3CD),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "SIMULATED EVIDENCE ATTACHMENT",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF856404),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        viewModel.reportMediaType.value = null
                        viewModel.reportMediaUrl.value = null
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Capture & Upload Evidence",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Attach high-fidelity photos or video clips to substantiate your ticket and feed AI classifications.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPhotoPickerDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Photo", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { showVideoPickerDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Video", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Photo Picker Dialog
        if (showPhotoPickerDialog) {
            Dialog(onDismissRequest = { showPhotoPickerDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Simulate Photo Capture",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select a simulated high-definition photo captured via the device camera:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        val mockPhotos = listOf(
                            "pothole_crater_depth_3in" to "Pothole Crater (3 inches deep)",
                            "trash_dump_6_bags" to "Illegal trash dump (6 bags of rubbish)",
                            "dark_streetlight_block_5" to "Dimly blinking fluorescent lamp",
                            "water_spurt_pavement" to "Sub-sidewalk water pipe crack spurt"
                        )
                        
                        mockPhotos.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.reportMediaType.value = "IMAGE"
                                        viewModel.reportMediaUrl.value = "hd_camera_capture_${key}.jpg"
                                        showPhotoPickerDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // Video Picker Dialog
        if (showVideoPickerDialog) {
            Dialog(onDismissRequest = { showVideoPickerDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Simulate Video Recording",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select a simulated video clip recorded to substantiate movement, audio patterns, or volume of issues:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        val mockVideos = listOf(
                            "water_geyser_leak_12s" to "Water Main Geyser spraying bakery wall (12s clip)",
                            "flickering_streetlight_8s" to "Flickering dangerous street corner light (8s clip)",
                            "garbage_pile_stench_15s" to "Overflowing garbage dump with rodent indicators (15s clip)",
                            "pothole_tire_strike_5s" to "Pothole causing severe tire impact noise (5s clip)"
                        )
                        
                        mockVideos.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.reportMediaType.value = "VIDEO"
                                        viewModel.reportMediaUrl.value = "video_recording_${key}.mp4"
                                        showVideoPickerDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // Location text field
        OutlinedTextField(
            value = locationState,
            onValueChange = { viewModel.reportLocation.value = it },
            label = { Text("Location description") },
            placeholder = { Text("e.g. Near Civic Center park entrance") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_field_location"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
        )

        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

        // AI automation drafting section
        if (aiLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Gemini AI Analyzing & Planning...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Creating municipal classification, analyzing priority rating, and drafting an actionable resolution plan.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else if (aiAnalysisResult != null) {
            val result = aiAnalysisResult!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_plan_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OfflineBolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Intelligent AI Automation Draft",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Suggesting Plan",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Category",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                result.category,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Priority",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                            PriorityBadge(result.priority)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Muni Est. Resolution",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${result.estimatedDaysToResolve} working days",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Automated Municipal Resolution Plan:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.actionPlan,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Draft with Gemini AI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Analyze description, automatically select category/priority, and design step-by-step resolution steps immediately.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Button(
                        onClick = { viewModel.generateAIDraft() },
                        enabled = titleState.isNotBlank() && descState.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("ai_draft_button")
                    ) {
                        Text("Draft Plan", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.Feed) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = { viewModel.submitReport() },
                enabled = titleState.isNotBlank() && descState.isNotBlank() && aiAnalysisResult != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("report_submit_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Publish Report")
                }
            }
        }
    }
}

// Single Issue Detail Screen
@Composable
fun IssueDetailScreen(viewModel: CivicViewModel) {
    val issue by viewModel.selectedIssue.collectAsStateWithLifecycle()
    val comments by viewModel.currentComments.collectAsStateWithLifecycle()

    var commentText by remember { mutableStateOf("") }
    var resolutionText by remember { mutableStateOf("") }
    var showResolveDialog by remember { mutableStateOf(false) }

    if (issue == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentIssue = issue!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Feed) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Go back",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    PriorityBadge(currentIssue.priority)
                    StatusBadge(currentIssue.status)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentIssue.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meta Row Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentIssue.location,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Reported by: ${currentIssue.reporterName}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Large Validate Capsule
                    Button(
                        onClick = { viewModel.voteOrValidate(currentIssue.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("detail_endorse_button")
                    ) {
                        Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Endorse (${currentIssue.upvoteCount})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Description block
            Text(
                text = "What was observed:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Text(
                text = currentIssue.description,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Attached Media Evidence Section
            if (currentIssue.mediaType != null) {
                Text(
                    text = "📁 Attached Evidence",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        // Simulated media container window
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color(0xFF1E1E1E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (currentIssue.mediaType == "VIDEO") Icons.Default.Videocam else Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = if (currentIssue.mediaType == "VIDEO") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (currentIssue.mediaType == "VIDEO") "PLAY LIVE VIDEO STREAM" else "VIEW FULL RES PHOTO",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            // If video, draw a glowing play button overlay
                            if (currentIssue.mediaType == "VIDEO") {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                        .align(Alignment.Center),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play video",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // Media Info Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentIssue.mediaUrl ?: "",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Simulated ${currentIssue.mediaType} Evidence Feed • Verified by CivicResolve AI",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            
                            // High-contrast security badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE6F4EA)
                            ) {
                                Text(
                                    text = "SECURE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF137333),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // --- Sentient Routing & NLP Analysis Panel ---
            Text(
                text = "🤖 AI Sentient Routing Insights",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "REAL-TIME NATURAL LANGUAGE PROCESSING (NLP) DECODER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Emotion / Tone pill
                        val toneEmoji = when (currentIssue.citizenTone.uppercase()) {
                            "ANGRY" -> "😠"
                            "ANXIOUS" -> "😰"
                            "HELPFUL" -> "😊"
                            "URGENT" -> "⚡"
                            else -> "😐"
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Citizen Emotion Tone", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = toneEmoji, fontSize = 16.sp)
                                Text(text = currentIssue.citizenTone, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Hazard Status
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Critical Hazard Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (currentIssue.isImmediateSafetyHazard) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (currentIssue.isImmediateSafetyHazard) Color(0xFFBA1A1A) else Color(0xFF137333),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (currentIssue.isImmediateSafetyHazard) "IMMEDIATE DANGER" else "STABLE / ROUTINE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentIssue.isImmediateSafetyHazard) Color(0xFFBA1A1A) else Color(0xFF137333)
                                )
                            }
                        }
                    }

                    if (currentIssue.isImmediateSafetyHazard && currentIssue.hazardJustification.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFBA1A1A).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "⚠️ MUNICIPAL HAZARD ROUTING JUSTIFICATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBA1A1A)
                                )
                                Text(
                                    text = currentIssue.hazardJustification,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Cleaned Description
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MUNICIPAL DISPATCH TRANSLATION (CLEANED)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = currentIssue.cleanedDescription.ifBlank { "Structured processing pending..." },
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Note: emotionally charged citizen text is converted automatically into structured data for municipal repair teams to minimize bureaucratic bias.",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Step-by-Step Resolution Plan (Collaborative Roadmap)
            if (currentIssue.actionPlan.isNotBlank()) {
                Text(
                    text = "🛠️ Public Resolution Plan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MUNICIPAL ASSISTANCE RESOLUTION ACTION LEDGER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Format actionPlan as step elements
                        val steps = currentIssue.actionPlan.split("\n").filter { it.isNotBlank() }
                        steps.forEachIndexed { index, stepText ->
                            val isResolved = currentIssue.status == "RESOLVED"
                            val isStepActive = !isResolved && (index == 0 || (currentIssue.status == "IN_PROGRESS" && index == 1))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isResolved -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                isStepActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isResolved) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isStepActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = stepText.replace(Regex("^\\d+\\.\\s*"), ""),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = if (isStepActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isResolved) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Resolved Update block (if successfully resolved)
            if (currentIssue.status == "RESOLVED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Issue Resolved Successfully!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currentIssue.resolutionUpdate ?: "Civil services completed repair.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        currentIssue.resolvedTimestamp?.let {
                            Text(
                                text = "Marked resolved on: ${formatDate(it)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
            } else {
                // If not resolved, let citizens collaborative mark it as resolved or in progress!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentIssue.status == "VALIDATED" || currentIssue.status == "REPORTED") {
                        Button(
                            onClick = {
                                viewModel.navigateTo(Screen.Feed)
                                viewModel.addComment(currentIssue.id, "Dispatched site crew.")
                                val updated = currentIssue.copy(status = "IN_PROGRESS")
                                viewModel.updateSort("timestamp") // trigger recompile/update
                                viewModel.voteOrValidate(currentIssue.id) // increment validation to update database
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Repair", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { showResolveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_trigger_resolve_dialog")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Declare Resolved", fontSize = 12.sp)
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Comments Feed Section
            Text(
                text = "💬 Collaboration Board (${comments.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Submit comment box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add update/comment to help...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("field_comment_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(
                    onClick = {
                        viewModel.addComment(currentIssue.id, commentText)
                        commentText = ""
                    },
                    enabled = commentText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f
                            ), RoundedCornerShape(8.dp)
                        )
                        .testTag("btn_submit_comment")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Comments Feed List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (comment in comments) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = comment.authorName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = formatDate(comment.timestamp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = comment.text,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialog to submit resolution verification details
    if (showResolveDialog) {
        Dialog(onDismissRequest = { showResolveDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Resolve Issue Validation",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Explain how this issue was resolved to provide accountability and earn your 100 civil points.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    OutlinedTextField(
                        value = resolutionText,
                        onValueChange = { resolutionText = it },
                        placeholder = { Text("e.g. Cleared debris and washed riverbanks.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("field_resolution_input"),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showResolveDialog = false }) {
                            Text("Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.submitResolution(currentIssue.id, resolutionText)
                                showResolveDialog = false
                                resolutionText = ""
                            },
                            enabled = resolutionText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.testTag("btn_resolution_submit")
                        ) {
                            Text("Submit Verification")
                        }
                    }
                }
            }
        }
    }
}

// Leaderboard / Citizen Impactboard Screen
@Composable
fun LeaderboardScreen(viewModel: CivicViewModel) {
    val scores by viewModel.leaderboard.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Civic Participation Board",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Track your civic contribution standing. Community points are awarded for identifying safety concerns (50 pts), endorsing neighborhood issues (10 pts), and verify resolving issues (100 pts).",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            lineHeight = 18.sp
        )

        // General Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${scores.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Active Citizens",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                )

                val aggregatePoints = scores.sumOf { it.points }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$aggregatePoints",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Total Points Logged",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                )

                val solvedIssues = scores.sumOf { it.issuesResolved }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$solvedIssues",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Issues Resolved",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Leaderboard Standings",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Standings leaderboard items list
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("leaderboard_standings_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                scores.forEachIndexed { idx, impact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Rank indicator
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (idx) {
                                            0 -> Color(0xFFFBBF24).copy(alpha = 0.2f) // Gold
                                            1 -> Color(0xFF94A3B8).copy(alpha = 0.2f) // Silver
                                            2 -> Color(0xFFB45309).copy(alpha = 0.2f) // Bronze
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when (idx) {
                                        0 -> Color(0xFFB45309)
                                        1 -> Color(0xFF475569)
                                        2 -> Color(0xFF78350F)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    }
                                )
                            }

                            // Avatar + Name + badge
                            Column {
                                Text(
                                    text = impact.citizenName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = getUserBadgeTitle(impact.points),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Stats counters / points
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${impact.points} pts",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${impact.issuesReported} reported • ${impact.issuesResolved} resolved",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (idx < scores.size - 1) {
                        Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Dialog to edit profile name
@Composable
fun ProfileCustomizerDialog(
    currentName: String,
    currentPoints: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Citizen Profile settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Establish your citizen identity for the public collaboration ledger. Registered points and resolving contributions scale under this specific identifier.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Public Identifier Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Token,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Standings status: ${getUserBadgeTitle(currentPoints)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Current Ledger score: $currentPoints points",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(nameInput) },
                        enabled = nameInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("profile_save_button")
                    ) {
                        Text("Save Change")
                    }
                }
            }
        }
    }
}

// Helper methods

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Road" -> Icons.Default.Construction
        "Lighting" -> Icons.Default.Lightbulb
        "Sanitation" -> Icons.Default.DeleteOutline
        "Utilities" -> Icons.Default.Water
        "Environment" -> Icons.Default.Spa
        "Safety" -> Icons.Default.Warning
        else -> Icons.Default.Assignment
    }
}

fun getUserBadgeTitle(points: Int): String {
    return when {
        points >= 300 -> "Elite Guardian"
        points >= 200 -> "Civic Catalyst"
        points >= 100 -> "Neighborhood Hero"
        points >= 50 -> "Initiator Pathfinder"
        else -> "Civic Contributor"
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun CivicMapScreen(viewModel: CivicViewModel) {
    val issues by viewModel.issues.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedIssueOnMap by remember { mutableStateOf<CommunityIssue?>(null) }
    
    // Auto select first issue if available and none selected
    LaunchedEffect(issues) {
        if (selectedIssueOnMap == null && issues.isNotEmpty()) {
            selectedIssueOnMap = issues.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Map Screen Title & Info Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Civic Map",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Interactive geographic telemetry of localized civic events.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            
            // Simulated GPS Coordinate Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("gps_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "GPS ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Map Canvas Box (The simulated interactive grid!)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("map_canvas_card"),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E252D)) // Rich Slate Dark Map Theme
            ) {
                // Interactive Grid Line Drawings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val gridSpacing = 60.dp.toPx()
                    
                    // Draw soft coordinate grid lines
                    var x = 0f
                    while (x < width) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                            end = androidx.compose.ui.geometry.Offset(x, height),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }
                    
                    var y = 0f
                    while (y < height) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }

                    // Draw stylized simulated roads / waterways
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = androidx.compose.ui.geometry.Offset(0f, height * 0.4f),
                        end = androidx.compose.ui.geometry.Offset(width, height * 0.45f),
                        strokeWidth = 24.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = androidx.compose.ui.geometry.Offset(width * 0.35f, 0f),
                        end = androidx.compose.ui.geometry.Offset(width * 0.35f, height),
                        strokeWidth = 16.dp.toPx()
                    )
                }

                // Render Pins based on issue coordinates
                for (issue in issues) {
                    // Convert lat/long coordinates of SF (approx 37.75 to 37.80, and -122.45 to -122.38) to local percentage coordinates
                    val latPercent = ((issue.latitude - 37.75) / (37.80 - 37.75)).coerceIn(0.0, 1.0)
                    // Reverse lat percent for screen coords (0 is top)
                    val xPercent = ((issue.longitude - (-122.45)) / ((-122.38) - (-122.45))).coerceIn(0.0, 1.0)
                    val yPercent = 1.0 - latPercent

                    // Set status/category colored ring
                    val pinColor = when (issue.category) {
                        "Road" -> Color(0xFFEA4335)
                        "Lighting" -> Color(0xFFFBBC05)
                        "Environment" -> Color(0xFF34A853)
                        "Utilities" -> Color(0xFF4285F4)
                        "Safety" -> Color(0xFFFF6D01)
                        else -> Color(0xFF9E9E9E)
                    }

                    val isSelected = selectedIssueOnMap?.id == issue.id

                    // Position the pin on screen using Box constraints
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val pixelX = (maxWidth * xPercent.toFloat()) - 14.dp
                        val pixelY = (maxHeight * yPercent.toFloat()) - 14.dp

                        Box(
                            modifier = Modifier
                                .absoluteOffset(x = pixelX, y = pixelY)
                                .size(if (isSelected) 34.dp else 26.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) pinColor else pinColor.copy(alpha = 0.75f))
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                    CircleShape
                                )
                                .clickable { selectedIssueOnMap = issue }
                                .padding(if (isSelected) 5.dp else 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(issue.category),
                                contentDescription = issue.title,
                                tint = Color.White,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Floating Map Controls Info overlay
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("LEGEND", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEA4335)))
                            Text("Roads", color = Color.White, fontSize = 8.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFBBC05)))
                            Text("Lights", color = Color.White, fontSize = 8.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34A853)))
                            Text("Eco", color = Color.White, fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        // Selected Issue Bottom Preview Panel
        if (selectedIssueOnMap != null) {
            val sIssue = selectedIssueOnMap!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_map_issue_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(sIssue.category),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = sIssue.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp)
                            )
                        }
                        PriorityBadge(sIssue.priority)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(
                                text = "LOCATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = sIssue.location,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(modifier = Modifier.weight(0.9f)) {
                            Text(
                                text = "COORDINATES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = String.format(Locale.US, "%.3f, %.3f", sIssue.latitude, sIssue.longitude),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // View Full Ticket
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Detail(sIssue.id)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Explore Ticket Details", fontSize = 11.sp)
                        }

                        // Add issue right here shortcut
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screen.Report) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(imageVector = Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Here", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CivicImpactScreen(viewModel: CivicViewModel) {
    val issues by viewModel.issues.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val totalIssues = issues.size
    val resolvedIssues = issues.count { it.status == "RESOLVED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Impact & Analytics",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "AI-powered municipal telemetry, community ROI, and risk forecasts.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Key ROI Metrics Row Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Funds Saved Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_funds_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFF137333),
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFE6F4EA), CircleShape)
                            .padding(6.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Muni ROI Saved",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$184,320",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "+14.2% this week",
                        fontSize = 9.sp,
                        color = Color(0xFF137333),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Citizen Hours Mobilized Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_hours_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(6.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Citizen Labor",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "1,420 Hours",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "214 Active members",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Dynamic Graphical Resolution Rate
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resolution_analytics_chart"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekly Civic Resolution Rate",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Active issues reported vs validated by community nodes",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Render Canvas Chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    
                    val points = listOf(
                        0.2f, 0.35f, 0.15f, 0.6f, 0.45f, 0.8f, 0.95f
                    )
                    val path = androidx.compose.ui.graphics.Path()
                    
                    val stepX = width / (points.size - 1)
                    
                    points.forEachIndexed { index, yValue ->
                        val currentX = index * stepX
                        val currentY = height - (yValue * (height - 30f))
                        if (index == 0) {
                            path.moveTo(currentX, currentY)
                        } else {
                            path.lineTo(currentX, currentY)
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = Color(0xFF137333),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                    )

                    // Draw dot anchors
                    points.forEachIndexed { index, yValue ->
                        val currentX = index * stepX
                        val currentY = height - (yValue * (height - 30f))
                        drawCircle(
                            color = Color(0xFF1B5E20),
                            radius = 8f,
                            center = androidx.compose.ui.geometry.Offset(currentX, currentY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = androidx.compose.ui.geometry.Offset(currentX, currentY)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                        Text(day, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Predictive Civic Insights Section
        Text(
            text = "🔮 AI Predictive Insights",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("predictive_insights_card"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Infrastructure Risk Surge Alerts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "CivicResolve AI model analyzes historical reports, seasonal weather variables, and citizen validation density to forecast infrastructure stress points.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                // Forecast Row 1
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFCE8E6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFFC5221F), modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("District 4: Water Pipe Stress", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFCE8E6)) {
                                Text("89% Risk", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFC5221F), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = "Temp drops followed by high localized usage patterns around East Hill Circle suggest pipeline fatigue. Pre-allocation of copper sleeve repair parts scheduled.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                // Forecast Row 2
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF7E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFB06000), modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Oakridge Sub-Station: Transformer Wear", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEF7E0)) {
                                Text("74% Risk", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFB06000), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = "A cluster of interactive lamp repair requests indicates substation secondary surges. Automated preventative electrical sweep dispatched to public teams.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun CivicPetitionsScreen(viewModel: CivicViewModel, petitions: List<com.example.ui.viewmodel.MicroPetition>) {
    if (petitions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Micro-Petitions Yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When multiple issues of the same type accumulate in a district, the system automatically bundles them into formal micro-petitions to engage municipal representatives.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("micro_petitions_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "📣 Digital Town Hall Platform",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "When issues in the same sector accumulate critical endorsements, the system escalates them into a micro-petition. Endorsing petitions schedules localized Town Halls with your District Representative.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(petitions, key = { it.id }) { petition ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = petition.category.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (petition.status == "TOWN_HALL_SCHEDULED") {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE6F4EA), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF137333), CircleShape)
                                        )
                                        Text(
                                            text = "TOWN HALL READY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF137333)
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF7E0), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "PROPOSED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFB06000)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = petition.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "District Sector: ${petition.location} • Lead: ${petition.representativeName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = petition.description,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Signature Progress Bar
                        val progressFraction = (petition.signatureCount.toFloat() / petition.requiredSignatures.toFloat()).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Community Endorsement Progress",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${petition.signatureCount} / ${petition.requiredSignatures} signs",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (petition.status == "TOWN_HALL_SCHEDULED") Color(0xFF137333) else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        if (petition.status == "TOWN_HALL_SCHEDULED") {
                            Button(
                                onClick = {
                                    viewModel.navigateTo(
                                        Screen.TownHall(
                                            petitionId = petition.id,
                                            category = petition.category,
                                            location = petition.location,
                                            representativeName = petition.representativeName
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF137333)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enter Local Digital Town Hall", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            if (petition.isSignedByMe) {
                                OutlinedButton(
                                    onClick = { },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Signed & Endorsed")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.signPetition(petition.id) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sign Micro-Petition", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CivicTownHallScreen(
    viewModel: CivicViewModel,
    petitionId: String,
    category: String,
    location: String,
    representativeName: String
) {
    val chatsMap by viewModel.townHallChats.collectAsStateWithLifecycle()
    val activeCitizen by viewModel.currentCitizen.collectAsStateWithLifecycle()

    val currentChat = chatsMap[petitionId] ?: listOf(
        TownHallMessage(
            senderName = representativeName,
            text = "Welcome, residents of $location. I am $representativeName, your district representative. Thank you for signing this micro-petition regarding our local $category issues. I am here to listen and discuss. What questions or concerns do you have?",
            isOfficial = true
        )
    )

    var inputMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header
        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Feed) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go back")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "🏛️ District Town Hall",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE6F4EA), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("LIVE", color = Color(0xFF137333), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Systemic $category Resolution • $location",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "This Town Hall is enabled by your neighborhood's active micro-petition. The representative responds dynamically using Civic NLP models.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(currentChat) { msg ->
                val isMe = msg.senderName == activeCitizen
                val align = if (isMe) Alignment.End else Alignment.Start
                val bubbleColor = when {
                    isMe -> MaterialTheme.colorScheme.primary
                    msg.isOfficial -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = when {
                    isMe -> MaterialTheme.colorScheme.onPrimary
                    msg.isOfficial -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = align
                ) {
                    Text(
                        text = if (msg.isOfficial) "${msg.senderName} (Official)" else msg.senderName,
                        fontSize = 10.sp,
                        fontWeight = if (msg.isOfficial) FontWeight.Bold else FontWeight.Normal,
                        color = if (msg.isOfficial) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        ),
                        color = bubbleColor,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = msg.text,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // Input field
        Surface(
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Ask your representative a question...", fontSize = 13.sp) },
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("town_hall_input")
                )

                IconButton(
                    onClick = {
                        val txt = inputMessage.trim()
                        if (txt.isNotBlank()) {
                            viewModel.sendTownHallMessage(
                                petitionId = petitionId,
                                text = txt,
                                repName = representativeName,
                                category = category,
                                location = location
                            )
                            inputMessage = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("town_hall_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
