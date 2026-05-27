package com.comradebite.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comradebite.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.random.Random

enum class SyncStatus {
    IDLE, SYNCING, SYNCED, ERROR
}

data class GroupInfo(val code: String, val name: String)
data class DailyHealth(val kcal: Int, val groups: List<String>)
data class AiHint(val icon: String, val text: String)
data class ChatMessage(val text: String, val isUser: Boolean)

class MealViewModel(
    private val repository: MealRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val databaseUrl = "https://comradebite-c0265c98-default-rtdb.firebaseio.com"
    private val firebase = FirebaseDatabase.getInstance(databaseUrl)
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()
    
    private var mealsListener: ValueEventListener? = null
    private var inventoryListener: ValueEventListener? = null
    private var userGroupsListener: ValueEventListener? = null
    private var currentGroupNode: String? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) observeUserGroups(user.uid)
        else _userGroups.value = emptyList()
    }

    companion object {
        private const val TAG = "MealViewModel"
        private const val RESET_DELAY_MS = 60000L
        private const val MAX_GROUP_SIZE = 10
        
        // --- SMART CALCULATOR WEIGHTS ---
        private const val WEIGHT_VARIETY = 0.8
        private const val WEIGHT_AFFORDABILITY = 0.2
        
        private const val PENALTY_RECENTLY_EATEN = 50000.0 
        private const val PENALTY_REPEATED_IN_WEEK = 10000.0 
        private const val PENALTY_OVER_BUDGET = 6000.0
        private const val BONUS_SPECIAL = 15000.0
        private const val LUCK_FACTOR_MAX = 500.0 

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

    private val _groupSize = MutableStateFlow(prefs.getInt("group_size", 1))
    val groupSize: StateFlow<Int> = _groupSize

    private val _budgetPerPerson = MutableStateFlow<Double?>(null)
    val budgetPerPerson: StateFlow<Double?> = _budgetPerPerson

    private val _eatenTodayIngredients = MutableStateFlow<Set<String>>(emptySet())
    val eatenTodayIngredients: StateFlow<Set<String>> = _eatenTodayIngredients

    private val _eatenTodayComboIds = MutableStateFlow<Set<Int>>(emptySet())
    val eatenTodayComboIds: StateFlow<Set<Int>> = _eatenTodayComboIds

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    private val _waterToday = MutableStateFlow(prefs.getInt("water_today", 0))
    val waterToday: StateFlow<Int> = _waterToday

    private val _chatMessages = MutableStateFlow(listOf(ChatMessage("👋 Hey Comrade! I'm Chef AI. I've taken over the kitchen to help you eat well on any budget. Ask me anything!", false)))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val dateSeparator = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val _currentDay = MutableStateFlow(LocalDate.now().format(dateSeparator))
    val currentDay: StateFlow<String> = _currentDay

    private val _lockedWeeklyPlanIds = MutableStateFlow<List<Map<String, Int?>>>(loadLockedPlan())

    val allBaseMeals = repository.allBaseMeals
    val allCombinations = repository.allCombinations

    val currentBackground: Flow<Int> = flow {
        while (true) {
            emit((LocalDateTime.now().hour / 2) % 3)
            delay(RESET_DELAY_MS)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dailyPlan: StateFlow<Map<String, MealCombination?>> = combine(
        allCombinations,
        _lockedWeeklyPlanIds,
        _currentDay
    ) { combos, lockedIds, _ ->
        val now = LocalDate.now()
        val dayIndex = now.dayOfWeek.value % 7 // Sunday = 0
        if (lockedIds.size > dayIndex) dayPlanFromIds(lockedIds[dayIndex], combos) else emptyMap()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val weeklyTimetable: StateFlow<List<Map<String, MealCombination?>>> = combine(
        allCombinations,
        _lockedWeeklyPlanIds
    ) { combos, lockedIds ->
        lockedIds.map { dayMap -> dayPlanFromIds(dayMap, combos) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyHealth: StateFlow<DailyHealth> = dailyPlan.map { plan: Map<String, MealCombination?> ->
        var kcal = 0
        val groups = mutableSetOf<String>()
        plan.values.filterNotNull().forEach { combo ->
            combo.baseNames.forEach { name ->
                FoodDatabase.getFoodData(name)?.let {
                    kcal += it.kcal
                    groups.add(it.group)
                }
            }
        }
        DailyHealth(kcal, groups.toList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyHealth(0, emptyList()))

    val aiThoughts: StateFlow<List<String>> = combine(dailyHealth, _waterToday, _budgetPerPerson) { health, water, budget ->
        val groups = health.groups
        val kcal = health.kcal
        
        val thoughts = mutableListOf<String>()
        
        // Humanized Chef AI thoughts - human-like, conversational, student-friendly
        thoughts.add("Welcome to today's meals, eat with a smile!")
        thoughts.add("Good food, good mood, good grades. You've got this!")
        
        if (water < 4) {
            thoughts.add("You're looking a bit dry, Comrade. Grab a glass of water, your brain will thank you!")
        } else if (water >= 8) {
            thoughts.add("Absolute hydration legend! You're basically made of logic and fresh water today! 🎉")
        }

        if (!groups.any { it == "fruit" || it == "vegetable" }) {
            thoughts.add("A fruit today won't cost much, but man, it'll make you feel like a new person!")
        }

        if (kcal > 0 && kcal < 1300) {
            thoughts.add("Don't let your battery hit zero! Your brain needs energy as much as your books.")
        } else if (kcal > 2200) {
            thoughts.add("That's some solid fuel right there! You're definitely ready for whatever the day throws at you.")
        }

        if (budget != null && budget < 120) {
            thoughts.add("Managing that budget like a pro. Saving is an art, and you're the artist.")
        }

        if (groups.size >= 4) {
            thoughts.add("Your plate looks like a rainbow of health! Excellence is definitely a habit for you.")
        }

        thoughts.add("A hungry comrade is a weary comrade. Let's fuel up!")
        thoughts.add("Success starts with a full stomach. Bon appétit!")
        
        thoughts.shuffle()
        thoughts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Welcome to today's meals, eat with a smile!"))

    val aiHint: StateFlow<AiHint?> = combine(dailyHealth, _waterToday) { health: DailyHealth, water: Int ->
        val groups = health.groups
        val kcal = health.kcal
        val hour = LocalDateTime.now().hour

        when {
            groups.isEmpty() -> null
            !groups.any { it == "vegetable" || it == "fruit" || it == "mixed" } -> 
                AiHint("🍎", "Missing some greens today? A small fruit goes a long way in keeping you fresh!")
            !groups.any { it == "protein" || it == "mixed" } -> 
                AiHint("💪", "Chef AI says: A little protein keeps you full longer. Maybe add an egg or some beans?")
            kcal > 0 && kcal < 1400 -> 
                AiHint("🔥", "Energy check! You're a bit low today. Treat yourself to a heartier dinner, you deserve it!")
            water < 4 && hour > 12 -> 
                AiHint("💧", "Hydration break! Grab a glass of water, Comrade. Your focus depends on it.")
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            while (true) {
                val now = LocalDate.now()
                val today = now.format(dateSeparator)
                if (today != prefs.getString("last_reset_day", "")) {
                    resetDay()
                    _currentDay.value = today
                    prefs.edit().putString("last_reset_day", today).apply()
                }

                val sunday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val currentWeekKey = sunday.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"))
                
                if (currentWeekKey != prefs.getString("last_generated_week", "") || _lockedWeeklyPlanIds.value.isEmpty()) {
                    generateAndLockWeeklyPlan()
                    prefs.edit().putString("last_generated_week", currentWeekKey).apply()
                }

                delay(RESET_DELAY_MS)
            }
        }
        auth.addAuthStateListener(authStateListener)
        if (_groupCode.value.isNotBlank()) setupFirebaseSync(_groupCode.value)
    }

    private fun generateAndLockWeeklyPlan() {
        viewModelScope.launch {
            val combos = allCombinations.first()
            val baseMeals = allBaseMeals.first()
            if (combos.isEmpty()) return@launch

            val now = LocalDate.now()
            val sunday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            val seed = sunday.year * 10000L + sunday.monthValue * 100L + sunday.dayOfMonth
            val weekRandom = Random(seed)

            val lockedPlan = mutableListOf<Map<String, Int?>>()
            val weekHistory = mutableMapOf<Int, Int>() 

            repeat(7) { i ->
                val isSpecial = (i == 0 || i == 3 || i == 5 || i == 6) // Sun, Wed, Fri, Sat
                val dayPlan = calculatePlanForDay(combos, baseMeals, _groupSize.value, isSpecial, weekRandom, weekHistory)
                lockedPlan.add(dayPlan.mapValues { it.value?.id })
                dayPlan.values.filterNotNull().forEach { weekHistory[it.id] = (weekHistory[it.id] ?: 0) + 1 }
            }
            saveLockedPlan(lockedPlan)
            _lockedWeeklyPlanIds.value = lockedPlan
        }
    }

    private fun calculatePlanForDay(
        combos: List<MealCombination>, 
        bm: List<BaseMeal>, 
        size: Int, 
        forceSpecial: Boolean, 
        rand: Random,
        weekHistory: Map<Int, Int>
    ): Map<String, MealCombination?> {
        val plan = mutableMapOf<String, MealCombination?>()
        val usedIngsToday = mutableSetOf<String>()
        
        listOf("Breakfast", "Lunch", "Dinner").forEach { time ->
            val candidates = combos.filter { it.targetTime == time && !it.baseNames.any { b -> usedIngsToday.contains(b.lowercase().trim()) } }
            val pick = candidates.maxByOrNull { combo ->
                var score = calculateScore(combo, bm, size, forceSpecial)
                score -= (weekHistory[combo.id] ?: 0) * PENALTY_REPEATED_IN_WEEK
                score + rand.nextDouble(0.0, LUCK_FACTOR_MAX) 
            }
            plan[time] = pick
            pick?.baseNames?.forEach { usedIngsToday.add(it.lowercase().trim()) }
        }
        return plan
    }

    private fun calculateScore(c: MealCombination, bm: List<BaseMeal>, size: Int, isSpecialDay: Boolean): Double {
        val shareResult = getDetailedShare(c, bm, size)
        val share = shareResult.totalPrice
        val budget = _budgetPerPerson.value
        
        var score = 10000.0 // Baseline
        
        // 1. Variety Logic (Most Critical)
        val now = System.currentTimeMillis()
        val hoursSinceEaten = if (c.lastEaten == 0L) 1000L else (now - c.lastEaten) / (1000 * 60 * 60)
        
        if (hoursSinceEaten < 48) {
            score -= PENALTY_RECENTLY_EATEN
        } else {
            // Reward variety: linear boost for time since last eaten
            score += (hoursSinceEaten.toDouble().coerceAtMost(500.0) * 20) * WEIGHT_VARIETY 
        }

        // 2. Affordability logic
        val costFactor = if (share <= 0) 45.0 else share
        score += (4000.0 / (costFactor + 1.0)) * WEIGHT_AFFORDABILITY
        
        // 3. Elastic Budget penalty
        if (budget != null && share > budget) {
            val overage = share - budget
            score -= (PENALTY_OVER_BUDGET + (overage * 20))
        }

        // 4. Special Logic
        if (isSpecialDay) {
            if (c.isSpecial) score += BONUS_SPECIAL
        } else {
            if (c.isSpecial) score -= (BONUS_SPECIAL * 1.5)
        }
        
        if (c.targetTime == "Lunch" && c.name.contains("Ugali", true)) score -= 5000.0
        
        return score
    }

    data class ShareResult(val totalPrice: Double, val hasMissing: Boolean)

    private fun getDetailedShare(c: MealCombination, bm: List<BaseMeal>, s: Int): ShareResult {
        var total = 0.0
        var missing = false
        c.baseNames.forEach { name ->
            val match = bm.find { it.name.trim().equals(name.trim(), true) && it.numPeople == s } 
                ?: bm.find { it.name.trim().equals(name.trim(), true) }
            if (match != null) total += match.pricePerPerson else missing = true
        }
        return ShareResult(total, missing)
    }

    fun getIndividualShare(c: MealCombination, bm: List<BaseMeal>, s: Int): Double? {
        val res = getDetailedShare(c, bm, s)
        return if (res.totalPrice == 0.0 && res.hasMissing) null else res.totalPrice
    }

    private fun saveLockedPlan(plan: List<Map<String, Int?>>) = prefs.edit().putString("locked_weekly_plan", gson.toJson(plan)).apply()
    private fun loadLockedPlan(): List<Map<String, Int?>> {
        val json = prefs.getString("locked_weekly_plan", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<Map<String, Int?>>>() {}.type)
    }
    private fun dayPlanFromIds(map: Map<String, Int?>, combos: List<MealCombination>) = map.mapValues { entry -> combos.find { combo -> combo.id == entry.value } }

    fun setGroupSize(s: Int) { 
        _groupSize.value = s.coerceIn(1, 10)
        prefs.edit().putInt("group_size", _groupSize.value).apply()
        generateAndLockWeeklyPlan() 
    }

    fun setBudget(b: Double?) { _budgetPerPerson.value = b }
    fun toggleTheme() { _isDarkTheme.value = !_isDarkTheme.value; prefs.edit().putBoolean("is_dark_theme", _isDarkTheme.value).apply() }
    
    fun logWater() {
        _waterToday.value = (_waterToday.value + 1).coerceAtMost(12)
        prefs.edit().putInt("water_today", _waterToday.value).apply()
    }

    fun sendChatMessage(query: String) {
        val userMsg = ChatMessage(query, true)
        _chatMessages.value = _chatMessages.value + userMsg
        
        viewModelScope.launch {
            delay(400)
            val response = aiRespond(query)
            _chatMessages.value = _chatMessages.value + ChatMessage(response, false)
        }
    }

    private fun aiRespond(query: String): String {
        val q = query.lowercase().trim()
        val health = dailyHealth.value
        val kcal = health.kcal
        val groups = health.groups
        val water = _waterToday.value
        
        // Humanized Chef AI responses
        FoodDatabase.DATA.keys.forEach { kw ->
            if (q.contains(kw) && (q.contains("calorie") || q.contains("kcal") || q.contains("how many") || q.contains("much"))) {
                val d = FoodDatabase.DATA[kw]!!
                return "${d.emoji} <b>${kw.replaceFirstChar { it.uppercase() }}</b> — it's about ${d.kcal} kcal. It's a <b>${d.label}</b> food. ${
                    when(d.group) {
                        "protein" -> "Perfect for keeping you strong and focused!"
                        "vegetable", "fruit" -> "Great choice! Your body loves those vitamins."
                        "carb" -> "That'll give you plenty of energy to push through."
                        else -> "Enjoy your meal!"
                    }
                }"
            }
        }

        if (q.contains("balance") || q.contains("balanced")) {
            val hitCount = listOf("carb", "protein", "vegetable", "fruit").count { t -> groups.any { it == t || it == "mixed" } }
            val bal = (hitCount.toFloat() / 4f * 100).toInt()
            return "Looking at your plate, it's about $bal% balanced. ${if(bal >= 75) "You're doing great!" else "Maybe throw in some " + (if(!groups.contains("protein")) "beans or eggs " else "") + (if(!groups.any { it == "fruit" || it == "vegetable" }) "fruits or veggies" else "") + " next time?"}"
        }

        if (q.contains("calorie") || q.contains("kcal")) {
            return "You've got about ~$kcal kcal in your plan for today. ${if(kcal < 1400) "A bit light, don't you think? You might need a snack later." else if(kcal > 2400) "That's a solid amount of energy! You're ready for anything." else "Looks like a good balance!"}"
        }

        if (q.contains("water") || q.contains("drink") || q.contains("hydrat")) {
            return "You've had $water glasses today. ${if(water >= 8) "You're a hydration hero! 🎉" else "Keep sipping! Your brain will thank you for it."}"
        }

        if (q.contains("protein")) return "💪 For protein on a budget, go for beans, eggs, lentils, or groundnuts. They are absolute lifesavers!"
        if (q.contains("fruit") || q.contains("vitamin")) return "🍎 Bananas, mangoes, and oranges are easy wins. Even an avocado is great for those healthy fats!"
        if (q.contains("vegetable") || q.contains("veggie") || q.contains("green")) return "🥬 Sukuma wiki, spinach, and cabbage are the real MVPs. Cheap and super healthy!"
        if (q.contains("cheap") || q.contains("budget") || q.contains("afford")) return "💰 Eggs, beans, ugali, and sukuma wiki. This is the ultimate comrade power combo!"

        return "🤖 I'm Chef AI! Ask me about calories, if your plan looks good, or some budget tips. I'm here to help you eat like a king on a student budget!"
    }

    fun insertBaseMeal(name: String, price: Double, serves: Int, id: Int = 0) = viewModelScope.launch { repository.insertBaseMeal(BaseMeal(id, name, price, serves)) }
    fun deleteBaseMeal(meal: BaseMeal) = viewModelScope.launch { repository.deleteBaseMeal(meal) }
    
    fun saveCombination(name: String, baseNames: List<String>, targetTime: String, isSpecial: Boolean, isRare: Boolean) = viewModelScope.launch {
        repository.insertCombination(MealCombination(name = name, baseNames = baseNames, targetTime = targetTime, isSpecial = isSpecial, isRare = isRare))
    }
    fun deleteCombination(combo: MealCombination) = viewModelScope.launch { repository.deleteCombination(combo) }

    fun markAsEaten(c: MealCombination) = viewModelScope.launch { repository.updateCombination(c.copy(frequency = c.frequency + 1, lastEaten = System.currentTimeMillis())) }
    fun resetDay() { 
        _eatenTodayIngredients.value = emptySet()
        _eatenTodayComboIds.value = emptySet()
        _waterToday.value = 0
        prefs.edit().putInt("water_today", 0).apply()
    }
    fun updateGroupCode(code: String) { _groupCode.value = code.uppercase(); prefs.edit().putString("group_code", code).apply(); if (code.isNotBlank()) setupFirebaseSync(code) }

    fun joinGroup(code: String, onResult: (Boolean, String) -> Unit) { 
        // Sync implementation would go here
    }

    fun uploadInventoryListToFirebase(meals: List<BaseMeal>) {
        val code = _groupCode.value
        if (code.isBlank()) return
        val ref = firebase.getReference("groups").child(code).child("inventory")
        meals.forEach { meal -> ref.child(meal.id.toString()).setValue(meal) }
    }

    fun uploadComboToFirebase(combo: MealCombination) {
        val code = _groupCode.value
        if (code.isBlank()) return
        firebase.getReference("groups").child(code).child("meals").child(combo.id.toString()).setValue(combo)
    }

    private fun setupFirebaseSync(code: String) {
        _syncStatus.value = SyncStatus.SYNCING
        currentGroupNode = code
        mealsListener = firebase.getReference("groups").child(code).child("meals").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch {
                    val local = allCombinations.first()
                    s.children.forEach { c ->
                        val map = c.value as? Map<*, *> ?: return@forEach
                        val name = map["name"] as? String ?: ""
                        val ings = (map["baseNames"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        if (local.none { it.name.equals(name, true) && it.baseNames == ings }) {
                            repository.insertCombination(MealCombination(name = name, baseNames = ings, targetTime = map["targetTime"] as? String ?: "Breakfast"))
                        }
                    }
                    _syncStatus.value = SyncStatus.SYNCED
                }
            }
            override fun onCancelled(e: DatabaseError) { _syncStatus.value = SyncStatus.ERROR }
        })
    }

    private fun observeUserGroups(uid: String) {
        userGroupsListener = firebase.getReference("users").child(uid).child("groups").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                _userGroups.value = s.children.map { GroupInfo(it.key ?: "", it.child("name").getValue(String::class.java) ?: "Unknown") }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        currentGroupNode?.let { code ->
            mealsListener?.let { firebase.getReference("groups").child(code).child("meals").removeEventListener(it) }
            inventoryListener?.let { firebase.getReference("groups").child(code).child("inventory").removeEventListener(it) }
        }
        val user = auth.currentUser
        if (user != null && userGroupsListener != null) {
            firebase.getReference("users").child(user.uid).child("groups").removeEventListener(userGroupsListener!!)
        }
    }
}
