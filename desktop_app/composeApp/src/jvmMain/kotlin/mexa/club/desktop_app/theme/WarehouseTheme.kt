package mexa.club.desktop_app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object WarehousePalette {
    val Background = Color(0xFFF5F5F5)
    val OnBackground = Color(0xFF1B1B23)
    val Primary = Color(0xFF4648D4)
    val OnPrimary = Color.White
    val PrimaryContainer = Color(0xFF6063EE)
    val OnPrimaryContainer = Color(0xFFFFFBFF)
    val PrimaryFixed = Color(0xFFE1E0FF)
    val OnPrimaryFixed = Color(0xFF07006C)
    val Secondary = Color(0xFF565E74)
    val OnSecondary = Color.White
    val SecondaryContainer = Color(0xFFDAE2FD)
    val OnSecondaryContainer = Color(0xFF5C647A)
    val Tertiary = Color(0xFF904900)
    val OnTertiary = Color.White
    val TertiaryFixed = Color(0xFFFFDCC5)
    val OnTertiaryFixed = Color(0xFF301400)
    val TertiaryFixedDim = Color(0xFFFFB783)
    val Error = Color(0xFFBA1A1A)
    val OnError = Color.White
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)
    val Surface = Color(0xFFF5F5F5)
    val OnSurface = Color(0xFF1B1B23)
    val SurfaceVariant = Color(0xFFE4E1ED)
    val OnSurfaceVariant = Color(0xFF464554)
    val SurfaceContainerLow = Color(0xFFF5F2FE)
    val SurfaceContainer = Color(0xFFEFECF8)
    val Outline = Color(0xFF767586)
    val OutlineVariant = Color(0xFFC7C4D7)
    val CardSurface = Color.White

    val SidebarBg = Color(0xFF0F172A)
    val SidebarBorder = Color(0xFF1E293B)
    val SidebarMuted = Color(0xFF94A3B8)
    val SidebarActive = Color(0xFF6366F1)
    val TopBarBorder = Color(0xFFE2E8F0)
    val TopBarSurface = Color.White
    val Slate900 = Color(0xFF0F172A)
    val Green600 = Color(0xFF16A34A)
    val Green50 = Color(0xFFF0FDF4)
    val Amber500 = Color(0xFFF59E0B)
}

private val WarehouseLightColorScheme = lightColorScheme(
    primary = WarehousePalette.Primary,
    onPrimary = WarehousePalette.OnPrimary,
    primaryContainer = WarehousePalette.PrimaryContainer,
    onPrimaryContainer = WarehousePalette.OnPrimaryContainer,
    secondary = WarehousePalette.Secondary,
    onSecondary = WarehousePalette.OnSecondary,
    secondaryContainer = WarehousePalette.SecondaryContainer,
    onSecondaryContainer = WarehousePalette.OnSecondaryContainer,
    tertiary = WarehousePalette.Tertiary,
    onTertiary = WarehousePalette.OnTertiary,
    error = WarehousePalette.Error,
    onError = WarehousePalette.OnError,
    errorContainer = WarehousePalette.ErrorContainer,
    onErrorContainer = WarehousePalette.OnErrorContainer,
    background = WarehousePalette.Background,
    onBackground = WarehousePalette.OnBackground,
    surface = WarehousePalette.Surface,
    onSurface = WarehousePalette.OnSurface,
    surfaceVariant = WarehousePalette.SurfaceVariant,
    onSurfaceVariant = WarehousePalette.OnSurfaceVariant,
    outline = WarehousePalette.Outline,
    outlineVariant = WarehousePalette.OutlineVariant,
)

private val WarehouseTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.02).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun WarehouseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarehouseLightColorScheme,
        typography = WarehouseTypography,
        content = content,
    )
}
