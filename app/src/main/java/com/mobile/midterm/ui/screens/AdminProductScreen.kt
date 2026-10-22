package com.mobile.midterm.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobile.midterm.data.model.Product
import com.mobile.midterm.data.model.UserProfile
import com.mobile.midterm.data.repository.ProductRepository
import com.mobile.midterm.ui.components.JsonExportDialog
import com.mobile.midterm.ui.theme.CardBorderColor
import com.mobile.midterm.ui.theme.DeleteRed
import com.mobile.midterm.ui.theme.EditYellow
import com.mobile.midterm.ui.theme.LightBlueBorder
import com.mobile.midterm.ui.theme.PrimaryBlue

@Composable
fun AdminProductScreen(
    userProfile: UserProfile,
    productRepo: ProductRepository,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val products by productRepo.getProductsFlow().collectAsState(initial = emptyList())

    // Form states
    var tenSanPham by remember { mutableStateOf("") }
    var loaiSanPham by remember { mutableStateOf("") }
    var gia by remember { mutableStateOf("") }
    var imageUriOrUrl by remember { mutableStateOf("") }
    var imageFileName by remember { mutableStateOf("") }

    // State chỉnh sửa
    var editingProductId by remember { mutableStateOf<String?>(null) }

    // Loading & dialogs
    var isUploadingImage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showJsonDialog by remember { mutableStateOf(false) }

    // Launcher chọn ảnh từ thiết bị
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var displayName = "image_${System.currentTimeMillis()}.jpg"
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = it.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}

            imageFileName = displayName

            // Tải ảnh lên API miễn phí không cần đăng ký
            productRepo.uploadOrProcessImage(
                context = context,
                uri = uri,
                onProgress = { isUploadingImage = it },
                onResult = { resultString ->
                    if (!resultString.isNullOrBlank()) {
                        imageUriOrUrl = resultString
                        Toast.makeText(context, "Tải ảnh lên thành công!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Không thể tải ảnh, vui lòng thử lại!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    fun resetForm() {
        tenSanPham = ""
        loaiSanPham = ""
        gia = ""
        imageUriOrUrl = ""
        imageFileName = ""
        editingProductId = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top action bar cho Admin
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Quản trị viên: ${userProfile.email}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Phân hệ: Admin (Quản lý sản phẩm)",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }

            Row {
                IconButton(
                    onClick = { showJsonDialog = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Xuất JSON",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Đăng xuất",
                        tint = DeleteRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Nội dung chính
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Tiêu đề: "Dữ liệu sản phẩm" theo đúng mẫu đề bài
                Text(
                    text = "Dữ liệu sản phẩm",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Trường 1: Tên sản phẩm
                OutlinedTextField(
                    value = tenSanPham,
                    onValueChange = { tenSanPham = it },
                    placeholder = { Text("Ap phong nam", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightBlueBorder,
                        unfocusedBorderColor = LightBlueBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Trường 2: Loại sản phẩm
                OutlinedTextField(
                    value = loaiSanPham,
                    onValueChange = { loaiSanPham = it },
                    placeholder = { Text("Thoi trang nu", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightBlueBorder,
                        unfocusedBorderColor = LightBlueBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Trường 3: Giá
                OutlinedTextField(
                    value = gia,
                    onValueChange = { gia = it.filter { char -> char.isDigit() } },
                    placeholder = { Text("300000", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightBlueBorder,
                        unfocusedBorderColor = LightBlueBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mục 4: Chọn hình ảnh (Có hiển thị preview ảnh ngay trong form)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .border(1.dp, LightBlueBorder, RoundedCornerShape(6.dp))
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Xem trước ảnh nếu đã chọn
                        if (imageUriOrUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUriOrUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Xem trước ảnh",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEEEEEE))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Chọn tệp ảnh",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đang tải ảnh lên máy chủ...",
                                fontSize = 13.sp,
                                color = PrimaryBlue
                            )
                        } else {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (imageFileName.isNotBlank()) {
                                        imageFileName
                                    } else if (imageUriOrUrl.isNotBlank()) {
                                        "Đã có ảnh sản phẩm"
                                    } else {
                                        "Chọn hình ảnh sản phẩm..."
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (imageUriOrUrl.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                                    color = if (imageUriOrUrl.isNotBlank()) Color.Black else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (imageUriOrUrl.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ảnh sẵn sàng",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }

                            if (imageUriOrUrl.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        imageUriOrUrl = ""
                                        imageFileName = ""
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa ảnh",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nút THÊM SẢN PHẨM / CẬP NHẬT SẢN PHẨM
                if (isSaving) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (tenSanPham.isBlank() || loaiSanPham.isBlank() || gia.isBlank()) {
                                    Toast.makeText(context, "Vui lòng nhập đủ: Tên, Loại, Giá!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val priceLong = gia.toLongOrNull() ?: 0L
                                isSaving = true

                                if (editingProductId == null) {
                                    // THÊM SẢN PHẨM MỚI (chỉ 4 trường theo đề bài)
                                    productRepo.addProduct(
                                        tenSanPham = tenSanPham.trim(),
                                        loaiSanPham = loaiSanPham.trim(),
                                        gia = priceLong,
                                        image = imageUriOrUrl
                                    ) { success, error ->
                                        isSaving = false
                                        if (success) {
                                            Toast.makeText(context, "Thêm sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                                            resetForm()
                                        } else {
                                            Toast.makeText(context, "Lỗi thêm: $error", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    // CẬP NHẬT SẢN PHẨM
                                    productRepo.updateProduct(
                                        id = editingProductId!!,
                                        tenSanPham = tenSanPham.trim(),
                                        loaiSanPham = loaiSanPham.trim(),
                                        gia = priceLong,
                                        image = imageUriOrUrl
                                    ) { success, error ->
                                        isSaving = false
                                        if (success) {
                                            Toast.makeText(context, "Cập nhật sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                                            resetForm()
                                        } else {
                                            Toast.makeText(context, "Lỗi cập nhật: $error", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text(
                                text = if (editingProductId == null) "THÊM SẢN PHẨM" else "CẬP NHẬT SẢN PHẨM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        if (editingProductId != null) {
                            OutlinedButton(
                                onClick = { resetForm() },
                                modifier = Modifier.height(46.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("HỦY", color = Color.DarkGray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tiêu đề mục: "Danh sách sản phẩm:" theo ảnh mẫu
                Text(
                    text = "Danh sách sản phẩm:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (products.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có sản phẩm nào. Hãy thêm sản phẩm ở trên!",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(products, key = { it.id }) { product ->
                    AdminProductItem(
                        product = product,
                        onEdit = {
                            editingProductId = product.id
                            tenSanPham = product.tenSanPham
                            loaiSanPham = product.loaiSanPham
                            gia = product.gia.toString()
                            imageUriOrUrl = product.image
                            imageFileName = if (product.image.isNotBlank()) "Ảnh hiện tại" else ""
                        },
                        onDelete = {
                            productToDelete = product
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Hộp thoại xác nhận xóa
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa sản phẩm '${productToDelete?.tenSanPham}' không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = productToDelete
                        productToDelete = null
                        if (toDelete != null) {
                            productRepo.deleteProduct(toDelete.id) { success, err ->
                                if (success) {
                                    Toast.makeText(context, "Đã xóa sản phẩm!", Toast.LENGTH_SHORT).show()
                                    if (editingProductId == toDelete.id) {
                                        resetForm()
                                    }
                                } else {
                                    Toast.makeText(context, "Lỗi xóa: $err", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("XÓA", color = DeleteRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("HỦY")
                }
            }
        )
    }

    // Hộp thoại xem/sao chép JSON CSDL
    if (showJsonDialog) {
        val jsonString = remember(products) { productRepo.exportToJson(products) }
        JsonExportDialog(
            jsonString = jsonString,
            onDismiss = { showJsonDialog = false }
        )
    }
}

/**
 * Item từng sản phẩm theo đúng giao diện trong hình mẫu:
 * [Ảnh thumbnail] | [Tên sp, Giá sp, Loại sp] | [Nút Sửa vàng, Nút Xóa đỏ]
 */
@Composable
fun AdminProductItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cột trái: Hình ảnh sản phẩm (thumbnail vuông 64x64)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (product.image.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.image)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.tenSanPham,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Cột giữa: Tên sp, Giá sp, Loại sp đúng mẫu text trong ảnh
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Tên sp: ${product.tenSanPham}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Giá sp: ${product.gia}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Loại sp: ${product.loaiSanPham}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Cột phải: 2 nút Sửa (vàng) và Xóa (đỏ) dạng ô vuông bo viền theo ảnh mẫu
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Nút sửa (bút chì vàng)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, EditYellow, RoundedCornerShape(4.dp))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Sửa",
                        tint = EditYellow,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Nút xóa (thùng rác đỏ)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, DeleteRed, RoundedCornerShape(4.dp))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = DeleteRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
