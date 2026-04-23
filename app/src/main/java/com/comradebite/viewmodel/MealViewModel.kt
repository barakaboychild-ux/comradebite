package com.comradebite.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comradebite.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

enum class SyncStatus {
    IDLE, SYNCING, SYNCED, ERROR
}

data class GroupInfo(val code: String, val name: String)

class MealViewModel(
    private val repository: MealRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val databaseUrl = "https://comradebite-c0265c98-default-rtdb.firebaseio.com"
    private val firebase = FirebaseDatabase.getInstance(databaseUrl)
    private val auth = FirebaseAuth.getInstance()
    
    private var mealsListener: ValueEventListener? = null
    private var inventoryListener: ValueEventListener? = null
    private var userGroupsListener: ValueEventListener? = null
    private var currentGroupNode: String? = null

    companion object {
        private const val TAG = "MealViewModel"
        private const val TWO_WEEKS_DAYS = 14L
        private const val RESET_DELAY_MS = 60000L
        private const val CHECK_AUTO_EAT_DELAY_MS = 30000L
        private const val MAX_GROUP_SIZE = 10
        private const val SCORE_AFFORDABILITY_WEIGHT = 0.7
        private const val SCORE_VARIETY_WEIGHT = 0.3
        
        private const val PENALTY_UGALI_LUNCH = 3000.0
        private const val BONUS_SPECIAL_DINNER = 1000.0
        private const val PENALTY_NOT_SPECIAL_DINNER = 2000.0
        private const val PENALTY_RARE_RECENT = 10000.0
        private const val PENALTY_RARE_BASE = 1500.0
        private const val PENALTY_VARIETY_STEP = 500.0
        private const val RANDOM_OFFSET_MAX = 50.0

        val BREAKFAST_TIME = LocalTime.of(9, 0)
        val LUNCH_TIME = LocalTime.of(13, 0)
        val DINNER_TIME = LocalTime.of(20, 0)
    }

    private val _groupCode = MutableStateFlow(prefs.getString("group_code", "") ?: "")
    val groupCode: StateFlow<String> = _groupCode

    private val _groupName = MutableStateFlow(prefs.getString("group_name", "") ?: "")
    val groupName: StateFlow<String> = _groupName

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _userGroups = MutableStateFlow<List<GroupInfo>>(emptyList())
    val userGroups: StateFlow<List<GroupInfo>> = _userGroups

    private val _groupSize = MutableStateFlow(1)
    val groupSize: StateFlow<Int> = _groupSize

    private val _budgetPerPerson = MutableStateFlow<Double?>(null)
    val budgetPerPerson: StateFlow<Double?> = _budgetPerPerson

    private val _eatenTodayIngredients = MutableStateFlow<Set<String>>(emptySet())
    val eatenTodayIngredients: StateFlow<Set<String>> = _eatenTodayIngredients

    private val _eatenTodayComboIds = MutableStateFlow<Set<Int>>(emptySet())
    val eatenTodayComboIds: StateFlow<Set<Int>> = _eatenTodayComboIds

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    private val dateSeparator = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val _currentDay = MutableStateFlow(LocalDate.now().format(dateSeparator))
    val currentDay: StateFlow<String> = _currentDay

    val currentBackground: Flow<Int> = flow {
        while (true) {
            val hour = LocalDateTime.now().hour
            emit((hour / 2) % 3)
            delay(RESET_DELAY_MS)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allBaseMeals = repository.allBaseMeals
    val allCombinations = repository.allCombinations

    val dailyPlan: StateFlow<Map<String, MealCombination?>> = combine(
        allCombinations,
        allBaseMeals,
        _eatenTodayIngredients,
        _eatenTodayComboIds,
        _currentDay
    ) { combos, baseMeals, eaten, eatenIds, _ ->
        calculatePlanForDay(combos, baseMeals, _groupSize.value, eaten, eatenIds, false, emptyMap(), false)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        viewModelScope.launch {
            while (true) {
                val today = LocalDate.now().format(dateSeparator)
                val lastReset = prefs.getString("last_reset_day", "")
                if (today != lastReset) {
                    resetDay()
                    _currentDay.value = today
                    prefs.edit().putString("last_reset_day", today).apply()
                }
                delay(RESET_DELAY_MS)
            }
        }

        viewModelScope.launch {
            while (true) {
                val now = LocalTime.now()
                val currentPlan = dailyPlan.value
                if (now.isAfter(BREAKFAST_TIME) && now.isBefore(LUNCH_TIME)) {
                    if (!_eatenTodayComboIds.value.any { id -> currentPlan["Breakfast"]?.id == id }) {
                        currentPlan["Breakfast"]?.let { markAsEaten(it) }
                    }
                }
                if (now.isAfter(LUNCH_TIME) && now.isBefore(DINNER_TIME)) {
                    if (!_eatenTodayComboIds.value.any { id -> currentPlan["Lunch"]?.id == id }) {
                        currentPlan["Lunch"]?.let { markAsEaten(it) }
                    }
                }
                if (now.isAfter(DINNER_TIME)) {
                    if (!_eatenTodayComboIds.value.any { id -> currentPlan["Dinner"]?.id == id }) {
                        currentPlan["Dinner"]?.let { markAsEaten(it) }
                    }
                }
                delay(CHECK_AUTO_EAT_DELAY_MS)
            }
        }

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) observeUserGroups(user.uid)
            else _userGroups.value = emptyList()
        }

        if (_groupCode.value.isNotBlank()) setupFirebaseSync(_groupCode.value)
    }

    private fun observeUserGroups(uid: String) {
        userGroupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = mutableListOf<GroupInfo>()
                snapshot.children.forEach { child ->
                    val code = child.key ?: return@forEach
                    val name = child.child("name").getValue(String::class.java) ?: "Unknown"
                    groups.add(GroupInfo(code, name))
                }
                _userGroups.value = groups
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        firebase.getReference("users").child(uid).child("groups").addValueEventListener(userGroupsListener!!)
    }

    fun manualSync() {
        if (_groupCode.value.isNotBlank()) setupFirebaseSync(_groupCode.value)
    }

    fun updateGroupCode(code: String) {
        val cleanCode = code.trim().uppercase()
        currentGroupNode?.let { old ->
            mealsListener?.let { firebase.getReference("groups").child(old).child("meals").removeEventListener(it) }
            inventoryListener?.let { firebase.getReference("groups").child(old).child("inventory").removeEventListener(it) }
        }
        _groupCode.value = cleanCode
        prefs.edit().putString("group_code", cleanCode).apply()
        if (cleanCode.isNotBlank()) setupFirebaseSync(cleanCode)
        else {
            _syncStatus.value = SyncStatus.IDLE
            _groupName.value = ""
            currentGroupNode = null
        }
    }

    fun joinGroup(code: String, onResult: (Boolean, String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        val user = auth.currentUser ?: return onResult(false, "Please login first")
        _syncStatus.value = SyncStatus.SYNCING
        firebase.getReference("groups").child(cleanCode).child("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.getValue(String::class.java)
                if (name != null) {
                    val updates = mapOf(
                        "users/${user.uid}/groups/$cleanCode/name" to name,
                        "groups/$cleanCode/members/${user.uid}" to true
                    )
                    firebase.reference.updateChildren(updates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            updateGroupCode(cleanCode)
                            onResult(true, "Joined $name!")
                        } else onResult(false, "Join failed")
                    }
                } else onResult(false, "Invalid Code")
            }
            override fun onCancelled(error: DatabaseError) = onResult(false, error.message)
        })
    }

    fun createGroup(name: String) {
        val user = auth.currentUser ?: return
        val code = generateRandomCode(6)
        val updates = mutableMapOf<String, Any>()
        updates["groups/$code/name"] = name
        updates["groups/$code/owner"] = user.uid
        updates["groups/$code/members/${user.uid}"] = true
        updates["users/${user.uid}/groups/$code/name"] = name
        firebase.reference.updateChildren(updates).addOnCompleteListener { if (it.isSuccessful) updateGroupCode(code) }
    }

    private fun generateRandomCode(length: Int) = (1..length).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")

    private fun setupFirebaseSync(code: String) {
        _syncStatus.value = SyncStatus.SYNCING
        currentGroupNode = code
        firebase.getReference("groups").child(code).child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val n = s.getValue(String::class.java) ?: "Group"
                _groupName.value = n
                prefs.edit().putString("group_name", n).apply()
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        var mLoaded = false
        var iLoaded = false
        mealsListener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                mLoaded = true
                if (iLoaded) _syncStatus.value = SyncStatus.SYNCED
                viewModelScope.launch {
                    val local = allCombinations.first()
                    val toIns = mutableListOf<MealCombination>()
                    s.children.forEach { c ->
                        val map = c.value as? Map<*, *> ?: return@forEach
                        val name = map["name"] as? String ?: ""
                        val ings = map["baseNames"] as? List<String> ?: emptyList()
                        if (local.none { it.name == name && it.baseNames == ings }) {
                            toIns.add(MealCombination(name = name, baseNames = ings, targetTime = map["targetTime"] as? String ?: "Breakfast"))
                        }
                    }
                    if (toIns.isNotEmpty()) repository.insertCombinations(toIns)
                }
            }
            override fun onCancelled(e: DatabaseError) { _syncStatus.value = SyncStatus.ERROR }
        }
        firebase.getReference("groups").child(code).child("meals").addValueEventListener(mealsListener!!)

        inventoryListener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                iLoaded = true
                if (mLoaded) _syncStatus.value = SyncStatus.SYNCED
                viewModelScope.launch {
                    val local = allBaseMeals.first()
                    val toIns = mutableListOf<BaseMeal>()
                    s.children.forEach { c ->
                        val map = c.value as? Map<*, *> ?: return@forEach
                        val name = map["name"] as? String ?: ""
                        val price = (map["totalPrice"] as? Number)?.toDouble() ?: 0.0
                        val ppl = (map["numPeople"] as? Number)?.toInt() ?: 1
                        if (local.none { it.name == name && it.numPeople == ppl }) {
                            toIns.add(BaseMeal(name = name, totalPrice = price, numPeople = ppl))
                        }
                    }
                    if (toIns.isNotEmpty()) repository.insertBaseMeals(toIns)
                }
            }
            override fun onCancelled(e: DatabaseError) { _syncStatus.value = SyncStatus.ERROR }
        }
        firebase.getReference("groups").child(code).child("inventory").addValueEventListener(inventoryListener!!)
    }

    fun uploadComboToFirebase(combo: MealCombination) {
        val code = _groupCode.value
        if (code.isBlank()) return
        val ref = firebase.getReference("groups").child(code).child("meals")
        ref.child(ref.push().key ?: return).setValue(combo)
    }

    fun uploadInventoryListToFirebase(meals: List<BaseMeal>) {
        val code = _groupCode.value
        if (code.isBlank() || meals.isEmpty()) return
        val ref = firebase.getReference("groups").child(code).child("inventory")
        val updates = mutableMapOf<String, Any>()
        meals.forEach { updates[ref.push().key ?: return@forEach] = it }
        ref.updateChildren(updates).addOnFailureListener { _syncStatus.value = SyncStatus.ERROR }
    }

    fun saveCombination(name: String, ingredients: List<String>, time: String, isSpecial: Boolean, isRare: Boolean) {
        viewModelScope.launch {
            repository.insertCombination(
                MealCombination(
                    name = name,
                    baseNames = ingredients,
                    targetTime = time,
                    isSpecial = isSpecial,
                    isRare = isRare
                )
            )
        }
    }

    fun deleteCombination(combo: MealCombination) {
        viewModelScope.launch {
            repository.deleteCombination(combo)
        }
    }

    fun setGroupSize(s: Int) { _groupSize.value = s.coerceIn(1, MAX_GROUP_SIZE) }
    fun toggleTheme() {
        val n = !_isDarkTheme.value
        _isDarkTheme.value = n
        prefs.edit().putBoolean("is_dark_theme", n).apply()
    }

    private fun calculatePlanForDay(
        combos: List<MealCombination>,
        baseMeals: List<BaseMeal>,
        size: Int,
        eaten: Set<String>,
        eatenIds: Set<Int>,
        forceSpecial: Boolean,
        planned: Map<Int, Int>,
        random: Boolean
    ): Map<String, MealCombination?> {
        val plan = mutableMapOf<String, MealCombination?>()
        val ingsUsed = eaten.map { it.trim().lowercase() }.toMutableSet()
        val idsUsed = eatenIds.toMutableSet()
        val nowMs = System.currentTimeMillis()

        listOf("Breakfast", "Lunch", "Dinner").forEach { time ->
            // FIX: Keep eaten meal visible in its slot
            val eatenInSlot = combos.find { it.id in eatenIds && it.targetTime == time }
            if (eatenInSlot != null) {
                plan[time] = eatenInSlot
                eatenInSlot.baseNames.forEach { ingsUsed.add(it.trim().lowercase()) }
                return@forEach
            }

            val cand = combos.filter { it.targetTime == time && it.id !in idsUsed && !isAnyIngredientUsed(it, ingsUsed) }
                .ifEmpty { combos.filter { it.targetTime == time && it.id !in idsUsed } }

            val pick = cand.maxByOrNull { calculateScore(it, baseMeals, size) + if (random) Random.nextDouble(0.0, 50.0) else 0.0 }
            plan[time] = pick
            pick?.let {
                idsUsed.add(it.id)
                it.baseNames.forEach { n -> ingsUsed.add(n.trim().lowercase()) }
            }
        }
        return plan
    }

    private fun isAnyIngredientUsed(c: MealCombination, used: Set<String>): Boolean {
        return c.baseNames.any { it.trim().lowercase() in used }
    }

    private fun calculateScore(c: MealCombination, bm: List<BaseMeal>, s: Int): Double {
        val share = getIndividualShare(c, bm, s) ?: return -1.0
        return (2000.0 / (share + 1.0)) * 0.7 + (10.0 / (c.frequency + 1.0)) * 0.3
    }

    fun getIndividualShare(c: MealCombination, bm: List<BaseMeal>, s: Int): Double? {
        var total = 0.0
        for (n in c.baseNames) {
            val m = bm.find { it.name.trim().equals(n.trim(), true) && it.numPeople == s } ?: bm.find { it.name.trim().equals(n.trim(), true) } ?: return null
            total += m.pricePerPerson
        }
        return total
    }

    fun markAsEaten(c: MealCombination) = viewModelScope.launch {
        repository.updateCombination(c.copy(frequency = c.frequency + 1, lastEaten = System.currentTimeMillis()))
        _eatenTodayIngredients.value = _eatenTodayIngredients.value + c.baseNames.map { it.trim().lowercase() }
        _eatenTodayComboIds.value = _eatenTodayComboIds.value + c.id
    }

    fun resetDay() {
        _eatenTodayIngredients.value = emptySet()
        _eatenTodayComboIds.value = emptySet()
    }
}
