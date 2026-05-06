package com.pascagames.photostudio

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp


const val TAG = "PHOTO"

// ------------------------------------------------------------------------------------------------
// SettingSwitch
// ------------------------------------------------------------------------------------------------
@Composable
fun SettingSwitch(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    ListItem(
        modifier = modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface),
        tonalElevation = 2.dp,
        headlineContent = { Text(label) },
        supportingContent = {
            if (description != null) {
                Text(description)
            }
        },
        trailingContent = {
            Switch(
                checked = value,
                onCheckedChange = onValueChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),   // verde acceso
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    )
}

// ------------------------------------------------------------------------------------------------
// TopBar
// ------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(title: String, callback: (Unit) -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(title,
                maxLines = 1)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Blue,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = {callback(Unit)}) {
                Icon(
                    painter = painterResource(R.drawable.previous),
                    contentDescription = "")
            }
        },
    )
}
