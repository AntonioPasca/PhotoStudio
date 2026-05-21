// --------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.5.0
//
// Date:        May 2026
//
// Module:      CommonComposables
// --------------------------------------------------------------------
//  UI Compose Componenents
//
//      fun CustomToast(message: String, duration: Long = 1500L)
//      fun NumericUpDown(value: Int, onValueChange: (Int) -> Unit,
//                      min: Int,  max: Int)
//      fun SettingSwitch(label: String, value: Boolean, onValueChange: (Boolean) -> Unit,
//                        modifier: Modifier = Modifier, description: String? = null
//      fun TopBar(title: String, callback: (Unit) -> Unit)
//      fun TopBarEx(title: String, callback: (Unit) -> Unit)
//      fun TopBarEx(title: String, actions: List<TopBarAction>,callback: (Unit) -> Unit)
// --------------------------------------------------------------------
package com.pascagames.photostudio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// --------------------------------------------------------------------
// CustomToast
// --------------------------------------------------------------------
@Composable
fun CustomToast(
    message: String,
    duration: Long = 1500L
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(duration)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top=160.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// --------------------------------------------------------------------
// NumericUpDown
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
            Icon(Icons.Default.Remove, contentDescription = "Decrement")
        }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium
        )

        IconButton(
            onClick = { if (value < max) onValueChange(value + 1) }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increment")
        }
    }
}

// --------------------------------------------------------------------
// PersistentMessage
// --------------------------------------------------------------------
@Composable
fun PersistentMessage(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp
        )
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

    var menuExpanded by remember { mutableStateOf(true) }

    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                maxLines = 1
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Blue,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = { callback(Unit) }) {
                Icon(
                    painter = painterResource(R.drawable.previous),
                    contentDescription = ""
                )
            }
        },
    )
}

// --------------------------------------------------------------------
// TopBarEx
// --------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarEx(
                title: String,
                actions: List<TopBarAction>,
                callback: (Unit) -> Unit) {

    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                maxLines = 1
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Blue,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = { callback(Unit) }) {
                Icon(
                    painter = painterResource(R.drawable.previous),
                    contentDescription = ""
                )
            }
        },

        actions = {
            actions.forEach { action ->
                IconButton(onClick = action.onClick) {
                    Icon(action.icon, contentDescription = action.label)
                }
            }
        }
    )
}

// --------------------------------------------------------------------
// class TopBarAction
// --------------------------------------------------------------------
sealed class TopBarAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
) {
    class Gallery(onClick: () -> Unit) :
        TopBarAction(Icons.Default.PhotoLibrary, "Gallery", onClick)

    class Settings(onClick: () -> Unit) :
        TopBarAction(Icons.Default.Settings, "Settings", onClick)

    class Help(onClick: () -> Unit) :
        TopBarAction(Icons.Default.Help, "Help", onClick)
}



