package com.mobile.midterm.data.model

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude

data class Product(
    @get:Exclude
    val id: String = "",
    val tenSanPham: String = "",
    val loaiSanPham: String = "",
    val gia: Long = 0L,
    val image: String = ""
) {
    fun toMap(): Map<String, Any> {
        // Chỉ lưu đúng 4 trường theo yêu cầu: tên sản phẩm, loại sản phẩm, giá, image
        return hashMapOf(
            "tenSanPham" to tenSanPham,
            "loaiSanPham" to loaiSanPham,
            "gia" to gia,
            "image" to image
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): Product {
            val id = doc.id
            val ten = doc.getString("tenSanPham")
                ?: doc.getString("name")
                ?: doc.getString("ten")
                ?: ""
            val loai = doc.getString("loaiSanPham")
                ?: doc.getString("category")
                ?: doc.getString("loai")
                ?: ""
            val giaVal = doc.getLong("gia")
                ?: doc.getLong("price")
                ?: (doc.get("gia") as? Number)?.toLong()
                ?: (doc.get("price") as? Number)?.toLong()
                ?: 0L
            val img = doc.getString("image")
                ?: doc.getString("imageUrl")
                ?: doc.getString("Image")
                ?: ""
            return Product(
                id = id,
                tenSanPham = ten,
                loaiSanPham = loai,
                gia = giaVal,
                image = img
            )
        }
    }
}
