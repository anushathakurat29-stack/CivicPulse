package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.CivicDatabase
import com.example.data.model.CommunityIssue
import com.example.data.model.IssueComment
import com.example.data.model.CitizenImpact
import com.example.data.network.GeminiClient
import com.example.data.network.GeminiIssueAnalysis
import com.example.data.repository.CivicRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

sealed class Screen {
    object Feed : Screen()
    object Report : Screen()
    data class Detail(val issueId: Int) : Screen()
    object Leaderboard : Screen()
    object Map : Screen()
    object Impact : Screen()
    data class TownHall(
        val petitionId: String,
        val category: String,
        val location: String,
        val representativeName: String
    ) : Screen()
}

data class MicroPetition(
    val id: String, // "$category-$location"
    val title: String,
    val category: String,
    val location: String,
    val issues: List<CommunityIssue>,
    val representativeName: String,
    val requiredSignatures: Int = 24,
    val signatureCount: Int,
    val isSignedByMe: Boolean,
    val status: String, // "PROPOSED" or "TOWN_HALL_SCHEDULED"
    val meetingTime: String,
    val meetingLink: String,
    val description: String
)

data class TownHallMessage(
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOfficial: Boolean = false
)

class CivicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CivicDatabase.getDatabase(application)
    private val repository = CivicRepository(database.civicDao())

    // App state: Current active user profile
    private val _currentCitizen = MutableStateFlow("Alex Carter")
    val currentCitizen = _currentCitizen.asStateFlow()

    // Navigation and UX routing state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Feed)
    val currentScreen = _currentScreen.asStateFlow()

    // Query and Filtering state parameters
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _sortByColumn = MutableStateFlow("timestamp") // timestamp or upvotes
    val sortByColumn = _sortByColumn.asStateFlow()

    // Core list with reactive combined flows
    val issues: StateFlow<List<CommunityIssue>> = repository.allIssues
        .combine(_selectedCategoryFilter) { list, category ->
            if (category == null) list else list.filter { it.category == category }
        }
        .combine(_sortByColumn) { list, sort ->
            when (sort) {
                "upvotes" -> list.sortedWith(
                    compareByDescending<CommunityIssue> { it.isImmediateSafetyHazard }
                        .thenByDescending { it.upvoteCount }
                )
                else -> list.sortedWith(
                    compareByDescending<CommunityIssue> { it.isImmediateSafetyHazard }
                        .thenByDescending { it.timestamp }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<CitizenImpact>> = repository.leaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Micro-Petitions & Town Halls State ---
    private val _signedPetitions = MutableStateFlow<Set<String>>(emptySet())
    val signedPetitions = _signedPetitions.asStateFlow()

    private val _townHallChats = MutableStateFlow<Map<String, List<TownHallMessage>>>(emptyMap())
    val townHallChats = _townHallChats.asStateFlow()

    val microPetitions: StateFlow<List<MicroPetition>> = combine(
        repository.allIssues,
        _signedPetitions
    ) { allIssuesList, signedSet ->
        allIssuesList
            .groupBy { it.category to it.location }
            .filter { (_, issuesInGroup) -> issuesInGroup.size >= 2 }
            .map { (key, issuesInGroup) ->
                val (category, location) = key
                val petitionId = "$category-$location"
                val representative = when (location) {
                    "Downtown" -> "Representative Clara Vance"
                    "Oakridge District" -> "Councilmember Liam Chen"
                    "Plaza Sector" -> "Representative Chloe Sterling"
                    "East Hill Circle" -> "Supervisor Diana Prince"
                    else -> "Municipal Representative Sarah Jenkins"
                }
                
                val isSigned = signedSet.contains(petitionId)
                val baseVotes = issuesInGroup.sumOf { it.upvoteCount }
                val signatureCount = baseVotes + (if (isSigned) 1 else 0)
                
                // For Sanitation Downtown: 12 + 11 = 23 votes. User sign -> 24 votes -> TOWN_HALL_SCHEDULED!
                val requiredSignatures = if (category == "Sanitation" && location == "Downtown") 24 else 25
                
                val status = if (signatureCount >= requiredSignatures) "TOWN_HALL_SCHEDULED" else "PROPOSED"
                val meetingTime = "Friday at 6:30 PM (Local Time)"
                val meetingLink = "https://townhall.civicresolve.org/meet/$category-$location"
                
                val description = "Our community has logged ${issuesInGroup.size} active $category issues in the $location area (affecting multiple residents). We request the municipality coordinate with $representative to resolve these systemic issues immediately."
                
                MicroPetition(
                    id = petitionId,
                    title = "Systemic $category Resolution in $location",
                    category = category,
                    location = location,
                    issues = issuesInGroup,
                    representativeName = representative,
                    requiredSignatures = requiredSignatures,
                    signatureCount = signatureCount,
                    isSignedByMe = isSigned,
                    status = status,
                    meetingTime = meetingTime,
                    meetingLink = meetingLink,
                    description = description
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun signPetition(petitionId: String) {
        _signedPetitions.value = _signedPetitions.value + petitionId
    }

    fun sendTownHallMessage(petitionId: String, text: String, repName: String, category: String, location: String) {
        if (text.isBlank()) return
        val currentMessages = _townHallChats.value[petitionId] ?: listOf(
            TownHallMessage(
                senderName = repName,
                text = "Welcome, residents of $location. I am $repName, your district representative. Thank you for signing this micro-petition regarding our local $category issues. I am here to listen and discuss. What questions or concerns do you have?",
                isOfficial = true
            )
        )
        
        val userMsg = TownHallMessage(senderName = _currentCitizen.value, text = text, isOfficial = false)
        val updated = currentMessages + userMsg
        _townHallChats.value = _townHallChats.value + (petitionId to updated)
        
        viewModelScope.launch {
            delay(1200)
            val repResponseText = GeminiClient.generateRepresentativeResponse(
                repName = repName,
                category = category,
                location = location,
                userQuestion = text,
                recentHistory = updated.takeLast(4).map { "${it.senderName}: ${it.text}" }
            )
            val repMsg = TownHallMessage(senderName = repName, text = repResponseText, isOfficial = true)
            _townHallChats.value = _townHallChats.value + (petitionId to (updated + repMsg))
        }
    }

    // Selected detail states
    private val _selectedIssueId = MutableStateFlow<Int?>(null)
    val selectedIssue: StateFlow<CommunityIssue?> = _selectedIssueId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getIssueById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentComments: StateFlow<List<IssueComment>> = _selectedIssueId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getCommentsForIssue(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form states
    val reportTitle = MutableStateFlow("")
    val reportDesc = MutableStateFlow("")
    val reportLocation = MutableStateFlow("Downtown")
    val reportMediaType = MutableStateFlow<String?>(null)
    val reportMediaUrl = MutableStateFlow<String?>(null)
    val reportLatitude = MutableStateFlow(37.7749)
    val reportLongitude = MutableStateFlow(-122.4194)

    private val _aiDraftLoading = MutableStateFlow(false)
    val aiDraftLoading = _aiDraftLoading.asStateFlow()

    private val _aiAnalysisResult = MutableStateFlow<GeminiIssueAnalysis?>(null)
    val aiAnalysisResult = _aiAnalysisResult.asStateFlow()

    init {
        seedDataIfEmpty()
    }

    fun updateCitizenName(name: String) {
        if (name.isNotBlank()) {
            val oldName = _currentCitizen.value.trim()
            val newName = name.trim()
            if (oldName != newName) {
                _currentCitizen.value = newName
                // Refresh his profile points status or transfer them
                viewModelScope.launch {
                    val existing = database.civicDao().getCitizenImpactByName(oldName)
                    if (existing != null) {
                        repository.addPoints(newName, existing.points, "manual")
                    } else {
                        repository.addPoints(newName, 10, "manual") // welcome bonus
                    }
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.Detail) {
            _selectedIssueId.value = screen.issueId
        } else if (screen is Screen.Report) {
            reportTitle.value = ""
            reportDesc.value = ""
            reportLocation.value = "Downtown"
            reportMediaType.value = null
            reportMediaUrl.value = null
            reportLatitude.value = 37.7749 + (Math.random() - 0.5) * 0.04
            reportLongitude.value = -122.4194 + (Math.random() - 0.5) * 0.04
            _aiAnalysisResult.value = null
        }
    }

    fun filterByCategory(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun updateSort(sortBy: String) {
        _sortByColumn.value = sortBy
    }

    // --- Actions ---

    fun generateAIDraft() {
        val title = reportTitle.value
        val desc = reportDesc.value
        val loc = reportLocation.value
        if (title.isBlank() || desc.isBlank()) return

        viewModelScope.launch {
            _aiDraftLoading.value = true
            val analysis = GeminiClient.getIssueIntelligentAnalysis(title, desc, loc)
            _aiAnalysisResult.value = analysis
            _aiDraftLoading.value = false
        }
    }

    fun submitReport() {
        val title = reportTitle.value.trim()
        val desc = reportDesc.value.trim()
        val loc = reportLocation.value.trim()
        val analysis = _aiAnalysisResult.value

        if (title.isBlank() || desc.isBlank()) return

        viewModelScope.launch {
            val finalAnalysis = analysis ?: GeminiClient.getIssueIntelligentAnalysis(title, desc, loc)
            val finalCategory = finalAnalysis.category
            val finalPriority = finalAnalysis.priority
            val finalPlan = finalAnalysis.actionPlan
            val finalTone = finalAnalysis.citizenTone
            val finalIsHazard = finalAnalysis.isImmediateSafetyHazard
            val finalCleanedDesc = finalAnalysis.cleanedDescription
            val finalJustification = finalAnalysis.hazardJustification

            val image = getCategoryPlaceholderImage(finalCategory)

            val issue = CommunityIssue(
                title = title,
                description = desc,
                category = finalCategory,
                location = loc,
                reporterName = _currentCitizen.value,
                priority = finalPriority,
                actionPlan = finalPlan,
                imageUrl = image,
                latitude = reportLatitude.value,
                longitude = reportLongitude.value,
                mediaType = reportMediaType.value,
                mediaUrl = reportMediaUrl.value,
                citizenTone = finalTone,
                isImmediateSafetyHazard = finalIsHazard,
                cleanedDescription = finalCleanedDesc,
                hazardJustification = finalJustification
            )
            repository.insertIssue(issue)
            navigateTo(Screen.Feed)
        }
    }

    fun voteOrValidate(issueId: Int) {
        viewModelScope.launch {
            repository.validateIssue(issueId, _currentCitizen.value)
        }
    }

    fun addComment(issueId: Int, commentText: String) {
        if (commentText.isBlank()) return
        viewModelScope.launch {
            val comment = IssueComment(
                issueId = issueId,
                authorName = _currentCitizen.value,
                text = commentText.trim()
            )
            repository.insertComment(comment)
        }
    }

    fun submitResolution(issueId: Int, resolutionDetails: String) {
        if (resolutionDetails.isBlank()) return
        viewModelScope.launch {
            repository.resolveIssue(issueId, _currentCitizen.value, resolutionDetails)
        }
    }

    private fun seedDataIfEmpty() {
        viewModelScope.launch {
            val existing = repository.allIssues.first()
            if (existing.isEmpty()) {
                val issuesToSeed = listOf(
                    CommunityIssue(
                        id = 1,
                        title = "Cracked Water Feed Main Flood",
                        description = "A massive crack in the main pipe under the sidewalk is spewing water 15 feet high, flooding our local bakery and dry cleaners. Extremely urgent!",
                        category = "Utilities",
                        location = "East Hill Circle",
                        reporterName = "Marcus Vance",
                        priority = "HIGH",
                        timestamp = System.currentTimeMillis() - 720000000,
                        upvoteCount = 38,
                        status = "IN_PROGRESS",
                        actionPlan = "1. Isolate the neighborhood main line bypass valve.\n2. Dispatch trench excavation contractor to access damaged line segments.\n3. Replace 10-foot stretch of high-pressure copper piping.\n4. Refill and re-concrete the walkway.\n5. Re-pressurize local civic distribution system.",
                        imageUrl = "photo_utilities",
                        latitude = 37.7858,
                        longitude = -122.4064,
                        mediaType = "VIDEO",
                        mediaUrl = "video_water_leak",
                        citizenTone = "URGENT",
                        isImmediateSafetyHazard = true,
                        cleanedDescription = "Severe main water pipe fracture beneath public sidewalk resulting in high-pressure surface flooding. Commercial properties impacted.",
                        hazardJustification = "Active high-volume water flooding adjacent to commercial properties, undermining sidewalk integrity and creating structural flooding risks."
                    ),
                    CommunityIssue(
                        id = 2,
                        title = "Streetlamps Broken Over Four Blocks",
                        description = "Street lights are completely black. This creates severe safety issues for school children and night joggers.",
                        category = "Lighting",
                        location = "Oakridge District",
                        reporterName = "Emily Rogers",
                        priority = "HIGH",
                        timestamp = System.currentTimeMillis() - 510000000,
                        upvoteCount = 18,
                        status = "VALIDATED",
                        actionPlan = "1. Run diagnostic sweep on sub-station feeder loops.\n2. Deploy hydraulic bucket vehicle for direct overhead lamp servicing.\n3. Fit solid-state modern photocell sensors and low-draw high-lumen bulbs.\n4. Check and lock high-voltage metal access junction blocks.",
                        imageUrl = "photo_lighting",
                        latitude = 37.7735,
                        longitude = -122.4312,
                        mediaType = "IMAGE",
                        mediaUrl = "image_dark_streetlamps",
                        citizenTone = "ANXIOUS",
                        isImmediateSafetyHazard = false,
                        cleanedDescription = "Systemic lighting outage spanning a contiguous four-block residential zone, reducing visibility to zero.",
                        hazardJustification = "Diminished night visibility increases risk of pedestrian accidents and opportunistic local crimes."
                    ),
                    CommunityIssue(
                        id = 3,
                        title = "Hazardous Chemical Debris Dumped at Riverbank",
                        description = "Large construction barrels marked with warning decals have been abandoned at the riverside. Some thick dark liquid is leaking into the water. Immediate intervention is required.",
                        category = "Environment",
                        location = "Plaza Sector",
                        reporterName = "Sarah Chen",
                        priority = "HIGH",
                        timestamp = System.currentTimeMillis() - 170000000,
                        upvoteCount = 29,
                        status = "RESOLVED",
                        actionPlan = "1. Cordon off the hazardous river segment with protective containment barriers.\n2. Deploy HAZMAT specialist team with safe heavy-lifting apparatus.\n3. Retrieve, catalog, and secure leaked containers for treatment.\n4. Collect soil and water samples to measure contaminant absorption levels.\n5. Log vehicle plates via nearby traffic cams to trigger enforcement.",
                        resolutionUpdate = "The local environmental protection squad responded within 6 hours. Leaking barrels were entirely cleared in secure hazmat drums. Water quality levels have been certified back to safety standards.",
                        resolvedTimestamp = System.currentTimeMillis() - 50000000,
                        imageUrl = "photo_environment",
                        latitude = 37.7612,
                        longitude = -122.3925,
                        mediaType = "IMAGE",
                        mediaUrl = "image_toxic_drums",
                        citizenTone = "URGENT",
                        isImmediateSafetyHazard = true,
                        cleanedDescription = "Unlawful dumping of construction canisters marked with hazardous chemical decals on public riverfront. Liquid run-off is actively entering the local waterway.",
                        hazardJustification = "Potential contamination of local water supplies and immediate danger to riverbank wildlife."
                    ),
                    CommunityIssue(
                        id = 4,
                        title = "Flickering Security Lamps behind Shopping Strip",
                        description = "The security lights behind the shopping alley flicker constantly, making the loading area very dark and dangerous at night! Someone needs to look at this.",
                        category = "Lighting",
                        location = "Oakridge District",
                        reporterName = "Alex Carter",
                        priority = "MEDIUM",
                        timestamp = System.currentTimeMillis() - 400000000,
                        upvoteCount = 9,
                        status = "VALIDATED",
                        actionPlan = "1. Survey rear alley illumination levels.\n2. Repair contact wiring on circuit poles.\n3. Fit modern high-pressure sodium bulbs.",
                        imageUrl = "photo_lighting",
                        latitude = 37.7715,
                        longitude = -122.4350,
                        citizenTone = "ANXIOUS",
                        isImmediateSafetyHazard = false,
                        cleanedDescription = "Unstable illumination on commercial back-lot security circuits, creating local shadows.",
                        hazardJustification = "Increases blind spots for loading crews and business workers at night."
                    ),
                    CommunityIssue(
                        id = 5,
                        title = "Low Sewer Pressure and Gutter Backflow",
                        description = "Water is bubbling up through the storm gutters on East Hill Circle, smelling terrible. It overflows every time we get even a tiny bit of rain.",
                        category = "Utilities",
                        location = "East Hill Circle",
                        reporterName = "Marcus Vance",
                        priority = "MEDIUM",
                        timestamp = System.currentTimeMillis() - 300000000,
                        upvoteCount = 14,
                        status = "VALIDATED",
                        actionPlan = "1. Clear drainage inlets using pressurized sewer sweep.\n2. Check for root intrusion in municipal line.\n3. Upgrade neighborhood run-off manifold capacity.",
                        imageUrl = "photo_utilities",
                        latitude = 37.7880,
                        longitude = -122.4090,
                        citizenTone = "FRUSTRATED",
                        isImmediateSafetyHazard = false,
                        cleanedDescription = "Substandard drainage flow and storm-drain backpressure in residential zone.",
                        hazardJustification = "Stagnant grey-water pooling represents a municipal sanitation vector and localized flooding risk."
                    ),
                    CommunityIssue(
                        id = 6,
                        title = "Overflowing Trash Bins in Downtown School Plaza",
                        description = "The trash cans here are completely full, and birds are scattering garbage everywhere. It is a disgusting mess right next to where kids eat lunch!",
                        category = "Sanitation",
                        location = "Downtown",
                        reporterName = "Sarah Chen",
                        priority = "MEDIUM",
                        timestamp = System.currentTimeMillis() - 200000000,
                        upvoteCount = 12,
                        status = "REPORTED",
                        actionPlan = "1. Schedule high-frequency trash bin collections.\n2. Disinfect the surrounding plaza area.\n3. Install larger secure containment bins.",
                        imageUrl = "photo_sanitation",
                        latitude = 37.7740,
                        longitude = -122.4180,
                        citizenTone = "ANGRY",
                        isImmediateSafetyHazard = false,
                        cleanedDescription = "Excess public refuse accumulation at high-traffic educational plaza.",
                        hazardJustification = "Public hygiene threat and pest attraction in high-density pedestrian zone."
                    ),
                    CommunityIssue(
                        id = 7,
                        title = "Bulk Mattress & Furniture Dumped in Alleyway",
                        description = "Someone dumped three massive mattresses and a broken sofa in the Downtown alley. It completely blocks the fire exit of our building!",
                        category = "Sanitation",
                        location = "Downtown",
                        reporterName = "Emily Rogers",
                        priority = "HIGH",
                        timestamp = System.currentTimeMillis() - 100000000,
                        upvoteCount = 11,
                        status = "REPORTED",
                        actionPlan = "1. Dispatch bulk trash flatbed vehicle.\n2. Clear blocked emergency egress alley.\n3. Audit area for illegal dumping signage.",
                        imageUrl = "photo_sanitation",
                        latitude = 37.7755,
                        longitude = -122.4210,
                        citizenTone = "ANGRY",
                        isImmediateSafetyHazard = true,
                        cleanedDescription = "Unlawful bulky furniture disposal in commercial easement, obstructing fire exit.",
                        hazardJustification = "Creates a dangerous obstruction of a certified emergency egress path."
                    )
                )

                for (iss in issuesToSeed) {
                    repository.updateIssue(iss)
                    database.civicDao().insertIssue(iss)
                }

                repository.insertComment(IssueComment(issueId = 1, authorName = "Alex Carter", text = "This is literally a waterfall in the middle of our sidewalk. Please fix ASAP!"))
                repository.insertComment(IssueComment(issueId = 1, authorName = "Mayor Adams", text = "Repair trucks are already on scene. Water authority crew has shut off the feeder main."))

                repository.insertComment(IssueComment(issueId = 2, authorName = "Brandon Lee", text = "Walking home from the train is genuinely scary now. Thank you for reporting this Emily!"))

                repository.addPoints("Marcus Vance", 120, "manual")
                repository.addPoints("Emily Rogers", 90, "manual")
                repository.addPoints("Sarah Chen", 175, "manual")
                repository.addPoints("Alex Carter", 45, "manual")
                repository.addPoints("City_Cleaner_Eco", 320, "manual")
            }
        }
    }

    private fun getCategoryPlaceholderImage(category: String): String {
        return when (category) {
            "Road" -> "photo_road"
            "Lighting" -> "photo_lighting"
            "Sanitation" -> "photo_sanitation"
            "Safety" -> "photo_safety"
            "Utilities" -> "photo_utilities"
            "Environment" -> "photo_environment"
            else -> "photo_other"
        }
    }
}
