package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.search.OmniboxSuggestion
import com.example.search.SuggestionType
import com.example.tabs.BrowserTab
import com.example.ui.theme.CrumiaBrandBrush
import com.example.ui.theme.CrumiaCyan
import com.example.ui.theme.CrumiaIndigo
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.ShieldEmerald

/**
 * Minimal Chrome-style top bar displayed on the Homepage:
 * Left: Home icon | Right: Ad Shield indicator, [N] Tab switcher box, 3-dot Menu.
 */
@Composable
fun HomepageChromeTopBar(
    tabCount: Int,
    isIncognito: Boolean,
    adShieldEnabled: Boolean,
    totalBlockedCount: Long,
    onHome: () -> Unit,
    onOpenAdShield: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onHome,
            modifier = Modifier.size(48.dp).testTag("home_top_home_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = stringResource(id = R.string.cd_home),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Compact Ad Shield status circle/pill (sits cleanly where Chrome places the profile chip)
            Surface(
                shape = CircleShape,
                color = if (adShieldEnabled) {
                    ShieldEmerald.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onOpenAdShield)
                    .testTag("toolbar_ad_shield_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = stringResource(id = R.string.cd_ad_shield),
                        tint = if (adShieldEnabled) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    if (totalBlockedCount > 0 && adShieldEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCompactCount(totalBlockedCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = ShieldEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            TabCountButton(
                tabCount = tabCount,
                isIncognito = isIncognito,
                onClick = onOpenTabs
            )

            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.size(46.dp).testTag("toolbar_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(id = R.string.cd_menu),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

/**
 * Chrome-style single-row Omnibox Toolbar when viewing a webpage.
 */
@Composable
fun BrowserToolbar(
    tab: BrowserTab,
    tabCount: Int,
    adShieldEnabled: Boolean,
    suggestions: List<OmniboxSuggestion>,
    voiceSearchAvailable: Boolean,
    onNavigate: (String) -> Unit,
    onAddressInputChanged: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenAdShield: () -> Unit,
    onOpenPageInfo: () -> Unit,
    onSwipeTabNext: () -> Unit = {},
    onSwipeTabPrev: () -> Unit = {},
    onVoiceSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var swipeDragAccum by remember { mutableFloatStateOf(0f) }

    val displayUrl = if (tab.isHomepage) "" else tab.url
    var fieldValue by remember(tab.id, displayUrl) {
        mutableStateOf(TextFieldValue(displayUrl))
    }

    LaunchedEffect(displayUrl, isFocused) {
        if (!isFocused) {
            fieldValue = TextFieldValue(displayUrl)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onHome,
                        modifier = Modifier.size(42.dp).testTag("nav_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = stringResource(id = R.string.cd_home),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Chrome-style rounded Omnibox Pill (supports horizontal swipe to switch tabs & tap lock for Site Info)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .border(
                                width = if (isFocused) 1.5.dp else 0.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(22.dp)
                            )
                            .pointerInput(isFocused, tab.id) {
                                if (!isFocused) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { swipeDragAccum = 0f },
                                        onHorizontalDrag = { _, dragAmount -> swipeDragAccum += dragAmount },
                                        onDragEnd = {
                                            when {
                                                swipeDragAccum > 90f -> onSwipeTabPrev()
                                                swipeDragAccum < -90f -> onSwipeTabNext()
                                            }
                                            swipeDragAccum = 0f
                                        },
                                        onDragCancel = { swipeDragAccum = 0f }
                                    )
                                }
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenPageInfo)
                                .testTag("omnibox_security_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tab.isSecure) Icons.Rounded.Lock else Icons.Rounded.WarningAmber,
                                contentDescription = stringResource(
                                    id = if (tab.isSecure) R.string.cd_security_https else R.string.cd_security_http
                                ),
                                tint = if (tab.isSecure) ShieldEmerald else SecurityAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (fieldValue.text.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.search_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = fieldValue,
                                onValueChange = { updated ->
                                    fieldValue = updated
                                    onAddressInputChanged(updated.text)
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        val query = fieldValue.text.trim()
                                        if (query.isNotEmpty()) onNavigate(query)
                                        onClearSuggestions()
                                        focusManager.clearFocus()
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { state ->
                                        isFocused = state.isFocused
                                        if (state.isFocused && fieldValue.text.isNotEmpty()) {
                                            fieldValue = fieldValue.copy(
                                                selection = TextRange(0, fieldValue.text.length)
                                            )
                                        } else if (!state.isFocused) {
                                            onClearSuggestions()
                                        }
                                    }
                                    .testTag("address_bar_input")
                            )
                        }

                        if (isFocused && fieldValue.text.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    fieldValue = TextFieldValue("")
                                    onAddressInputChanged("")
                                },
                                modifier = Modifier.size(32.dp).testTag("clear_address_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(id = R.string.cd_clear_input),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (isFocused && fieldValue.text.isEmpty()) {
                            val clipText = clipboard.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                IconButton(
                                    onClick = {
                                        fieldValue = TextFieldValue(clipText, TextRange(clipText.length))
                                        onAddressInputChanged(clipText)
                                    },
                                    modifier = Modifier.size(32.dp).testTag("paste_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            } else if (voiceSearchAvailable) {
                                IconButton(
                                    onClick = onVoiceSearch,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = stringResource(id = R.string.cd_voice_search),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = onOpenAdShield,
                                modifier = Modifier.size(30.dp).testTag("toolbar_ad_shield_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = stringResource(id = R.string.cd_ad_shield),
                                    tint = if (adShieldEnabled) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Chrome-style '+' New Tab button next to Tab Counter & Overflow Menu
                    IconButton(
                        onClick = onNewTab,
                        modifier = Modifier.size(40.dp).testTag("nav_new_tab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(id = R.string.cd_new_tab),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    TabCountButton(
                        tabCount = tabCount,
                        isIncognito = tab.isIncognito,
                        onClick = onOpenTabs
                    )

                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.size(42.dp).testTag("toolbar_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(id = R.string.cd_menu)
                        )
                    }
                }

                // Loading progress bar
                if (tab.isLoading) {
                    val fraction = (tab.progress.coerceIn(5, 100) / 100f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(2.5.dp)
                                .background(CrumiaBrandBrush)
                        )
                    }
                }
            }
        }

        // Omnibox Suggestions List
        AnimatedVisibility(visible = isFocused && suggestions.isNotEmpty()) {
            SuggestionsDropdown(
                suggestions = suggestions,
                onSelect = { target ->
                    fieldValue = TextFieldValue(target, TextRange(target.length))
                    onNavigate(target)
                    onClearSuggestions()
                    focusManager.clearFocus()
                }
            )
        }
    }
}

/**
 * Fullscreen Chrome-style Omnibox search overlay launched when tapping the Homepage Search Pill.
 */
@Composable
fun OmniboxSearchOverlay(
    initialQuery: String = "",
    searchEngineName: String,
    suggestions: List<OmniboxSuggestion>,
    voiceSearchAvailable: Boolean,
    onQueryChanged: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf(initialQuery) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("omnibox_search_overlay")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Search $searchEngineName or type URL",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    onQueryChanged(it)
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        if (text.isNotBlank()) onSubmit(text)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .testTag("omnibox_overlay_input")
                            )
                        }
                        if (text.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    text = ""
                                    onQueryChanged("")
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        } else {
                            val clip = clipboard.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                IconButton(
                                    onClick = {
                                        text = clip
                                        onQueryChanged(clip)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                                }
                            }
                            if (voiceSearchAvailable) {
                                IconButton(onClick = onVoiceSearch, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Mic, contentDescription = "Voice search", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            SuggestionsDropdown(
                suggestions = suggestions,
                onSelect = { target -> onSubmit(target) },
                maxHeightDp = 600
            )
        }
    }
}

@Composable
private fun SuggestionsDropdown(
    suggestions: List<OmniboxSuggestion>,
    onSelect: (String) -> Unit,
    maxHeightDp: Int = 290
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeightDp.dp)
        ) {
            items(suggestions) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item.url ?: item.text) }
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (item.type) {
                            SuggestionType.BOOKMARK -> Icons.Rounded.Bookmark
                            SuggestionType.HISTORY -> Icons.Rounded.History
                            SuggestionType.DIRECT_URL -> Icons.Rounded.Public
                            SuggestionType.AUTOCOMPLETE -> Icons.Rounded.Search
                        },
                        contentDescription = null,
                        tint = when (item.type) {
                            SuggestionType.BOOKMARK -> CrumiaIndigo
                            SuggestionType.DIRECT_URL -> CrumiaCyan
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!item.subtitle.isNullOrBlank()) {
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun TabCountButton(
    tabCount: Int,
    isIncognito: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
            .testTag("toolbar_tabs_button"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 1.8.dp,
                    color = if (isIncognito) CrumiaCyan else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (tabCount > 99) ":D" else tabCount.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIncognito) CrumiaCyan else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FindInPageBar(
    query: String,
    matches: Pair<Int, Int>,
    onQueryChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Find in page…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("find_in_page_input")
                )
            }
            Text(
                text = "${matches.first}/${matches.second}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp).testTag("find_prev_button")) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Previous match")
            }
            IconButton(onClick = onNext, modifier = Modifier.size(36.dp).testTag("find_next_button")) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Next match")
            }
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp).testTag("find_close_button")) {
                Icon(Icons.Rounded.Close, contentDescription = "Close find in page")
            }
        }
    }
}

private fun formatCompactCount(count: Long): String = when {
    count >= 1000L -> "${count / 1000L}k"
    else -> count.toString()
}
