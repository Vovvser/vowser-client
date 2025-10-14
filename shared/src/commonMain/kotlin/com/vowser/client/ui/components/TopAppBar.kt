package com.vowser.client.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vowser.client.ui.screens.AppScreen
import com.vowser.client.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    isLoggedIn: Boolean,
    onScreenChange: (AppScreen) -> Unit,
    onLogin: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "Vowser", fontSize = AppTheme.Typography.titleLarge,
                modifier = Modifier.padding(start = AppTheme.Dimensions.paddingSmall)
            )
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = AppTheme.Dimensions.paddingSmall)
            ) {
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
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericAppBar(
    title: String,
    onBackPress: () -> Unit
) {
    TopAppBar(
        title = { Text(title, fontSize = AppTheme.Typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBackPress) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    )
}