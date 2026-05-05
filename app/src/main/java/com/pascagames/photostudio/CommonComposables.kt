package com.pascagames.photostudio

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

const val TAG = "PHOTO"

@Composable
fun SettingSwitch(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(label) },
        supportingContent = {
            if (description != null) {
                Text(description)
            }
        },
        trailingContent = {
            Switch(
                checked = value,
                onCheckedChange = onValueChange
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
