package com.securetransfer.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.securetransfer.android.SecureTransferApplication
import com.securetransfer.android.ui.screen.MainScreen
import com.securetransfer.android.ui.theme.SecureTransferTheme
import com.securetransfer.android.viewmodel.MainViewModel
import com.securetransfer.android.viewmodel.MainViewModelFactory
import org.slf4j.LoggerFactory

class MainActivity : ComponentActivity() {
    
    private val logger = LoggerFactory.getLogger(MainActivity::class.java)
    
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            (application as SecureTransferApplication).deviceRepository,
            (application as SecureTransferApplication).transferRepository
        )
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            logger.info("All permissions granted")
            mainViewModel.onPermissionsGranted()
        } else {
            logger.warn("Some permissions were denied: ${permissions.filter { !it.value }}")
            mainViewModel.onPermissionsDenied()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.info("MainActivity created")
        
        setContent {
            SecureTransferTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = mainViewModel,
                        onRequestPermissions = { requestPermissions() }
                    )
                }
            }
        }
        
        checkAndRequestPermissions()
    }
    
    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>().apply {
            add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            logger.info("Requesting permissions: $missingPermissions")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            logger.info("All permissions already granted")
            mainViewModel.onPermissionsGranted()
        }
    }
    
    private fun requestPermissions() {
        checkAndRequestPermissions()
    }
}
