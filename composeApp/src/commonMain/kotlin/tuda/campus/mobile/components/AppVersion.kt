package tuda.campus.mobile.components

import androidx.compose.runtime.Composable

@Composable
expect fun getAppVersion(): String?

@Composable
expect fun getBuildNumber(): String?