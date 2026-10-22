package com.mobile.midterm.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.mobile.midterm.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

class ProductRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val collectionRef = firestore.collection("products")

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
        // Chỉ lưu đúng 4 trường theo đề bài
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
     * Tải ảnh lên Firebase Storage, nếu không khả dụng hoặc lỗi Storage thì chuyển thành Base64
     * để luôn hiển thị và lưu trữ an toàn trong Firestore.
     */
    fun uploadOrProcessImage(
        context: Context,
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onResult: (String?) -> Unit
    ) {
        onProgress(true)
        val filename = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val imageRef = storage.reference.child("products/$filename")

        // Thử upload lên Firebase Storage
        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onProgress(false)
                        onResult(downloadUri.toString())
                    }
                    .addOnFailureListener {
                        // Fallback sang Base64
                        fallbackToBase64(context, uri, onProgress, onResult)
                    }
            }
            .addOnFailureListener {
                // Khi Firebase Storage chưa cấu hình Rules/chưa kích hoạt bucket, tự động fallback sang Base64
                fallbackToBase64(context, uri, onProgress, onResult)
            }
    }

    private fun fallbackToBase64(
        context: Context,
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onResult: (String?) -> Unit
    ) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                // Resize nhẹ để tối ưu dung lượng
                val maxDim = 400
                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val targetW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                val targetH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim

                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetW.coerceAtLeast(10), targetH.coerceAtLeast(10), true)
                val baos = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                val bytes = baos.toByteArray()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                onProgress(false)
                onResult("data:image/jpeg;base64,$base64")
            } else {
                onProgress(false)
                onResult(uri.toString())
            }
        } catch (e: Exception) {
            onProgress(false)
            onResult(uri.toString())
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
