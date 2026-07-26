package com.sharjeel.fileviewerapp.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onAIClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onPlaceClick: (String) -> Unit = {},
    onStorageClick: () -> Unit = {}
) {
    AppScaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            HomeHeader(
                onMenuClick = onMenuClick,
                onAIClick = onAIClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            StorageDashboardCard(onClick = onStorageClick)

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeader(title = "QUICK ACCESS")
            PlacesGrid(onPlaceClick = onPlaceClick)

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeader(title = "CATEGORIES")
            CategoriesGrid(onCategoryClick = onCategoryClick)

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun HomeHeader(
    onMenuClick: () -> Unit,
    onAIClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(26.dp)
            )
        }

        Text(
            text = "FILE VIEWER",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Surface(
            onClick = onAIClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "AI Assistant",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun StorageDashboardCard(onClick: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFB300).copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.folder_icon),
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Open Storage",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Internal Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "63% used • 47 GB free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { 0.63f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = primaryColor,
                    trackColor = surfaceVariant.copy(alpha = 0.6f),
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "81 GB Used",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "128 GB",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
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
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun PlacesGrid(onPlaceClick: (String) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val places = listOf(
        PlaceItem("Downloads", painterResource(R.drawable.import_icon), primaryColor),
        PlaceItem("Recent", painterResource(R.drawable.rotate_left_arrow_icon), secondaryColor),
        PlaceItem("Favorites", painterResource(R.drawable.heart_black_icon), Color(0xFFE11D48)),
        PlaceItem("Vault", painterResource(R.drawable.shield_lock_line_icon), Color(0xFF059669)),
        PlaceItem("Trash", painterResource(R.drawable.delete_icon), Color(0xFFDC2626))
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            places.take(3).forEach { place ->
                PlaceCard(
                    place = place,
                    modifier = Modifier.weight(1f),
                    onPlaceClick = onPlaceClick
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            places.drop(3).forEach { place ->
                PlaceCard(
                    place = place,
                    modifier = Modifier.weight(1f),
                    onPlaceClick = onPlaceClick
                )
            }
            Spacer(modifier = Modifier.weight((3 - places.size % 3).toFloat()))
        }
    }
}

@Composable
fun PlaceCard(
    place: PlaceItem,
    modifier: Modifier = Modifier,
    onPlaceClick: (String) -> Unit
) {
    Column(
        modifier = modifier.clickable { onPlaceClick(place.name) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = RoundedCornerShape(18.dp),
            color = place.color.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, place.color.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = place.icon,
                    contentDescription = place.name,
                    tint = place.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = place.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun CategoriesGrid(onCategoryClick: (String) -> Unit) {
    // Dynamic counts hardcoded here for layout preview;
    // real app mein aap ViewModel se query karke pass karenge.
    val categories = listOf(
        CategoryItem("Images", painterResource(R.drawable.photo_collage_icon), AccentImages, "1,240 items"),
        CategoryItem("Videos", painterResource(R.drawable.video_playlist_icon), AccentVideos, "312 items"),
        CategoryItem("Audio", painterResource(R.drawable.audio_tune_icon), AccentAudio, "185 items"),
        CategoryItem("Docs", painterResource(R.drawable.text_document_line_icon), AccentDocuments, "94 items"),
        CategoryItem("Archives", painterResource(R.drawable.archive_line_icon), AccentArchives, "42 items")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in categories.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(
                    item = categories[i],
                    modifier = Modifier.weight(1f),
                    onClick = onCategoryClick
                )
                if (i + 1 < categories.size) {
                    CategoryCard(
                        item = categories[i + 1],
                        modifier = Modifier.weight(1f),
                        onClick = onCategoryClick
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    item: CategoryItem,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        onClick = { onClick(item.name) },
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = item.color.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = item.icon,
                        contentDescription = item.name,
                        tint = item.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.count,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class PlaceItem(val name: String, val icon: Painter, val color: Color)
data class CategoryItem(
    val name: String,
    val icon: Painter,
    val color: Color,
    val count: String = "0 items"
)

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