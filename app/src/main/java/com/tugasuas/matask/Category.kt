package com.tugasuas.matask

import com.google.firebase.firestore.DocumentId

data class Category(
    @DocumentId
    val id: String = "",
    val name: String = "",
    var position: Long = 0L
)
