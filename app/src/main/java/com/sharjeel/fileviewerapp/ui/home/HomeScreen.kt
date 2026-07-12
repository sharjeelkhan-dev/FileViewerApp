package com.sharjeel.fileviewerapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.ui.components.AppScaffold
import com.sharjeel.fileviewerapp.ui.theme.AccentArchives
import com.sharjeel.fileviewerapp.ui.theme.AccentAudio
import com.sharjeel.fileviewerapp.ui.theme.AccentDocuments
import com.sharjeel.fileviewerapp.ui.theme.AccentImages
import com.sharjeel.fileviewerapp.ui.theme.AccentVideos
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import com.sharjeel.fileviewerapp.ui.theme.GlassSurface
import com.sharjeel.fileviewerapp.ui.theme.NeonPrimary
import com.sharjeel.fileviewerapp.ui.theme.NeonSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit = {},
    onAIClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onPlaceClick: (String) -> Unit = {},
    onStorageClick: () -> Unit = {}
) {
    AppScaffold { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Scrollable Header (Yeh internally status bar ke niche apne content ko fit rakhega)
            HomeHeader(onMenuClick, onAIClick)

            StorageDashboardCard(onStorageClick)

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader("QUICK ACCESS")
            PlacesGrid(onPlaceClick)

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader("CATEGORIES")
            CategoriesGrid(onCategoryClick)

            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun HomeHeader(onMenuClick: () -> Unit, onAIClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding() // Correct behavior: Yeh sirf icons/text ko status bar se safe zone mein layega
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            "FILE VIEWER",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onAIClick) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "AI Assistant",
                tint = NeonPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun StorageDashboardCard(onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) GlassSurface else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = if (isDark) 0.15f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Folder Icon and decoration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFCA28).copy(alpha = 0.15f) // Light amber background
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.folder_icon),
                                contentDescription = null,
                                tint = Color(0xFFFFCA28), // Standard folder yellow color
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Middle section: Title
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Internal Storage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                }

                // Bottom section: Progress bar and stats
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Modern Linear Progress with Round Caps
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                CircleShape
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f) // Representing 65% used
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(primaryColor, secondaryColor)
                                    ),
                                    CircleShape,
                                )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Used: 81 GB",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "128 GB",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = NeonSecondary,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}
@Composable
fun PlacesGrid(onPlaceClick: (String) -> Unit) {
    val places = listOf(
        PlaceItem("Downloads", painterResource(R.drawable.import_icon), NeonPrimary),
        PlaceItem("Recent", painterResource(R.drawable.rotate_left_arrow_icon), NeonSecondary),
        PlaceItem("Favorites", painterResource(R.drawable.heart_black_icon), Color(0xFFFF4081)),
        PlaceItem("Vault", painterResource(R.drawable.shield_lock_line_icon), Color(0xFF69F0AE)),
        PlaceItem("Trash", painterResource(R.drawable.recycle_bin_line_icon), Color(0xFFEF5350))
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            places.take(3).forEach { place ->
                PlaceCard(place, Modifier.weight(1f), onPlaceClick)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            places.drop(3).forEach { place ->
                PlaceCard(place, Modifier.weight(1f), onPlaceClick)
            }
            Spacer(modifier = Modifier.weight((3 - places.size % 3).toFloat()))
        }
    }
}
@Composable
fun PlaceCard(place: PlaceItem, modifier: Modifier, onPlaceClick: (String) -> Unit)
{
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            onClick = { onPlaceClick(place.name) },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    place.icon,
                    contentDescription = null,
                    tint = place.color,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            place.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
@Composable
fun CategoriesGrid(onCategoryClick: (String) -> Unit) {
    val categories = listOf(
        CategoryItem("Images", painterResource(R.drawable.photo_collage_icon), AccentImages),
        CategoryItem("Videos", painterResource(R.drawable.video_playlist_icon), AccentVideos),
        CategoryItem("Audio", painterResource(R.drawable.audio_tune_icon), AccentAudio),
        CategoryItem("Docs", painterResource(R.drawable.text_document_line_icon), AccentDocuments),
        CategoryItem("Archives", painterResource(R.drawable.archive_line_icon), AccentArchives)
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (i in categories.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CategoryCard(categories[i], Modifier.weight(1f),
                    onCategoryClick)
                if (i + 1 < categories.size) {
                    CategoryCard(categories[i+1], Modifier.weight(1f),
                        onCategoryClick)
                } else {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryCard(item: CategoryItem, modifier: Modifier, onClick: (String) -> Unit) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = { onClick(item.name) },
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon,
                    contentDescription = null, tint = item.color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
data class PlaceItem(val name: String, val icon: Painter, val color: Color)
data class CategoryItem(val name: String, val icon: Painter, val color: Color)

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun HomeScreenPreviewLight() {
    FileViewerAppTheme(darkTheme = false) {
        HomeScreen()
    }
}
@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun HomeScreenPreviewDark() {
    FileViewerAppTheme(darkTheme = true) {
        HomeScreen()
    }
}
