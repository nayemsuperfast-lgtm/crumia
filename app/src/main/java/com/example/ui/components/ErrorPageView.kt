package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.tabs.BrowserError
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.SecurityAmber

@Composable
fun ErrorPageView(
    error: BrowserError,
    onRetry: () -> Unit,
    onReturnHome: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector
    val title: String
    val description: String
    val accentColor = when (error) {
        is BrowserError.SslError -> {
            icon = Icons.Rounded.GppBad
            title = stringResource(id = R.string.error_ssl_title)
            description = "${stringResource(id = R.string.error_ssl_desc)}\n\n${error.reason}"
            DangerCrimson
        }
        is BrowserError.NoInternet -> {
            icon = Icons.Rounded.WifiOff
            title = stringResource(id = R.string.error_no_internet_title)
            description = stringResource(id = R.string.error_no_internet_desc)
            SecurityAmber
        }
        is BrowserError.WebsiteUnavailable -> {
            icon = Icons.Rounded.CloudOff
            title = stringResource(id = R.string.error_page_title)
            description = error.message
            MaterialTheme.colorScheme.primary
        }
        is BrowserError.PageLoadError -> {
            icon = Icons.Rounded.ErrorOutline
            title = "Page Loading Error (${error.code})"
            description = error.message
            MaterialTheme.colorScheme.secondary
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (error.url.isNotBlank()) {
                    Text(
                        text = error.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (error is BrowserError.SslError) {
                        Button(
                            onClick = onGoBack,
                            colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson),
                            modifier = Modifier.weight(1f).testTag("error_back_safety_button")
                        ) {
                            Text(stringResource(id = R.string.action_go_back_safety), maxLines = 1)
                        }
                    } else {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f).testTag("error_retry_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(id = R.string.action_retry))
                        }
                    }

                    OutlinedButton(
                        onClick = onReturnHome,
                        modifier = Modifier.weight(1f).testTag("error_home_button")
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(id = R.string.action_return_home), maxLines = 1)
                    }
                }
            }
        }
    }
}
