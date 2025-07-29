package com.iss

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.FloatBuffer
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Arrays
import java.util.Collections
import kotlin.math.exp
import kotlin.math.max

class PredActivity : AppCompatActivity() {

    private lateinit var etFloorArea: EditText
    private lateinit var etTown: EditText
    private lateinit var etFlatType: EditText
    private lateinit var etFlatModel: EditText
    private lateinit var etStoreyRange: EditText
    private lateinit var etRemainingLease: EditText
    private lateinit var etMonth: EditText
    private lateinit var etLeaseCommenceDate: EditText
    private lateinit var btnPredict: Button
    private lateinit var tvResult: TextView

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    private val PRICE_RANGES = arrayOf(
        "<= $350,000",
        "$350,001 - $500,000",
        "$500,001 - $700,000",
        "$700,001 - $1,000,000",
        "> $1,000,000"
    )

    private val SCALER_MEANS = floatArrayOf(
        96.8740658027681f, 2020.2176078135776f, 29.45240004490562f, 8.673656439946754f, 74.294599230724f
    )
    private val SCALER_STDS = floatArrayOf(
        24.057867459941118f, 2.9905908513913864f, 13.933463465912096f, 5.855236447166976f, 13.732032823995471f
    )

    private val ONEHOT_CATEGORIES = listOf(
        arrayOf("ANG MO KIO", "BEDOK", "BISHAN", "BUKIT BATOK", "BUKIT MERAH", "BUKIT PANJANG", "BUKIT TIMAH", "CENTRAL AREA", "CHOA CHU KANG", "CLEMENTI", "GEYLANG", "HOUGANG", "JURONG EAST", "JURONG WEST", "KALLANG/WHAMPOA", "MARINE PARADE", "PASIR RIS", "PUNGGOL", "QUEENSTOWN", "SEMBAWANG", "SENGKANG", "SERANGOON", "TAMPINES", "TOA PAYOH", "WOODLANDS", "YISHUN"),
        arrayOf("1 ROOM", "2 ROOM", "3 ROOM", "4 ROOM", "5 ROOM", "EXECUTIVE", "MULTI-GENERATION"),
        arrayOf("2-ROOM", "3GEN", "ADJOINED FLAT", "APARTMENT", "DBSS", "IMPROVED", "IMPROVED-MAISONETTE", "MAISONETTE", "MODEL A", "MODEL A-MAISONETTE", "MODEL A2", "MULTI GENERATION", "NEW GENERATION", "PREMIUM APARTMENT", "PREMIUM APARTMENT LOFT", "PREMIUM MAISONETTE", "SIMPLIFIED", "STANDARD", "TERRACE", "TYPE S1", "TYPE S2"),
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pred_test)

        etFloorArea = findViewById(R.id.etFloorArea)
        etTown = findViewById(R.id.etTown)
        etFlatType = findViewById(R.id.etFlatType)
        etFlatModel = findViewById(R.id.etFlatModel)
        etStoreyRange = findViewById(R.id.etStoreyRange)
        etRemainingLease = findViewById(R.id.etRemainingLease)
        etMonth = findViewById(R.id.etMonth)
        etLeaseCommenceDate = findViewById(R.id.etLeaseCommenceDate)
        btnPredict = findViewById(R.id.btnPredict)
        tvResult = findViewById(R.id.tvResult)

        etFloorArea.setText("95.0")
        etTown.setText("TAMPINES")
        etFlatType.setText("4 ROOM")
        etFlatModel.setText("IMPROVED")
        etStoreyRange.setText("07 TO 09")
        etRemainingLease.setText("75 years 00 months")
        etMonth.setText("2019-06")
        etLeaseCommenceDate.setText("1990")

