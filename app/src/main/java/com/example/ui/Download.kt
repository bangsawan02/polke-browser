package com.example.ui

data class Download(
    val id: Long,
    val filename: String,
    val url: String,
    val progress: Float,
    val status: String,
    val totalSize: String
)
