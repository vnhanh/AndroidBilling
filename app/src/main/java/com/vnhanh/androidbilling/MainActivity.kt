package com.vnhanh.androidbilling

import android.os.Bundle
import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.Purchase
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.vnhanh.androidbilling.billing.AppBillingClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : FragmentActivity(), AppBillingClient.PurchasesUpdatedListener {
    private val billingClient: AppBillingClient by lazy {
        AppBillingClient.getInstance()
    }
    
    private val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        billingClient.initialize(context = this.applicationContext)
        
        lifecycleScope.launch {
            if (isFirebaseConnected()) {
                Log.i("Firebase", "Firebase is connected and working!")
                // Proceed with Firebase operations
            } else {
                Log.w("Firebase", "Firebase connection check failed.  Firebase may not be available.")
                // Handle the case where Firebase is not connected (e.g., disable features)
            }
        }
        
        setContent {
            MainScreen()
        }
    }

    private suspend fun isFirebaseConnected(): Boolean {
        return try {
            if (FirebaseApp.getApps(this).isNotEmpty()){
                //Firebase app already initialized
            } else {
                FirebaseApp.initializeApp(this)
            }
            val remoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600  // 1 hour (for testing, you can use a smaller value)
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.fetchAndActivate().await()  // Wait for fetch and activate to complete
            true // If we reached here without exception, Firebase is likely connected
        } catch (e: Exception) {
            Log.e("Firebase", "Firebase connection check failed: ${e.message}")
            false
        }
    }

    override fun onDestroy() {
        billingClient.dispose()
        super.onDestroy()
    }

    override fun onSuccess(purchase: Purchase) {
        Toast.makeText(this, "Purchase successfully!", Toast.LENGTH_LONG).show()
    }

    override fun onFailure(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    @Composable
    private fun MainScreen() {
        Scaffold(
            content = { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    val loadingState = loading.collectAsStateWithLifecycle().value
                    when (loadingState) {
                        true -> {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    strokeCap = StrokeCap.Round,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Loading...")
                            }
                        }

                        false -> {
                            Text(
                                text = "Buy",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color = Color.White)
                                    .border(width = 1.dp, color = colorResource(R.color.teal_700), shape = RoundedCornerShape(16.dp))
                                    .clickable {
                                        lifecycleScope.launch {
                                            billingClient.purchase(
//                                                sku = "com.vnhanh.billing.yearly",
                                                sku = "com.alanvo.test.googlebilling.sub1",
                                                activity = this@MainActivity,
                                                callback = this@MainActivity,
                                            )
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorResource(R.color.teal_700),
                                )
                            )       
                        }
                    }
                }
            }
        )
    }
}
