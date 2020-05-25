package com.example.criminalintent.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity
data class Crime(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isSolved: Boolean = false,
    var suspect: String = "",
    @ColumnInfo(name = "suspect_phone_number")
    var suspectPhoneNumber: String = ""
) {
    val photoFileName
        get() = "IMG_$id.jpg"
}