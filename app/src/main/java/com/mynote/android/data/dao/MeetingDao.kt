package com.mynote.android.data.dao

import androidx.room.*
import com.mynote.android.data.entity.Meeting
import com.mynote.android.data.entity.MeetingEntry

@Dao
interface MeetingDao {

    @Insert
    suspend fun insertMeeting(meeting: Meeting): Long

    @Update
    suspend fun updateMeeting(meeting: Meeting)

    @Delete
    suspend fun deleteMeeting(meeting: Meeting)

    @Query("SELECT * FROM meetings ORDER BY startedAt DESC")
    suspend fun getAllMeetings(): List<Meeting>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingById(id: Long): Meeting?

    // MeetingEntry

    @Insert
    suspend fun insertEntry(entry: MeetingEntry): Long

    @Update
    suspend fun updateEntry(entry: MeetingEntry)

    @Query("SELECT * FROM meeting_entries WHERE meetingId = :meetingId ORDER BY timestamp ASC")
    suspend fun getEntries(meetingId: Long): List<MeetingEntry>

    @Query("DELETE FROM meeting_entries WHERE meetingId = :meetingId")
    suspend fun deleteEntries(meetingId: Long)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteMeetingById(id: Long)
}
