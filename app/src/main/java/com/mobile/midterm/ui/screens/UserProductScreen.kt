package com.mobile.midterm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobile.midterm.data.model.Product
import com.mobile.midterm.data.model.UserProfile
import com.mobile.midterm.data.repository.ProductRepository
import com.mobile.midterm.ui.theme.CardBorderColor
import com.mobile.midterm.ui.theme.DeleteRed
import com.mobile.midterm.ui.theme.LightBlueBorder
import com.mobile.midterm.ui.theme.PrimaryBlue

@Composable
fun UserProductScreen(
    userProfile: UserProfile,
    productRepo: ProductRepository,
    onSignOut: () -> Unit
) {
    val products by productRepo.getProductsFlow().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.tenSanPham.contains(searchQuery, ignoreCase = true) ||
            it.loaiSanPham.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8F5E9))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Khách hàng: ${userProfile.email}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Phân quyền: USER (Chỉ xem sản phẩm)",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }

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

        // Notification banner về phân quyền
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF9C4))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFFF57F17),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tài khoản User chỉ có quyền xem danh sách sản phẩm. Quyền Thêm/Sửa/Xóa chỉ có ở phân hệ Admin.",
                    fontSize = 12.sp,
                    color = Color(0xFF5D4037)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Danh sách sản phẩm",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Thanh tìm kiếm
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm theo tên hoặc loại sản phẩm...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightBlueBorder,
                        unfocusedBorderColor = LightBlueBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Không tìm thấy sản phẩm phù hợp" else "Chưa có sản phẩm nào trong hệ thống.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    UserProductItem(product = product)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun UserProductItem(product: Product) {
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
        }
    }
}
