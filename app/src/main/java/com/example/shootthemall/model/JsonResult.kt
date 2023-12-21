package com.example.shootthemall.model

import kotlinx.serialization.Serializable


@Serializable
data class JsonResult(val username: String, val score: Int)
