package com.sharjeel.fileviewerapp.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
            .statusBarsPadding()
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
                tint = MaterialTheme.colorScheme.primary, // Fixed: Neon drop karke core dynamic brand accent use kiya
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun StorageDashboardCard(onClick: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.08f),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Folder Icon
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFB300).copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.folder_icon),
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // NEW: "Open" Text and Arrow Indicator
                    Row(
                        modifier = Modifier.offset(y = 25.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Open Storage",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // ... Remaining parts of the code stay the same ...
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Internal Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.2.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                CircleShape
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "128 GB",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
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
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.secondary, // Fixed: Sync with professional teal accent token
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun PlacesGrid(onPlaceClick: (String) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Balanced M3 compliant operational colors (No extreme wild colors)
    val places = listOf(
        PlaceItem("Downloads", painterResource(R.drawable.import_icon), primaryColor),
        PlaceItem("Recent", painterResource(R.drawable.rotate_left_arrow_icon), secondaryColor),
        PlaceItem("Favorites", painterResource(R.drawable.heart_black_icon), Color(0xFFE11D48)),
        PlaceItem("Vault", painterResource(R.drawable.shield_lock_line_icon), Color(0xFF059669)),
        PlaceItem("Trash", painterResource(R.drawable.recycle_bin_line_icon), Color(0xFFDC2626))
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
fun PlaceCard(place: PlaceItem, modifier: Modifier, onPlaceClick: (String) -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            onClick = { onPlaceClick(place.name) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    place.icon,
                    contentDescription = null,
                    tint = place.color,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            place.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in categories.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryCard(categories[i], Modifier.weight(1f), onCategoryClick)
                if (i + 1 < categories.size) {
                    CategoryCard(categories[i+1], Modifier.weight(1f), onCategoryClick)
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
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = { onClick(item.name) },
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium,
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