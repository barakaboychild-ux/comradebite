package com.comradebite.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader

@Database(entities = [BaseMeal::class, MealCombination::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MealDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile
        private var INSTANCE: MealDatabase? = null

        fun getDatabase(context: Context): MealDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MealDatabase::class.java,
                    "meal_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dbInstance = getDatabase(context)
                            populateInitialDataFromAssets(context, dbInstance.mealDao())
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialDataFromAssets(context: Context, dao: MealDao) {
            try {
                val inputStream = context.assets.open("initial_data.json")
                val reader = InputStreamReader(inputStream)
                
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = Gson().fromJson(reader, type)

                // Safe parsing for Base Meals
                val baseMealsRaw = data["base_meals"] as? List<*>
                baseMealsRaw?.forEach { item ->
                    val map = item as? Map<*, *>
                    if (map != null) {
                        dao.insertBaseMeal(BaseMeal(
                            name = map["name"] as String,
                            totalPrice = (map["totalPrice"] as Double),
                            numPeople = (map["numPeople"] as Double).toInt()
                        ))
                    }
                }

                // Safe parsing for Combinations
                val combinationsRaw = data["meal_combinations"] as? List<*>
                combinationsRaw?.forEach { item ->
                    val map = item as? Map<*, *>
                    if (map != null) {
                        @Suppress("UNCHECKED_CAST")
                        dao.insertCombination(MealCombination(
                            name = map["name"] as String,
                            baseNames = map["baseNames"] as List<String>,
                            targetTime = map["targetTime"] as String,
                            isSpecial = map["isSpecial"] as? Boolean ?: false,
                            isRare = map["isRare"] as? Boolean ?: false
                        ))
                    }
                }
                
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
