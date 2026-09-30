package com.example.jarvis.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.search.SearchActionType
import com.example.jarvis.search.SearchSource
import com.example.jarvis.search.UniversalSearchResult
import com.example.jarvis.search.UniversalSearchService
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisSurfaceElevated
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UniversalSearchScreen(
    repository: JarvisRepository,
    bridge: AndroidBridge,
    initialQuery: String = "",
    onOpenSubScreen: (com.example.jarvis.ui.SubScreen) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val searchService = remember(context, repository, bridge) {
        UniversalSearchService(context, repository, bridge)
    }

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var selectedSource by remember { mutableStateOf(SearchSource.ALL) }
    var results by remember { mutableStateOf<List<UniversalSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    // Deep Research State
    var isDeepResearching by remember { mutableStateOf(false) }
    var deepResearchProgress by remember { mutableStateOf("") }
    var deepResearchResult by remember { mutableStateOf<com.example.jarvis.search.deep.DeepResearchResult?>(null) }
    var deepResearchJob by remember { mutableStateOf<Job?>(null) }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        // Re-execute search if granted
        if (searchQuery.isNotBlank()) {
            coroutineScope.launch {
                isSearching = true
                results = searchService.search(searchQuery, selectedSource)
                isSearching = false
            }
        }
    }

    fun executeSearch(q: String, src: SearchSource) {
        searchJob?.cancel()
        if (q.isBlank()) {
            results = emptyList()
            isSearching = false
            return
        }
        searchJob = coroutineScope.launch {
            delay(200) // Debounce typing by 200ms
            isSearching = true
            results = searchService.search(q, src)
            isSearching = false
        }
    }

    // Trigger initial search if passed
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            executeSearch(initialQuery, selectedSource)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Header & Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UNIVERSAL SEARCH",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = JarvisTextPrimary
                )
                Text(
                    text = "SYSTEM-WIDE UNIFIED RETRIEVAL MATRIX",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = JarvisCyan
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0C1422))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${results.size} MATCHES",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                actionFeedback = null
                executeSearch(it, selectedSource)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("universal_search_input"),
            placeholder = {
                Text(
                    text = "Search apps, contacts, tasks, memories...",
                    fontSize = 13.sp,
                    color = JarvisTextDim
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = JarvisCyan,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        results = emptyList()
                        actionFeedback = null
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = JarvisTextDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = JarvisSurfaceElevated,
                unfocusedContainerColor = Color(0xFF080D1A),
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisBorderSubtle,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                executeSearch(searchQuery, selectedSource)
            })
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Source Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchSource.values()) { src ->
                val isSelected = src == selectedSource
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) JarvisCyan.copy(alpha = 0.2f) else Color(0xFF090E1A))
                        .border(
                            0.5.dp,
                            if (isSelected) JarvisCyan else JarvisBorderSubtle,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedSource = src
                            executeSearch(searchQuery, src)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("filter_chip_${src.name.lowercase()}")
                ) {
                    Text(
                        text = "${src.badge} ${src.displayName}",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) JarvisCyanBright else JarvisTextSecondary
                    )
                }
            }
        }

        // Deep Research Quick Action & Card
        if (searchQuery.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEEP INVESTIGATION",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )
                Button(
                    onClick = {
                        if (isDeepResearching) {
                            deepResearchJob?.cancel()
                            isDeepResearching = false
                            deepResearchProgress = "Research cancelled."
                        } else {
                            isDeepResearching = true
                            deepResearchProgress = "Initializing multi-round research plan..."
                            deepResearchResult = null
                            deepResearchJob = coroutineScope.launch {
                                try {
                                    val engine = com.example.jarvis.search.deep.DeepResearchEngine(bridge = bridge)
                                    val res = engine.executeDeepResearch(
                                        rawQuestion = searchQuery,
                                        depth = com.example.jarvis.search.deep.ResearchDepth.STANDARD,
                                        onProgress = { deepResearchProgress = it }
                                    )
                                    deepResearchResult = res
                                    isDeepResearching = false
                                } catch (_: Exception) {
                                    isDeepResearching = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDeepResearching) JarvisAmber else JarvisCyan.copy(alpha = 0.25f),
                        contentColor = if (isDeepResearching) Color.Black else JarvisCyanBright
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (isDeepResearching) "⏹ CANCEL" else "🔬 RUN DEEP RESEARCH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Deep Research Progress or Result Card
        if (isDeepResearching || deepResearchResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0C1626))
                    .border(1.dp, if (isDeepResearching) JarvisCyan else JarvisGreen, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isDeepResearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = JarvisCyan
                                )
                                Text(
                                    text = "RESEARCHING...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = JarvisCyan
                                )
                            } else {
                                Text(
                                    text = "✅ DOSSIER READY (${deepResearchResult?.sources?.size ?: 0} SOURCES)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = JarvisGreen
                                )
                            }
                        }

                        if (isDeepResearching) {
                            Text(
                                text = "MULTI-ROUND",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextDim
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    if (isDeepResearching) {
                        Text(
                            text = deepResearchProgress,
                            fontSize = 11.sp,
                            color = JarvisCyanBright,
                            fontFamily = FontFamily.Monospace
                        )
                    } else if (deepResearchResult != null) {
                        val res = deepResearchResult!!
                        Text(
                            text = res.directAnswer,
                            fontSize = 12.sp,
                            color = JarvisTextPrimary,
                            lineHeight = 16.sp
                        )
                        if (res.keyFindings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            res.keyFindings.take(3).forEach { finding ->
                                Text(
                                    text = "• $finding",
                                    fontSize = 11.sp,
                                    color = JarvisCyanBright,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Action Feedback / Notification
        if (actionFeedback != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1E2E))
                    .border(0.5.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = actionFeedback ?: "",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyanBright
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 5. Contacts Permission Notice Banner (if Contacts selected or Contacts permission denied)
        if (!hasContactsPermission && (selectedSource == SearchSource.CONTACTS || selectedSource == SearchSource.ALL)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1408))
                    .border(0.5.dp, JarvisAmber.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = JarvisAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Contacts permission needed to index phone contacts.",
                            fontSize = 11.sp,
                            color = JarvisAmber,
                            lineHeight = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("GRANT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 6. Results Stream or States
        when {
            isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = JarvisCyan, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Auditing device indexes...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextDim
                        )
                    }
                }
            }

            searchQuery.isBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "READY FOR RETRIEVAL",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter a query or keyword to search across launchable apps, device contacts, Room memories, agenda tasks, and notification intelligence.",
                            fontSize = 12.sp,
                            color = JarvisTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            results.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No records found matching '$searchQuery'",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextDim
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting the filter chip or searching with broader terms.",
                            fontSize = 11.sp,
                            color = JarvisTextDim
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { item ->
                        SearchResultCard(
                            result = item,
                            onActionClick = {
                                when (item.actionType) {
                                    SearchActionType.OPEN_APP -> {
                                        val (success, msg) = bridge.launchAppByNameOrPackage(item.actionPayload)
                                        actionFeedback = if (success) "Launched ${item.title}" else "Failed to launch: $msg"
                                    }
                                    SearchActionType.CALL_CONTACT -> {
                                        val (success, msg) = bridge.makePhoneCall(item.actionPayload)
                                        actionFeedback = msg
                                    }
                                    SearchActionType.VIEW_TASK -> {
                                        onOpenSubScreen(com.example.jarvis.ui.SubScreen.TASKS)
                                        actionFeedback = "Focused agenda task: ${item.title}"
                                    }
                                    SearchActionType.VIEW_MEMORY -> {
                                        onOpenSubScreen(com.example.jarvis.ui.SubScreen.MEMORY)
                                        actionFeedback = "Focused Room memory: ${item.title}"
                                    }
                                    SearchActionType.VIEW_NOTIFICATION -> {
                                        onOpenSubScreen(com.example.jarvis.ui.SubScreen.NOTIFICATIONS)
                                        actionFeedback = "Focused notification source: ${item.title}"
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    result: UniversalSearchResult,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(JarvisSurfaceElevated)
            .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("result_card_${result.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Source badge icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0C1424))
                        .border(0.5.dp, JarvisCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = result.badge, fontSize = 16.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = result.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTextPrimary,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF070F1A))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = result.source.name,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = result.subtitle,
                        fontSize = 11.sp,
                        color = JarvisTextSecondary,
                        maxLines = 2,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (result.actionType) {
                        SearchActionType.OPEN_APP -> JarvisCyan
                        SearchActionType.CALL_CONTACT -> JarvisGreen
                        else -> Color(0xFF162032)
                    },
                    contentColor = when (result.actionType) {
                        SearchActionType.OPEN_APP, SearchActionType.CALL_CONTACT -> Color.Black
                        else -> JarvisCyanBright
                    }
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("action_btn_${result.id}")
            ) {
                val actionIcon = when (result.actionType) {
                    SearchActionType.OPEN_APP -> Icons.AutoMirrored.Filled.Launch
                    SearchActionType.CALL_CONTACT -> Icons.Default.Call
                    else -> Icons.Default.Visibility
                }
                val actionText = when (result.actionType) {
                    SearchActionType.OPEN_APP -> "OPEN"
                    SearchActionType.CALL_CONTACT -> "DIAL"
                    SearchActionType.VIEW_CONTACT -> "VIEW"
                    SearchActionType.VIEW_TASK -> "TASK"
                    SearchActionType.VIEW_MEMORY -> "MEM"
                    SearchActionType.VIEW_NOTIFICATION -> "VIEW"
                }
                Icon(imageVector = actionIcon, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = actionText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
