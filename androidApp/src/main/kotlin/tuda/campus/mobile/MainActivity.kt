package tuda.campus.mobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.ui.platform.LocalContext
import tuda.campus.mobile.managers.MensaManager
import tuda.campus.mobile.managers.mensaManager.FoodIcons
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class, DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        VaultManager.initialize(applicationContext)
        DataStoreManager.initialize(applicationContext)

        // Initialize the three Manager objects by "accessing" them
        //TucanManager
        //MensaManager
        //IdentityManager

        GlobalScope.launch {
            launch { MensaManager.initializeStates() }
            launch { FoodIcons.load() }
        }

        setContent {
            val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val isDarkTheme = isSystemInDarkTheme()

            /*
            val colors = if (dynamicColor) {
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) darkColorScheme()
                else lightColorScheme()
            }
             */

            /*
            val colors = if (supportsDynamicColor) {
                if (isDarkTheme) darkColorScheme()
                else expressiveLightColorScheme()
            } else {
                if (isDarkTheme) darkColorScheme()
                else lightColorScheme()
            }

            MaterialExpressiveTheme(
                colorScheme = colorScheme
            ) {
                App()
            }
             */


            // I think this is googles recommendation for expressive (?)
            val colorScheme =
                when {
                    supportsDynamicColor && isDarkTheme ->
                    {
                        dynamicDarkColorScheme(LocalContext.current)
                    }
                    supportsDynamicColor && !isDarkTheme ->
                        dynamicLightColorScheme(LocalContext.current)
                    isDarkTheme -> darkColorScheme()
                    else -> expressiveLightColorScheme()
                }

            MaterialExpressiveTheme(
                colorScheme = colorScheme
            ) {
                App()
            }
        }
    }
}