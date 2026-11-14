package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adshield.AdShieldEngine
import com.example.data.db.FilterRuleEntity
import com.example.data.db.SiteSettingEntity
import com.example.data.model.FilterCategory
import com.example.data.model.FilterListSource
import com.example.data.prefs.CrumiaPreferences
import com.example.ui.theme.CrumiaCyan
import com.example.ui.theme.CrumiaIndigo
import com.example.ui.theme.CrumiaViolet
import com.example.ui.theme.ShieldEmerald
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdShieldScreen(
    prefs: CrumiaPreferences,
    currentHost: String?,
    isCurrentSiteAllowlisted: Boolean,
    allowlistedSites: List<SiteSettingEntity>,
    filterRules: List<FilterRuleEntity>,
    isUpdatingFilters: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    onToggleAds: (Boolean) -> Unit,
    onToggleTrackers: (Boolean) -> Unit,
    onTogglePopups: (Boolean) -> Unit,
    onToggleCurrentSiteAllowlist: () -> Unit,
    onRemoveAllowlistHost: (String) -> Unit,
    onAddAllowlistHost: (String) -> Unit,
    onToggleFilterSource: (FilterListSource, Boolean) -> Unit,
    onManualUpdate: () -> Unit,
    onAddCustomRule: (pattern: String, category: FilterCategory) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onResetSettings: () -> Unit,
    onBack: () -> Unit
) {
    var addAllowlistDialog by remember { mutableStateOf(false) }
    var addRuleDialog by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    val customRules = remember(filterRules) {
        filterRules.filter { it.listSource == FilterListSource.CUSTOM.name }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Crumia Ad Shield") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ad_shield_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.testTag("ad_shield_reset_button")
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset Ad Shield")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Status & Stats Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    if (prefs.adShieldEnabled) {
                                        listOf(
                                            ShieldEmerald.copy(alpha = 0.22f),
                                            CrumiaCyan.copy(alpha = 0.16f),
                                            CrumiaIndigo.copy(alpha = 0.18f)
                                        )
                                    } else {
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (prefs.adShieldEnabled) ShieldEmerald.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = if (prefs.adShieldEnabled) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (prefs.adShieldEnabled) "Protection Active" else "Protection Paused",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${filterRules.count { it.isEnabled }} active filter rules loaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.adShieldEnabled,
                                onCheckedChange = onToggleMaster,
                                modifier = Modifier.testTag("ad_shield_master_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatTile(
                                label = "Ads Blocked",
                                count = prefs.totalAdsBlocked,
                                icon = Icons.Rounded.Block,
                                accent = CrumiaCyan,
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = "Trackers Blocked",
                                count = prefs.totalTrackersBlocked,
                                icon = Icons.Rounded.TrackChanges,
                                accent = CrumiaViolet,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Counts reflect real network requests intercepted by Ad Shield. Android System WebView blocks third-party ad/tracker requests and hides common banner slots; embedded first-party stream ads cannot always be blocked at the WebView layer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Current Website Control
            if (!currentHost.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = CrumiaCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currentHost, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isCurrentSiteAllowlisted) "Ad Shield paused on this site" else "Ad Shield active on this site",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = !isCurrentSiteAllowlisted,
                                onCheckedChange = { onToggleCurrentSiteAllowlist() },
                                modifier = Modifier.testTag("current_site_shield_switch")
                            )
                        }
                    }
                }
            }

            // Blocking Controls
            item {
                Text(
                    text = "Blocking Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ToggleRow(
                            title = "Advertisement Blocking",
                            subtitle = "Block ad networks, banners, and video ad scripts",
                            checked = prefs.blockAds,
                            enabled = prefs.adShieldEnabled,
                            onCheckedChange = onToggleAds,
                            tag = "toggle_block_ads"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ToggleRow(
                            title = "Tracker Blocking",
                            subtitle = "Block analytics, pixels, and cross-site trackers",
                            checked = prefs.blockTrackers,
                            enabled = prefs.adShieldEnabled,
                            onCheckedChange = onToggleTrackers,
                            tag = "toggle_block_trackers"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ToggleRow(
                            title = "Pop-up & Redirect Blocking",
                            subtitle = "Prevent unsolicited pop-up windows and ad redirects",
                            checked = prefs.blockPopups,
                            enabled = prefs.adShieldEnabled,
                            onCheckedChange = onTogglePopups,
                            tag = "toggle_block_popups"
                        )
                    }
                }
            }

            // Filter Lists Management
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Filter Lists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val updatedText = if (prefs.lastFilterUpdateMillis > 0L) {
                            "Updated ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(prefs.lastFilterUpdateMillis))}"
                        } else {
                            "Built-in baseline active"
                        }
                        Text(
                            text = updatedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onManualUpdate,
                        enabled = !isUpdatingFilters,
                        modifier = Modifier.testTag("manual_update_filters_button")
                    ) {
                        if (isUpdatingFilters) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Updating")
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update Now")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ToggleRow(
                            title = "EasyList",
                            subtitle = "Primary advertisement and banner network list",
                            checked = prefs.easyListEnabled,
                            enabled = prefs.adShieldEnabled,
                            onCheckedChange = { onToggleFilterSource(FilterListSource.EASYLIST, it) },
                            tag = "toggle_easylist"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ToggleRow(
                            title = "EasyPrivacy",
                            subtitle = "Third-party trackers, beacons, and telemetry",
                            checked = prefs.easyPrivacyEnabled,
                            enabled = prefs.adShieldEnabled,
                            onCheckedChange = { onToggleFilterSource(FilterListSource.EASYPRIVACY, it) },
                            tag = "toggle_easyprivacy"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ToggleRow(
                            title = "AdGuard Base & Mobile",
                            subtitle = "Mobile ad servers and pop-under protections",
                            checked = prefs.adGuardEnabled,
                            enabled = prefs.adShieldEnabled,
                            onCheckedChange = { onToggleFilterSource(FilterListSource.ADGUARD, it) },
                            tag = "toggle_adguard"
                        )
                    }
                }
            }

            // Website Allowlist
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Website Allowlist (${allowlistedSites.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { addAllowlistDialog = true },
                        modifier = Modifier.testTag("add_allowlist_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Site")
                    }
                }
            }

            if (allowlistedSites.isEmpty()) {
                item {
                    Text(
                        text = "No websites allowlisted. Ad Shield protects all sites.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(allowlistedSites, key = { it.host }) { site ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(site.host, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemoveAllowlistHost(site.host) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove from allowlist")
                            }
                        }
                    }
                }
            }

            // Custom Rules
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Block Rules (${customRules.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { addRuleDialog = true },
                        modifier = Modifier.testTag("add_custom_rule_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Rule")
                    }
                }
            }

            items(customRules, key = { it.id }) { rule ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.pattern, style = MaterialTheme.typography.bodyMedium)
                            Text(rule.category, style = MaterialTheme.typography.labelSmall, color = CrumiaCyan)
                        }
                        IconButton(onClick = { onDeleteRule(rule.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete custom rule")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (addAllowlistDialog) {
        var hostInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addAllowlistDialog = false },
            title = { Text("Allowlist Website") },
            text = {
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("Domain (e.g. example.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = { addAllowlistDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(onClick = {
                    val clean = AdShieldEngine.normalizeHost(hostInput)
                    if (clean != null) {
                        onAddAllowlistHost(clean)
                    }
                    addAllowlistDialog = false
                }) { Text("Allow") }
            }
        )
    }

    if (addRuleDialog) {
        var domainInput by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(FilterCategory.AD) }
        AlertDialog(
            onDismissRequest = { addRuleDialog = false },
            title = { Text("Add Custom Block Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it },
                        label = { Text("Domain (e.g. ads.example.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(FilterCategory.AD, FilterCategory.TRACKER).forEachIndexed { idx, cat ->
                            SegmentedButton(
                                selected = category == cat,
                                onClick = { category = cat },
                                shape = SegmentedButtonDefaults.itemShape(idx, 2)
                            ) {
                                Text(cat.name)
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { addRuleDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(onClick = {
                    if (domainInput.isNotBlank()) {
                        onAddCustomRule(domainInput, category)
                    }
                    addRuleDialog = false
                }) { Text("Block") }
            }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset Ad Shield?") },
            text = { Text("This restores default EasyList, EasyPrivacy, and AdGuard rules and resets blocked counters.") },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
            confirmButton = {
                Button(onClick = {
                    confirmReset = false
                    onResetSettings()
                }) { Text("Reset") }
            }
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    count: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
