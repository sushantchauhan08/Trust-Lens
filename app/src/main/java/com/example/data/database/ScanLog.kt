package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "scan_logs")
data class ScanLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productName: String,
    val barcode: String?,
    val trustScore: Int,
    val imageUrl: String?,
    val adulterationStatus: String, // "Likely authentic based on available data" or "Potential risk—laboratory testing required for confirmation"
    val adulterationReason: String,
    val manufacturer: String?,
    val batchNumber: String?,
    val expiryDate: String?,
    val officialRegistration: String?, // e.g. "FSSAI Registered - Valid"
    val consumerReportsSummary: String?, // e.g. "4.2/5 stars from 120 reports"
    val fakeProductReasoning: String?,  // Details of batch/registration check
    val ingredientsJson: String, // JSON serialization of List<IngredientInfo>
    val alternativesJson: String, // JSON serialization of List<AlternativeProduct>
    val timestamp: Long = System.currentTimeMillis()
)

data class IngredientInfo(
    val name: String,
    val chemicalName: String?, // e.g., "INS 211" or "E211"
    val explanation: String,
    val isHarmful: Boolean,
    val category: String, // e.g., "Preservative", "Sweetener", "Thickener"
    val hazardLevel: String // "Low", "Medium", "High"
)

data class AlternativeProduct(
    val name: String,
    val sugarDiff: String?, // e.g. "30% less sugar"
    val sodiumDiff: String?, // e.g. "50% less sodium"
    val proteinDiff: String?, // e.g. "2x more protein"
    val priceDiff: String?, // e.g. "+5% cost" or "Better value"
    val reasonToBuy: String
)

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    @TypeConverter
    fun fromIngredientList(value: List<IngredientInfo>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, IngredientInfo::class.java)
        val adapter = moshi.adapter<List<IngredientInfo>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toIngredientList(value: String): List<IngredientInfo> {
        val type = Types.newParameterizedType(List::class.java, IngredientInfo::class.java)
        val adapter = moshi.adapter<List<IngredientInfo>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromAlternativeList(value: List<AlternativeProduct>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, AlternativeProduct::class.java)
        val adapter = moshi.adapter<List<AlternativeProduct>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toAlternativeList(value: String): List<AlternativeProduct> {
        val type = Types.newParameterizedType(List::class.java, AlternativeProduct::class.java)
        val adapter = moshi.adapter<List<AlternativeProduct>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }
}
