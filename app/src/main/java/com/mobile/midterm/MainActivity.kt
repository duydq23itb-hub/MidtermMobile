package com.mobile.midterm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobile.midterm.data.model.UserProfile
import com.mobile.midterm.data.repository.AuthRepository
import com.mobile.midterm.data.repository.ProductRepository
import com.mobile.midterm.ui.screens.AdminProductScreen
import com.mobile.midterm.ui.screens.AuthScreen
import com.mobile.midterm.ui.theme.MidtermMobileTheme
import com.mobile.midterm.ui.theme.PrimaryBlue

class MainActivity : ComponentActivity() {

    private val authRepo by lazy { AuthRepository() }
    private val productRepo by lazy { ProductRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MidtermMobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainApp(
                        authRepo = authRepo,
                        productRepo = productRepo,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainApp(
    authRepo: AuthRepository,
    productRepo: ProductRepository,
    modifier: Modifier = Modifier
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingAuth by remember { mutableStateOf(true) }

    fun refreshUser() {
        isLoadingAuth = true
        if (authRepo.currentUser == null) {
            userProfile = null
            isLoadingAuth = false
        } else {
            authRepo.fetchUserProfile { profile ->
                userProfile = profile
                isLoadingAuth = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshUser()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoadingAuth) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            val profile = userProfile
            if (profile == null) {
                AuthScreen(
                    authRepo = authRepo,
                    onLoginSuccess = { refreshUser() }
                )
            } else {
                // Toàn bộ ứng dụng là phân hệ Admin
                AdminProductScreen(
                    userProfile = profile,
                    productRepo = productRepo,
                    onSignOut = {
                        authRepo.signOut()
                        refreshUser()
                    }
                )
            }
        }
    }
}