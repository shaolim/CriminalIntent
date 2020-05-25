package com.example.criminalintent.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.criminalintent.entity.Crime
import java.util.*

@Dao
interface CrimeDao {
    @Query("Select * FROM crime ORDER BY date ASC")
    fun getCrimes(): LiveData<List<Crime>>

    @Query("Select * from crime where id=(:id)")
    fun getCrime(id: UUID): LiveData<Crime?>

    @Insert
    fun addCrime(crime: Crime)

    @Update
    fun updateCrime(crime: Crime)
}