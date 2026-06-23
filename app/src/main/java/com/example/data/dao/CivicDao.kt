package com.example.data.dao

import androidx.room.*
import com.example.data.model.CommunityIssue
import com.example.data.model.IssueComment
import com.example.data.model.CitizenImpact
import kotlinx.coroutines.flow.Flow

@Dao
interface CivicDao {
    // --- Issue Operations ---
    @Query("SELECT * FROM community_issues ORDER BY timestamp DESC")
    fun getAllIssues(): Flow<List<CommunityIssue>>

    @Query("SELECT * FROM community_issues WHERE id = :id")
    fun getIssueById(id: Int): Flow<CommunityIssue?>

    @Query("SELECT * FROM community_issues WHERE id = :id")
    suspend fun getIssueByIdSync(id: Int): CommunityIssue?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: CommunityIssue): Long

    @Update
    suspend fun updateIssue(issue: CommunityIssue)

    @Query("DELETE FROM community_issues WHERE id = :id")
    suspend fun deleteIssueById(id: Int)

    // --- Comment Operations ---
    @Query("SELECT * FROM issue_comments WHERE issueId = :issueId ORDER BY timestamp ASC")
    fun getCommentsForIssue(issueId: Int): Flow<List<IssueComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: IssueComment)

    // --- Citizen Impact Operations ---
    @Query("SELECT * FROM citizen_impact ORDER BY points DESC")
    fun getLeaderboard(): Flow<List<CitizenImpact>>

    @Query("SELECT * FROM citizen_impact WHERE citizenName = :name")
    suspend fun getCitizenImpactByName(name: String): CitizenImpact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCitizenImpact(impact: CitizenImpact)
}
