package com.breathein.feature.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One app: on/off switch plus, when on, how long a session may run before the breathing break. */
@Composable
internal fun AppLimitRow(
    title: String,
    subtitle: String,
    limitSeconds: Int?,
    limitOptions: List<Int>,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onLimitChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        if (limitSeconds != null) {
            LimitPicker(limitSeconds, limitOptions, onLimitChange)
        }
        Switch(
            checked = limitSeconds != null,
            onCheckedChange = { checked -> if (checked) onEnable() else onDisable() },
            enabled = enabled,
        )
    }
}

@Composable
private fun LimitPicker(limitSeconds: Int, options: List<Int>, onLimitChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(formatLimit(limitSeconds)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { seconds ->
                DropdownMenuItem(
                    text = { Text(formatLimit(seconds)) },
                    onClick = {
                        expanded = false
                        onLimitChange(seconds)
                    },
                )
            }
        }
    }
}
