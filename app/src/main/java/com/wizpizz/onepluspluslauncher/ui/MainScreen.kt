package com.wizpizz.onepluspluslauncher.ui

import android.content.SharedPreferences
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.wizpizz.onepluspluslauncher.BuildConfig
import com.wizpizz.onepluspluslauncher.LAUNCHER_PACKAGE
import com.wizpizz.onepluspluslauncher.R
import com.wizpizz.onepluspluslauncher.preferences.FeaturePreferences
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class FeatureSetting(
    val key: String,
    val titleResource: Int,
    val descriptionResource: Int,
)

private val featureSettings = listOf(
    FeatureSetting(
        FeaturePreferences.SWIPE_UP_AUTOFOCUS,
        R.string.swipe_up_autofocus,
        R.string.swipe_up_autofocus_description,
    ),
    FeatureSetting(
        FeaturePreferences.ENTER_KEY_LAUNCH,
        R.string.enter_key_launch,
        R.string.enter_key_launch_description,
    ),
    FeatureSetting(
        FeaturePreferences.RANKED_FUZZY_SEARCH,
        R.string.ranked_fuzzy_search,
        R.string.ranked_fuzzy_search_description,
    ),
    FeatureSetting(
        FeaturePreferences.GLOBAL_SEARCH_REDIRECT,
        R.string.global_search_redirect,
        R.string.global_search_redirect_description,
    ),
)

@Composable
fun MainScreen(service: XposedService?, scopeRefreshEvent: Long) {
    val preferences = remember(service) {
        service?.getRemotePreferences(FeaturePreferences.GROUP)
    }
    var values by remember(preferences) {
        mutableStateOf(featureSettings.associate { it.key to preferences.read(it.key) })
    }
    var moduleStatus by remember(service) {
        mutableStateOf(
            if (service == null) ModuleStatus.FRAMEWORK_UNAVAILABLE
            else ModuleStatus.CHECKING_SCOPE,
        )
    }

    LaunchedEffect(service, scopeRefreshEvent) {
        moduleStatus = if (service == null) {
            ModuleStatus.FRAMEWORK_UNAVAILABLE
        } else {
            withContext(Dispatchers.IO) {
                runCatching { service.scope }
                    .fold(
                        onSuccess = { scope ->
                            if (LAUNCHER_PACKAGE in scope) ModuleStatus.READY
                            else ModuleStatus.LAUNCHER_NOT_IN_SCOPE
                        },
                        onFailure = { ModuleStatus.SCOPE_CHECK_FAILED },
                    )
            }
        }
    }

    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
            if (key != null && key in FeaturePreferences.keys) {
                values = values + (key to changed.read(key))
            }
        }
        preferences?.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences?.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                ModuleHeader()
                Spacer(Modifier.height(32.dp))
                StatusCard(moduleStatus)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.launcher_enhancements),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                featureSettings.forEachIndexed { index, setting ->
                    SettingRow(
                        title = stringResource(setting.titleResource),
                        description = stringResource(setting.descriptionResource),
                        checked = values.getValue(setting.key),
                        enabled = preferences != null,
                        onCheckedChange = { enabled ->
                            values = values + (setting.key to enabled)
                            preferences?.edit { putBoolean(setting.key, enabled) }
                        },
                    )
                    if (index != featureSettings.lastIndex) Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = stringResource(
                        R.string.version_summary,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.SUPPORTED_LAUNCHER_VERSION,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun ModuleHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusCard(status: ModuleStatus) {
    val (container, content) = when (status) {
        ModuleStatus.READY -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        ModuleStatus.LAUNCHER_NOT_IN_SCOPE -> MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to
            MaterialTheme.colorScheme.onSurface
    }
    val (title, description) = when (status) {
        ModuleStatus.FRAMEWORK_UNAVAILABLE ->
            R.string.framework_unavailable to R.string.framework_unavailable_description
        ModuleStatus.CHECKING_SCOPE ->
            R.string.checking_scope to R.string.checking_scope_description
        ModuleStatus.LAUNCHER_NOT_IN_SCOPE ->
            R.string.launcher_not_in_scope to R.string.launcher_not_in_scope_description
        ModuleStatus.SCOPE_CHECK_FAILED ->
            R.string.scope_check_failed to R.string.scope_check_failed_description
        ModuleStatus.READY -> R.string.ready to R.string.ready_description
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(title),
                color = content,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(description),
                color = content,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(checked = checked, enabled = enabled, onCheckedChange = null)
        }
    }
}

private enum class ModuleStatus {
    FRAMEWORK_UNAVAILABLE,
    CHECKING_SCOPE,
    LAUNCHER_NOT_IN_SCOPE,
    SCOPE_CHECK_FAILED,
    READY,
}

private fun SharedPreferences?.read(key: String): Boolean =
    this?.getBoolean(key, FeaturePreferences.DEFAULT_ENABLED) ?: FeaturePreferences.DEFAULT_ENABLED
