// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.3.0
//
// Date:        May 2026
//
// Module:      CommonComposables
// --------------------------------------------------------------------------
//
// Common Components
//
//      SettingSwitch
package com.pascagames.photostudio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// --------------------------------------------------------------------
//  Componenents
//      fun NumericUpDown(value: Int, onValueChange: (Int) -> Unit,
//                      min: Int,  max: Int)
//      fun SettingSwitch(label: String, value: Boolean, onValueChange: (Boolean) -> Unit,
//                        modifier: Modifier = Modifier, description: String? = null
//      fun TopBar(title: String, callback: (Unit) -> Unit)
// --------------------------------------------------------------------
@Composable
fun NumericUpDown(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    description: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
           text = description
        )
        IconButton(
            onClick = { if (value > min) onValueChange(value - 1) }
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrementa")
        }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium
        )

        IconButton(
            onClick = { if (value < max) onValueChange(value + 1) }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Incrementa")
        }
    }
}

// --------------------------------------------------------------------
// SettingSwitch
// --------------------------------------------------------------------
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
                    checkedTrackColor = Color(0xFF4CAF50),   // Green
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    )
}

// --------------------------------------------------------------------
// TopBar
// --------------------------------------------------------------------
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
