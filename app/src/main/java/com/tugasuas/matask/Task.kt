package com.tugasuas.matask

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Task(
    @DocumentId
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String? = null,

    @get:PropertyName("deadline")
    @set:PropertyName("deadline")
    var deadline: Timestamp? = null,

    @get:PropertyName("completed")
    @set:PropertyName("completed")
    var completed: Boolean = false,

    @get:PropertyName("favorite")
    @set:PropertyName("favorite")
    var favorite: Boolean = false,

    @get:PropertyName("categoryId")
    @set:PropertyName("categoryId")
    var categoryId: String = "",

    @get:PropertyName("position")
    @set:PropertyName("position")
    var position: Long = 0L,
    
    @get:PropertyName("favoritedTimestamp")
    @set:PropertyName("favoritedTimestamp")
    var favoritedTimestamp: Timestamp? = null,
    
    @get:PropertyName("completedTimestamp")
    @set:PropertyName("completedTimestamp")
    var completedTimestamp: Timestamp? = null
) : Parcelable
