package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.database.AlternativeProduct
import com.example.data.database.IngredientInfo
import com.example.data.database.ScanLog
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val TAG = "GeminiClient"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    // JSON response parser helper to parse structured Gemini outputs
    @com.squareup.moshi.JsonClass(generateAdapter = true)
    data class GeminiStructuredOutput(
        val productName: String,
        val trustScore: Int,
        val adulterationStatus: String,
        val adulterationReason: String,
        val manufacturer: String?,
        val batchNumber: String?,
        val expiryDate: String?,
        val officialRegistration: String?,
        val consumerReportsSummary: String?,
        val fakeProductReasoning: String?,
        val ingredients: List<IngredientInfo>,
        val alternatives: List<AlternativeProduct>
    )

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeProduct(
        bitmap: Bitmap?,
        inputText: String?,
        barcode: String? = null
    ): ScanLog? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or holds the placeholder value!")
            return@withContext null
        }

        val prompt = buildPrompt(inputText, barcode, bitmap != null)

        val partsList = mutableListOf<Part>()
        partsList.add(Part(text = prompt))
        if (bitmap != null) {
            partsList.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64())))
        }

        val systemPrompt = """
            You are TrustLens, an expert AI Food Safety, Adulteration, and Nutritionist Scanner.
            Analyze the input (ingredient listing image, barcode, and/or product description text) and assess:
            1. OCR Extraction: Read and identify the name of the product and all visual ingredients.
            2. Ingredient Explanation: Explain every single ingredient in extremely clear, simple language suitable for consumers. Avoid just naming them. Replace or translate additive codes (e.g., Sodium Benzoate instead of just INS 211). Highlight harmful chemical additives/preservatives.
            3. Adulteration Risk Checker: Estimate indicators for categories of potential adulterants, such as:
               - Artificial milk (urea, starch, detergents, etc.)
               - Fake honey (sugar syrup, corn syrup)
               - Adulterated spices (metanil yellow in turmeric, chalk powder, starch)
               - Synthetic ghee (hydrolyzed vegetable oil, palm oil dilution, animal fat)
               - Excess food colors (unauthorized industrial dyes)
               - Palm oil substitution (cheap default fats replacing healthy oils)
               Classify adulterationStatus strictly to one of:
               "Likely authentic based on available data"
               "Potential risk—laboratory testing required for confirmation"
            4. Better Alternative Finder: Search knowledge and provide 1-2 realistic, highly direct alternative products or specific swaps that have:
               - Less sugar, less sodium, better protein ratio, or better value. For each alternative, provide specific percentage differences if possible (e.g. "30% less sodium").
            5. Trust Score (0-100): Evaluate the product and give a quantitative trust score. Deduct heavily for:
               - Highly ultra-processed status (NOVA group 4)
               - Potentially toxic or risky preservatives/additives (hazardous colorants, sodium benzoate, titanium dioxide, BHA/BHT, synthetic sweeteners)
               - High level of sodium, sugar, or trans fats
               - Adulteration risk or missing official registration
            6. Fake Product & Registration: Double check manufacturer info, batch details/expiry format consistency, and registration context. Detail this under officialRegistration and fakeProductReasoning.
            
            Return ONLY a valid JSON object matching the requested schema. No markdown formatting outside of JSON, and no explanation text outside of JSON.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = partsList)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Raw Response: $jsonText")
                val adapter = moshi.adapter(GeminiStructuredOutput::class.java)
                val structuredResult = adapter.fromJson(jsonText)
                if (structuredResult != null) {
                    val ingredientsAdapter = moshi.adapter<List<IngredientInfo>>(
                        com.squareup.moshi.Types.newParameterizedType(List::class.java, IngredientInfo::class.java)
                    )
                    val alternativesAdapter = moshi.adapter<List<AlternativeProduct>>(
                        com.squareup.moshi.Types.newParameterizedType(List::class.java, AlternativeProduct::class.java)
                    )

                    return@withContext ScanLog(
                        productName = structuredResult.productName,
                        barcode = barcode ?: structuredResult.batchNumber,
                        trustScore = structuredResult.trustScore,
                        imageUrl = null, // Will set locally
                        adulterationStatus = structuredResult.adulterationStatus,
                        adulterationReason = structuredResult.adulterationReason,
                        manufacturer = structuredResult.manufacturer,
                        batchNumber = structuredResult.batchNumber,
                        expiryDate = structuredResult.expiryDate,
                        officialRegistration = structuredResult.officialRegistration,
                        consumerReportsSummary = structuredResult.consumerReportsSummary ?: "No consumer reports verified",
                        fakeProductReasoning = structuredResult.fakeProductReasoning,
                        ingredientsJson = ingredientsAdapter.toJson(structuredResult.ingredients),
                        alternativesJson = alternativesAdapter.toJson(structuredResult.alternatives)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
        }
        return@withContext null
    }

    private fun buildPrompt(text: String?, barcode: String?, hasImage: Boolean): String {
        return buildString {
            append("Please analyze the food or cosmetic product. ")
            if (text != null && text.isNotBlank()) {
                append("User provided text/details: \"$text\". ")
            }
            if (barcode != null && barcode.isNotBlank()) {
                append("Detected Barcode number is \"$barcode\". ")
            }
            if (hasImage) {
                append("An image of the product ingredients list/packaging has been provided. Try to do OCR on the image. ")
            }
            append("\nProvide analysis structured exactly in JSON format, containing values for:\n")
            append("1. productName\n")
            append("2. trustScore (0 to 100)\n")
            append("3. adulterationStatus\n")
            append("4. adulterationReason\n")
            append("5. manufacturer\n")
            append("6. batchNumber\n")
            append("7. expiryDate\n")
            append("8. officialRegistration\n")
            append("9. consumerReportsSummary\n")
            append("10. fakeProductReasoning\n")
            append("11. ingredients: Array of { name, chemicalName, explanation, isHarmful (Boolean), category, hazardLevel ('Low', 'Medium', 'High') }\n")
            append("12. alternatives: Array of { name, sugarDiff, sodiumDiff, proteinDiff, priceDiff, reasonToBuy }\n")
        }
    }
}