        GlobalScope.launch(Dispatchers.IO) {
            Log.d("ONNXRT_DEBUG", "Starting asynchronous ONNX model loading...")
            try {
                ortEnvironment = OrtEnvironment.getEnvironment()
                val sessionOptions = OrtSession.SessionOptions()
                Log.d("ONNXRT_DEBUG", "OrtEnvironment created successfully.")

                val modelPath = assetFilePath(applicationContext, "house_price_classifier.onnx")
                Log.d("ONNXRT_DEBUG", "Model file path: $modelPath")

                ortSession = ortEnvironment?.createSession(modelPath, sessionOptions)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PredActivity, "ONNX model loaded successfully!", Toast.LENGTH_SHORT).show()
                    Log.d("ONNXRT_DEBUG", "ONNX model loaded successfully!")
                }
            } catch (e: Exception) {
                Log.e("ONNXRT", "ONNX model loading failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PredActivity, "ONNX model loading failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ONNXRT_DEBUG", "ONNX model loading exception: ${e.message}")
                }
            }
        }

        btnPredict.setOnClickListener {
            performPrediction()
        }
    }

    private fun performPrediction() {
        Log.d("ONNXRT_DEBUG", "performPrediction started.")
        if (ortSession == null) {
            Toast.makeText(this, "Model not loaded yet, please wait.", Toast.LENGTH_SHORT).show()
            Log.w("ONNXRT_DEBUG", "ortSession is null, model not loaded.")
            return
        }

        val floorAreaStr = etFloorArea.text.toString()
        val town = etTown.text.toString().trim().uppercase()
        val flatType = etFlatType.text.toString().trim().uppercase()
        val flatModel = etFlatModel.text.toString().trim().uppercase()
        val storeyRange = etStoreyRange.text.toString().trim()
        val remainingLease = etRemainingLease.text.toString().trim()
        val month = etMonth.text.toString().trim()
        val leaseCommenceDateStr = etLeaseCommenceDate.text.toString().trim()

        if (floorAreaStr.isEmpty() || town.isEmpty() || flatType.isEmpty() || flatModel.isEmpty() ||
            storeyRange.isEmpty() || remainingLease.isEmpty() || month.isEmpty() || leaseCommenceDateStr.isEmpty()) {
            Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show()
            Log.w("ONNXRT_DEBUG", "Input fields are empty.")
            return
        }

        try {
            val floorArea = floorAreaStr.toFloat()
            val leaseCommenceDate = leaseCommenceDateStr.toInt()

            Log.d("ONNXRT_DEBUG", "Starting data preprocessing (synchronous).")
            val preprocessedFeatures = preprocessInputData(
                floorArea, town, flatType, flatModel,
                storeyRange, remainingLease, month, leaseCommenceDate
            )
            Log.d("ONNXRT_DEBUG", "Data preprocessing complete. Feature count: ${preprocessedFeatures.size}")
            Log.d("ONNXRT_DEBUG", "Preprocessed features (first 10): ${Arrays.toString(preprocessedFeatures.sliceArray(0..minOf(preprocessedFeatures.size - 1, 9)))}")

            if (preprocessedFeatures.any { it.isNaN() }) {
                val errorMessage = "ERROR: Preprocessed features contain NaN values!"
                Log.e("ONNXRT_DEBUG", errorMessage)
                tvResult.text = "Prediction failed: $errorMessage"
                Toast.makeText(this, "Prediction failed: $errorMessage", Toast.LENGTH_LONG).show()
                return
            }

            Log.d("ONNXRT_DEBUG", "Starting ONNX inference (synchronous).")
            val predictedRange = runInference(preprocessedFeatures)
            Log.d("ONNXRT_DEBUG", "runInference returned: $predictedRange")

            tvResult.text = "Prediction Result: $predictedRange"
            Toast.makeText(this@PredActivity, "Prediction complete: $predictedRange", Toast.LENGTH_SHORT).show()

        } catch (e: NumberFormatException) {
            Log.e("ONNXRT_DEBUG", "NumberFormatException: ${e.message}", e)
            Toast.makeText(this, "Please enter valid number formats!", Toast.LENGTH_SHORT).show()
            tvResult.text = "Prediction failed: Invalid number format"
        } catch (e: Exception) {
            Log.e("ONNXRT_DEBUG", "Unhandled exception in performPrediction: ${e.message}", e)
            tvResult.text = "Prediction failed: ${e.message}"
            Toast.makeText(this, "Prediction failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            Log.d("ONNXRT_DEBUG", "File already exists in internal storage: ${file.absolutePath}")
            return file.absolutePath
        }

        Log.d("ONNXRT_DEBUG", "Copying file from assets: $assetName to ${file.absolutePath}")
        context.assets.open(assetName).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    private fun preprocessInputData(
        floorArea: Float,
        town: String,
        flatType: String,
        flatModel: String,
        storeyRange: String,
        remainingLease: String,
        month: String,
        leaseCommenceDate: Int
    ): FloatArray {
        val storeyMid = convertStoreyRange(storeyRange)
        val remainingLeaseYears = parseRemainingLease(remainingLease)
        val year = parseMonthToYear(month)
        val flatAge = (2025 - leaseCommenceDate).toFloat()

        Log.d("ONNXRT_DEBUG", "FE: storeyMid=$storeyMid, remainingLeaseYears=$remainingLeaseYears, year=$year, flatAge=$flatAge")

        val finalStoreyMid = storeyMid ?: 0.0f
        val finalRemainingLeaseYears = remainingLeaseYears ?: 0.0f

        val numericalFeatures = floatArrayOf(
            (floorArea - SCALER_MEANS[0]) / SCALER_STDS[0],
            (year - SCALER_MEANS[1]) / SCALER_STDS[1],
            (flatAge - SCALER_MEANS[2]) / SCALER_STDS[2],
            (finalStoreyMid - SCALER_MEANS[3]) / SCALER_STDS[3],
            (finalRemainingLeaseYears - SCALER_MEANS[4]) / SCALER_STDS[4]
        )
        Log.d("ONNXRT_DEBUG", "FE: numericalFeatures=${Arrays.toString(numericalFeatures)}")

        val oneHotFeatures = mutableListOf<Float>()

        val townCategories = ONEHOT_CATEGORIES[0]
        val townOneHot = FloatArray(townCategories.size) { 0.0f }
        val townIndex = townCategories.indexOf(town)
        if (townIndex != -1) {
            townOneHot[townIndex] = 1.0f
        } else {
            Log.w("ONNXRT_DEBUG", "Town '$town' not found in categories. All zeros for town one-hot.")
        }
        oneHotFeatures.addAll(townOneHot.toList())

        val flatTypeCategories = ONEHOT_CATEGORIES[1]
        val flatTypeOneHot = FloatArray(flatTypeCategories.size) { 0.0f }
        val flatTypeIndex = flatTypeCategories.indexOf(flatType)
        if (flatTypeIndex != -1) {
            flatTypeOneHot[flatTypeIndex] = 1.0f
        } else {
            Log.w("ONNXRT_DEBUG", "FlatType '$flatType' not found in categories. All zeros for flat_type one-hot.")
        }
        oneHotFeatures.addAll(flatTypeOneHot.toList())

        val flatModelCategories = ONEHOT_CATEGORIES[2]
        val flatModelOneHot = FloatArray(flatModelCategories.size) { 0.0f }
        val flatModelIndex = flatModelCategories.indexOf(flatModel)
        if (flatModelIndex != -1) {
            flatModelOneHot[flatModelIndex] = 1.0f
        } else {
            Log.w("ONNXRT_DEBUG", "FlatModel '$flatModel' not found in categories. All zeros for flat_model one-hot.")
        }
        oneHotFeatures.addAll(flatModelOneHot.toList())

        val finalFeatures = FloatArray(numericalFeatures.size + oneHotFeatures.size)
        System.arraycopy(numericalFeatures, 0, finalFeatures, 0, numericalFeatures.size)
        for (i in oneHotFeatures.indices) {
            finalFeatures[numericalFeatures.size + i] = oneHotFeatures[i]
        }

        Log.d("ONNXRT", "Preprocessed feature count: ${finalFeatures.size}")

        return finalFeatures
    }

    private fun convertStoreyRange(s: String): Float? {
        return try {
            val parts = s.split(" TO ")
            val lower = parts[0].toFloat()
            val upper = parts[1].toFloat()
            (lower + upper) / 2.0f
        } catch (e: Exception) {
            Log.e("PREPROCESS", "Failed to parse storey range: $s", e)
            null
        }
    }

    private fun parseRemainingLease(leaseStr: String): Float? {
        if (leaseStr.isEmpty()) return null
        return try {
            leaseStr.toFloat()
        } catch (e: NumberFormatException) {
            val cleanedStr = leaseStr.toLowerCase().replace("years", "").replace("year", "").replace("months", "").replace("month", "").trim()
            val parts = cleanedStr.split(" ").filter { it.isNotBlank() }

            var years = 0.0f
            var months = 0.0f

            if (parts.isNotEmpty()) {
                try {
                    years = parts[0].toFloat()
                } catch (e: NumberFormatException) { }
            }
            if (parts.size > 1) {
                try {
                    months = parts[1].toFloat()
                } catch (e: NumberFormatException) { }
            }
            years + months / 12.0f
        } catch (e: Exception) {
            Log.e("PREPROCESS", "Failed to parse remaining lease: $leaseStr", e)
            null
        }
    }

    private fun parseMonthToYear(monthStr: String): Float {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
            YearMonth.parse(monthStr, formatter).year.toFloat()
        } catch (e: Exception) {
            Log.e("PREPROCESS", "Failed to parse month to year: $monthStr", e)
            2015.0f
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0.0f
        val expValues = logits.map { exp(it - maxLogit) }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toFloatArray()
    }

    private fun runInference(preprocessedFeatures: FloatArray): String {
        var predictedRange = "Prediction Failed"
        var inputTensor: OnnxTensor? = null
        var results: OrtSession.Result? = null

        try {
            val inputBuffer = FloatBuffer.wrap(preprocessedFeatures)
            val inputShape = longArrayOf(1, preprocessedFeatures.size.toLong())

            Log.d("ONNXRT_DEBUG", "ONNX Inference: Creating OnnxTensor with shape: ${Arrays.toString(inputShape)}")
            inputTensor = OnnxTensor.createTensor(ortEnvironment, inputBuffer, inputShape)
            Log.d("ONNXRT_DEBUG", "ONNX Inference: OnnxTensor created successfully.")

            val inputs = Collections.singletonMap("input", inputTensor)

            Log.d("ONNXRT_DEBUG", "ONNX Inference: Running ONNX session.")
            results = ortSession?.run(inputs)
            Log.d("ONNXRT_DEBUG", "ONNX Inference: ONNX session completed.")

            val outputOnnxValueOptional = results?.get("output")

            if (outputOnnxValueOptional == null || !outputOnnxValueOptional.isPresent) {
                Log.e("ONNXRT_DEBUG", "ONNX Inference: Could not get output named 'output' or it is empty.")
                predictedRange = "ONNX Output Node Missing/Empty"
                return predictedRange
            }

            val outputOnnxValue = outputOnnxValueOptional.get()

            val outputTensor = outputOnnxValue as? OnnxTensor
            if (outputTensor == null) {
                Log.e("ONNXRT_DEBUG", "ONNX Inference: Obtained output object is not of type OnnxTensor. Actual type: ${outputOnnxValue.javaClass.name}")
                predictedRange = "ONNX Output Type Error"
                return predictedRange
            }

            val outputLogits = outputTensor.floatBuffer?.array()
            if (outputLogits == null || outputLogits.isEmpty()) {
                Log.e("ONNXRT_DEBUG", "ONNX Inference: Model output (logits) is null or floatBuffer conversion failed. outputLogits: ${outputLogits}")
                predictedRange = "ONNX Output Data Empty"
                return predictedRange
            }

            Log.d("ONNXRT_DEBUG", "ONNX Inference: Model output (logits): ${Arrays.toString(outputLogits)}")

            val outputProbabilities = softmax(outputLogits)
            Log.d("ONNXRT_DEBUG", "ONNX Inference: Model output (probabilities): ${Arrays.toString(outputProbabilities)}")

            var predictedClassIndex = -1
            var maxProb = -Float.MAX_VALUE

            for (i in outputProbabilities.indices) {
                if (outputProbabilities[i] > maxProb) {
                    maxProb = outputProbabilities[i]
                    predictedClassIndex = i
                }
            }

            if (predictedClassIndex >= 0 && predictedClassIndex < PRICE_RANGES.size) {
                predictedRange = PRICE_RANGES[predictedClassIndex]
                Log.d("ONNXRT_DEBUG", "ONNX Inference: Predicted class index: $predictedClassIndex, Predicted range: $predictedRange")
            } else {
                Log.e("ONNXRT_DEBUG", "ONNX Inference: Predicted class index ($predictedClassIndex) out of PRICE_RANGES bounds.")
                predictedRange = "Invalid Predicted Class Index"
            }
        } catch (e: Exception) {
            Log.e("ONNXRT", "Error during ONNX inference: ${e.message}", e)
            predictedRange = "Inference Exception: ${e.message}"
            throw e
        } finally {
            inputTensor?.close()
            results?.close()
            Log.d("ONNXRT_DEBUG", "ONNX Inference: Resources closed.")
        }
        return predictedRange
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            ortSession?.close()
            ortEnvironment?.close()
            Log.d("ONNXRT_DEBUG", "ONNX Session and Environment closed.")
        } catch (e: Exception) {
            Log.e("ONNXRT", "Failed to close ONNX resources: ${e.message}", e)
            Log.e("ONNXRT_DEBUG", "ONNX resource closing exception: ${e.message}")
        }
    }
}