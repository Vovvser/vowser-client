package com.vowser.client.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.vowser.client.ui.screens.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    isLoggedIn: Boolean,
    onScreenChange: (AppScreen) -> Unit,
    onLogin: () -> Unit
) {
    TopAppBar(
        title = { Text("Vowser", fontSize = 24.sp) },
        actions = {
            IconButton(onClick = {
                if (isLoggedIn) {
                    onScreenChange(AppScreen.USER)
                } else {
                    onLogin()
                }
            }) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "User Profile")
            }
            IconButton(onClick = { onScreenChange(AppScreen.SETTINGS) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericAppBar(
    title: String,
    onBackPress: () -> Unit
) {
    TopAppBar(
        title = { Text(title, fontSize = 24.sp) },
        navigationIcon = {
            IconButton(onClick = onBackPress) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}