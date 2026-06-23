package com.example.data.repository

import com.example.data.dao.CivicDao
import com.example.data.model.CommunityIssue
import com.example.data.model.IssueComment
import com.example.data.model.CitizenImpact
import kotlinx.coroutines.flow.Flow

class CivicRepository(private val civicDao: CivicDao) {

    val allIssues: Flow<List<CommunityIssue>> = civicDao.getAllIssues()
    
    val leaderboard: Flow<List<CitizenImpact>> = civicDao.getLeaderboard()

    fun getIssueById(id: Int): Flow<CommunityIssue?> = civicDao.getIssueById(id)

    suspend fun insertIssue(issue: CommunityIssue): Long {
        val id = civicDao.insertIssue(issue)
        // Award points to the reporter
        addPoints(issue.reporterName, 50, "report")
        return id
    }

    suspend fun updateIssue(issue: CommunityIssue) {
        civicDao.updateIssue(issue)
    }

    fun getCommentsForIssue(issueId: Int): Flow<List<IssueComment>> = 
        civicDao.getCommentsForIssue(issueId)

    suspend fun insertComment(comment: IssueComment) {
        civicDao.insertComment(comment)
        // Award 5 points for commenting
        addPoints(comment.authorName, 5, "comment")
    }

    suspend fun validateIssue(issueId: Int, validatorName: String) {
        val issue = civicDao.getIssueByIdSync(issueId) ?: return
        // Increment upvote count
        val updated = issue.copy(
            upvoteCount = issue.upvoteCount + 1,
            status = if (issue.status == "REPORTED") "VALIDATED" else issue.status
        )
        civicDao.updateIssue(updated)
        // Award points to validator
        addPoints(validatorName, 10, "validate")
    }

    suspend fun resolveIssue(issueId: Int, resolverName: String, updateText: String) {
        val issue = civicDao.getIssueByIdSync(issueId) ?: return
        val updated = issue.copy(
            status = "RESOLVED",
            resolutionUpdate = updateText,
            resolvedTimestamp = System.currentTimeMillis()
        )
        civicDao.updateIssue(updated)
        // Award points to the resolver and reporter
        addPoints(resolverName, 100, "resolve")
        if (issue.reporterName != resolverName) {
            addPoints(issue.reporterName, 25, "reported_resolved") // Reporter gets bonus points for successful resolution
        }
    }

    suspend fun addPoints(citizenName: String, pointsToAdd: Int, actionType: String) {
        if (citizenName.isBlank()) return
        val trimmedName = citizenName.trim()
        val current = civicDao.getCitizenImpactByName(trimmedName) ?: CitizenImpact(citizenName = trimmedName)
        
        val updated = when (actionType) {
            "report" -> current.copy(
                points = current.points + pointsToAdd,
                issuesReported = current.issuesReported + 1
            )
            "validate" -> current.copy(
                points = current.points + pointsToAdd,
                validationsCount = current.validationsCount + 1
            )
            "resolve" -> current.copy(
                points = current.points + pointsToAdd,
                issuesResolved = current.issuesResolved + 1
            )
            else -> current.copy(
                points = current.points + pointsToAdd
            )
        }
        civicDao.insertCitizenImpact(updated)
    }
}
