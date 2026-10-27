package tuda.campus.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

val LocalNativeViewFactory = staticCompositionLocalOf<NativeViewFactory> {
    error("No View Factory provided")       // Should never happen, theoretically
}

interface NativeViewFactory {
    fun createGeneralMapView(): UIViewController
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun MainViewController(
    nativeViewFactory: NativeViewFactory
) = ComposeUIViewController {
    val colorScheme = if (isSystemInDarkTheme()) {
        darkColorScheme()
    } else {
        expressiveLightColorScheme()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme
    ) {
        CompositionLocalProvider(LocalNativeViewFactory provides nativeViewFactory) {
            App()
        }
    }
}