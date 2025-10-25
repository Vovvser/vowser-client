package com.vowser.client.browserautomation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SelectOption(
    val value: String,
    val label: String,
    @SerialName("isSelected")
    val isSelected: Boolean = false
)
