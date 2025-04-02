package com.example.qrcodescanner

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("name") val nameSurname: String,
    @SerializedName("phone") val phoneNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("validation_date") val validation_date: String?
)