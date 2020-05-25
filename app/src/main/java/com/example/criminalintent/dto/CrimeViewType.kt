package com.example.criminalintent.dto

import com.example.criminalintent.entity.Crime
import java.util.*

data class CrimeViewType(
    var type: ViewType = ViewType.HEADER,
    var crime: Crime? = null,
    var header: Date? = null
)