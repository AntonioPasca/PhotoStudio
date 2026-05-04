package com.pascagames.photostudio

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

const val TAG = "PHOTO"

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
