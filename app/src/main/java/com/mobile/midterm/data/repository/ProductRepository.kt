package com.mobile.midterm.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mobile.midterm.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ProductRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collectionRef = firestore.collection("products")
    private val mainHandler = Handler(Looper.getMainLooper())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener: ListenerRegistration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.map { doc ->
                    Product.fromDocument(doc)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun addProduct(
        tenSanPham: String,
        loaiSanPham: String,
        gia: Long,
        image: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val product = Product(
            tenSanPham = tenSanPham,
            loaiSanPham = loaiSanPham,
            gia = gia,
            image = image
        )
        // Lưu đúng 4 trường theo đề bài
        collectionRef.add(product.toMap())
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage)
            }
    }

    fun updateProduct(
        id: String,
        tenSanPham: String,
        loaiSanPham: String,
        gia: Long,
        image: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val product = Product(
            id = id,
            tenSanPham = tenSanPham,
            loaiSanPham = loaiSanPham,
            gia = gia,
            image = image
        )
        collectionRef.document(id).set(product.toMap())
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage)
            }
    }

    fun deleteProduct(id: String, onComplete: (Boolean, String?) -> Unit) {
        collectionRef.document(id).delete()
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage)
            }
    }

    /**
     * Tải ảnh lên API hosting miễn phí hoàn toàn không cần đăng ký tài khoản / không cần API Key:
     * 1. Thử PixelVault Quickstart API (https://api.pixelvault.dev/v1/images)
     * 2. Thử TmpFiles API (https://tmpfiles.org/api/v1/upload)
     * 3. Fallback sang Base64 nếu mất mạng hoặc API bận, đảm bảo ảnh luôn hiển thị 100% trong app.
     */
    fun uploadOrProcessImage(
        context: Context,
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onResult: (String?) -> Unit
    ) {
        onProgress(true)

        // Đọc bytes ảnh và nén nhẹ nếu cần
        val imageBytes: ByteArray? = try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val maxDim = 800
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                val targetH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                val scaled = Bitmap.createScaledBitmap(bitmap, targetW.coerceAtLeast(10), targetH.coerceAtLeast(10), true)

                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                baos.toByteArray()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        if (imageBytes == null) {
            onProgress(false)
            onResult(null)
            return
        }

        // Bước 1: Upload lên PixelVault (Miễn phí 100%, không cần tài khoản, trả về direct CDN URL)
        val filename = "image_${System.currentTimeMillis()}.jpg"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                filename,
                imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.pixelvault.dev/v1/images")
            .post(requestBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Thử dịch vụ dự phòng thứ 2
                uploadToTmpFiles(imageBytes, filename, context, uri, onProgress, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                    try {
                        val json = JSONObject(bodyStr)
                        val directUrl = json.getJSONObject("data").getString("url")
                        mainHandler.post {
                            onProgress(false)
                            onResult(directUrl)
                        }
                        return
                    } catch (_: Exception) {}
                }
                // Thử dịch vụ dự phòng thứ 2
                uploadToTmpFiles(imageBytes, filename, context, uri, onProgress, onResult)
            }
        })
    }

    private fun uploadToTmpFiles(
        imageBytes: ByteArray,
        filename: String,
        context: Context,
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onResult: (String?) -> Unit
    ) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                filename,
                imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://tmpfiles.org/api/v1/upload")
            .post(requestBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                fallbackToBase64(imageBytes, onProgress, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                    try {
                        val json = JSONObject(bodyStr)
                        val rawUrl = json.getJSONObject("data").getString("url")
                        // Đổi https://tmpfiles.org/xxx/file.jpg sang direct link https://tmpfiles.org/dl/xxx/file.jpg
                        val directUrl = rawUrl.replace("tmpfiles.org/", "tmpfiles.org/dl/")
                        mainHandler.post {
                            onProgress(false)
                            onResult(directUrl)
                        }
                        return
                    } catch (_: Exception) {}
                }
                fallbackToBase64(imageBytes, onProgress, onResult)
            }
        })
    }

    private fun fallbackToBase64(
        imageBytes: ByteArray,
        onProgress: (Boolean) -> Unit,
        onResult: (String?) -> Unit
    ) {
        mainHandler.post {
            try {
                val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                onProgress(false)
                onResult("data:image/jpeg;base64,$base64")
            } catch (e: Exception) {
                onProgress(false)
                onResult(null)
            }
        }
    }

    fun exportToJson(products: List<Product>): String {
        val array = JSONArray()
        for (p in products) {
            val obj = JSONObject()
            obj.put("tenSanPham", p.tenSanPham)
            obj.put("loaiSanPham", p.loaiSanPham)
            obj.put("gia", p.gia)
            obj.put("image", p.image)
            array.put(obj)
        }
        return array.toString(2)
    }
}
