package com.example.qrcodescanner

data class UserResponse(
    val message: String,
    val count: Int,
    val data: List<User>
)
