package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date LIMIT 1")
    suspend fun getAttendanceByDate(date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC")
    fun getAttendanceByMonth(monthPrefix: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: AttendanceEntity): Long

    @Update
    suspend fun update(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteById(id: Int)
}
