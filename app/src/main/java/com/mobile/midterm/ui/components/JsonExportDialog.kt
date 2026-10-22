package com.mobile.midterm.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JsonExportDialog(
    jsonString: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Dữ liệu CSDL (JSON)", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                Text(
                    text = "Dữ liệu xuất từ Cloud Firestore theo các trường quy định:",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .background(Color(0xFF263238), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()
                    Text(
                        text = jsonString,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF80CBC4),
                        modifier = Modifier
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("ECOMER_DATABASE_JSON", jsonString)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Đã sao chép JSON vào bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sao chép")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
