package com.vowser.client

data class StatusLogEntry(
    val timestamp: String,
    val message: String,
    val type: StatusLogType = StatusLogType.INFO
)
