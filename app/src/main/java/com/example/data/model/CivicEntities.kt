package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_issues")
data class CommunityIssue(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // Road, Lighting, Sanitation, Safety, Utilities, Environment, Other
    val location: String, // Neighborhood / Address
    val reporterName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "REPORTED", // REPORTED, VALIDATED, INVESTIGATING, IN_PROGRESS, RESOLVED
    val upvoteCount: Int = 1,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val actionPlan: String = "", // AI generated step-by-step resolution plan
    val resolutionUpdate: String? = null,
    val resolvedTimestamp: Long? = null,
    val imageUrl: String? = null,
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val mediaType: String? = null, // "IMAGE", "VIDEO"
    val mediaUrl: String? = null,
    
    // Sentient Routing & Tone Analysis Fields
    val citizenTone: String = "NEUTRAL", // Detected citizen emotion/tone (ANGRY, FRUSTRATED, CALM, ANXIOUS, HELPFUL)
    val isImmediateSafetyHazard: Boolean = false, // Priority safety hazard flag
    val cleanedDescription: String = "", // Cleaned professional technical description
    val hazardJustification: String = "" // Justification for priority & hazard flag
)

@Entity(tableName = "issue_comments")
data class IssueComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val issueId: Int,
    val authorName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "citizen_impact")
data class CitizenImpact(
    @PrimaryKey val citizenName: String,
    val points: Int = 0,
    val issuesReported: Int = 0,
    val validationsCount: Int = 0,
    val issuesResolved: Int = 0
)
