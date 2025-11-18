package com.barghest.bux.data.mapper

import com.barghest.bux.data.dto.LoggedInUser
import com.barghest.bux.domain.model.User

fun LoggedInUser.toDomain(): User {
    return User(
        id = id,
        username = username
    )
}