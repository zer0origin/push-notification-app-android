package com.willcocks.callum.timetablenotifier.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import java.util.UUID

@InternalSerializationApi
@Serializable
data class RegisterModel(
    val token: String,
    val username: String,

    @Serializable(with = UUIDSerializer::class)
    val userUID: UUID?,
)