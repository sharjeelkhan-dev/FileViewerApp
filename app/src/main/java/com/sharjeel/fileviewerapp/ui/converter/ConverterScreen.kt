package com.sharjeel.fileviewerapp.ui.converter

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.util.FileUtils
import java.io.File

data class ConverterColors(
    val appBackground: Color,
    val surfaceCardBg: Color,
    val innerCardBg: Color,
    val selectedCardBg: Color,
    val outlineBorderColor: Color,
    val selectedBorderColor: Color,
    val primaryGradientStart: Color,
    val primaryGradientEnd: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val anyDocIconColor: Color,
    val chipContainer: Color,
    val chipSelectedContainer: Color
)

@Composable
fun getConverterColors(isDark: Boolean = isSystemInDarkTheme()): ConverterColors {
    return ConverterColors(
        appBackground = if (isDark) Color(0xFF0F151F) else Color(0xFFF8FAFC),
        surfaceCardBg = if (isDark) Color(0xFF1B2431) else Color(0xFFFFFFFF),
        innerCardBg = if (isDark) Color(0xFF18222E) else Color(0xFFF1F5F9),
        selectedCardBg = if (isDark) Color(0xFF1D2C3F) else Color(0xFFE0F2FE),
        outlineBorderColor = if (isDark) Color(0xFF283648) else Color(0xFFE2E8F0),
        selectedBorderColor = if (isDark) Color(0xFF2E63A8) else Color(0xFF3B82F6),
        primaryGradientStart = Color(0xFF2898E8),
        primaryGradientEnd = Color(0xFF1E63C3),
        textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A),
        textSecondary = if (isDark) Color(0xFF8B9CB0) else Color(0xFF64748B),
        anyDocIconColor = Color(0xFF3B82F6),
        chipContainer = if (isDark) Color(0xFF16202C) else Color(0xFFF1F5F9),
        chipSelectedContainer = if (isDark) Color(0xFF1F3147) else Color(0xFFDBEAFE)
    )
}

data class FormatOption(
    val format: DocumentFormat,
    val title: String,
    val extension: String,
    val getDescription: (ConversionTarget) -> String,
    val iconRes: Int,
    val mimeTypes: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FormatOption

        if (iconRes != other.iconRes) return false
        if (format != other.format) return false
        if (title != other.title) return false
        if (extension != other.extension) return false
        if (getDescription != other.getDescription) return false
        if (!mimeTypes.contentEquals(other.mimeTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iconRes
        result = 31 * result + format.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + extension.hashCode()
        result = 31 * result + getDescription.hashCode()
        result = 31 * result + mimeTypes.contentHashCode()
        return result
    }
}

private val formatOptionsList = listOf(
    FormatOption(
        format = DocumentFormat.WORD,
        title = "Word",
        extension = "(DOCX)",
        getDescription = { target ->
            when (target) {
                ConversionTarget.PDF -> "Convert Word\nto PDF"
                ConversionTarget.IMAGE -> "Convert Word\nto Image"
                ConversionTarget.TEXT -> "Convert Word\nto Text"
            }
        },
        iconRes = R.drawable.microsoft_word_icon,
        mimeTypes = arrayOf(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            "application/vnd.ms-word.document.macroEnabled.12",
            "application/vnd.ms-word",
            "application/docx",
            "application/x-docx",
            "application/x-msword",
            "application/wps-office.docx",
            "application/wps-office.doc"
        )
    ),
    FormatOption(
        format = DocumentFormat.POWERPOINT,
        title = "PowerPoint",
        extension = "(PPTX)",
        getDescription = { target ->
            when (target) {
                ConversionTarget.PDF -> "Convert PPT\nto PDF"
                ConversionTarget.IMAGE -> "Convert PPT\nto Image"
                ConversionTarget.TEXT -> "Convert PPT\nto Text"
            }
        },
        iconRes = R.drawable.microsoft_powerpoint_icon,
        mimeTypes = arrayOf(
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.presentationml.template",
            "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
            "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
            "application/pptx",
            "application/x-pptx",
            "application/wps-office.pptx",
            "application/wps-office.ppt"
        )
    ),
    FormatOption(
        format = DocumentFormat.ANY_DOC,
        title = "Any Doc",
        extension = "(Multi)",
        getDescription = { target ->
            when (target) {
                ConversionTarget.PDF -> "Convert any\nfile to PDF"
                ConversionTarget.IMAGE -> "Convert any\nfile to Image"
                ConversionTarget.TEXT -> "Convert any\nfile to Text"
            }
        },
        iconRes = R.drawable.page_black_icon,
        mimeTypes = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            "application/vnd.ms-word",
            "application/docx",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/pptx",
            "text/plain"
        )
    )
)

@Composable
fun ConverterScreen(
    onBackClick: () -> Unit,
    onOpenFileClick: (filePath: String, extension: String) -> Unit = { _, _ -> },
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addSelectedFileUri(it) }
    }

    ConverterContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onTargetSelect = { viewModel.setTarget(it) },
        onFormatSelect = { format, mimeTypes ->
            viewModel.setFormat(format)
            filePickerLauncher.launch(mimeTypes)
        },
        onPickFileClick = {
            val selectedOption = formatOptionsList.find { it.format == uiState.selectedFormat }
            val types = selectedOption?.mimeTypes ?: arrayOf("*/*")
            filePickerLauncher.launch(types)
        },
        onRemoveFile = { viewModel.removeFile(it) },
        onConvertClick = { viewModel.convertFiles() }
    )
}

@Composable
fun ConverterContent(
    uiState: ConverterUiState,
    onBackClick: () -> Unit,
    onTargetSelect: (ConversionTarget) -> Unit,
    onFormatSelect: (DocumentFormat, Array<String>) -> Unit,
    onPickFileClick: () -> Unit,
    onRemoveFile: (ConverterFileItem) -> Unit,
    onConvertClick: () -> Unit
) {
    val colors = getConverterColors()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
    ) {
        // FIXED HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = topInset + 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Docs Converter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                color = colors.textPrimary
            )
        }

        // SCROLLABLE CONTENT
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomInset + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCardBg),
                    border = BorderStroke(1.dp, colors.outlineBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.doc_converter),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Tools",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Select source & format",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ConversionTarget.entries.forEach { target ->
                                val isSelected = uiState.target == target
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onTargetSelect(target) },
                                    label = {
                                        Text(
                                            text = when (target) {
                                                ConversionTarget.PDF -> "to PDF"
                                                ConversionTarget.IMAGE -> "to Image"
                                                ConversionTarget.TEXT -> "to Text"
                                            },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.chipSelectedContainer,
                                        selectedLabelColor = colors.textPrimary,
                                        containerColor = colors.chipContainer,
                                        labelColor = colors.textSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = colors.outlineBorderColor,
                                        selectedBorderColor = colors.selectedBorderColor,
                                        borderWidth = 1.dp
                                    )
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            formatOptionsList.forEach { option ->
                                ScreenshotFormatCard(
                                    option = option,
                                    currentTarget = uiState.target,
                                    colors = colors,
                                    isSelected = uiState.selectedFormat == option.format,
                                    onClick = {
                                        onFormatSelect(option.format, option.mimeTypes)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onPickFileClick() }
                                .border(
                                    width = 1.5.dp,
                                    color = colors.outlineBorderColor,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .background(
                                    color = colors.innerCardBg.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FileUpload,
                                    contentDescription = null,
                                    tint = colors.primaryGradientStart,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Tap here to select files for conversion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (uiState.errorMessage != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = uiState.selectedFiles.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HorizontalDivider(
                                    color = colors.outlineBorderColor.copy(alpha = 0.6f),
                                    thickness = 1.dp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Manage Files",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(
                                        items = uiState.selectedFiles,
                                        key = { it.path }
                                    ) { fileItem ->
                                        SelectedFileQueueBadge(
                                            fileItem = fileItem,
                                            colors = colors,
                                            isConverting = uiState.isConverting,
                                            onRemove = { onRemoveFile(fileItem) }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.statusMessage != null && uiState.conversionComplete) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Document Saved to System!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                        Text(
                                            text = "${uiState.statusMessage}. You can view or delete it from Categories anytime.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onConvertClick,
                    enabled = !uiState.isConverting && uiState.selectedFiles.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(colors.primaryGradientStart, colors.primaryGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isConverting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.conversionComplete) "Converted & Saved to System!" else "Convert Selected Files (${uiState.selectedFiles.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenshotFormatCard(
    option: FormatOption,
    currentTarget: ConversionTarget,
    colors: ConverterColors,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg by animateColorAsState(
        targetValue = if (isSelected) colors.selectedCardBg else colors.innerCardBg,
        label = "cardBgAnimation"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.selectedBorderColor else colors.outlineBorderColor,
        label = "borderColorAnimation"
    )

    val iconTint = if (option.format == DocumentFormat.ANY_DOC) {
        colors.primaryGradientStart
    } else {
        Color.Unspecified
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = option.iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = option.getDescription(currentTarget),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add_plus_icon),
                    contentDescription = "Custom Icon",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedFileQueueBadge(
    fileItem: ConverterFileItem,
    colors: ConverterColors,
    isConverting: Boolean,
    onRemove: () -> Unit
) {
    val documentAccentColor = when (fileItem.format) {
        DocumentFormat.WORD -> Color(0xFF2B579A)
        DocumentFormat.POWERPOINT -> Color(0xFFD24726)
        else -> colors.primaryGradientStart
    }

    val iconRes = when (fileItem.format) {
        DocumentFormat.WORD -> R.drawable.microsoft_word_icon
        DocumentFormat.POWERPOINT -> R.drawable.microsoft_powerpoint_icon
        else -> R.drawable.page_black_icon
    }

    val iconTint = if (fileItem.format == DocumentFormat.ANY_DOC) {
        documentAccentColor
    } else {
        Color.Unspecified
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(88.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 72.dp, height = 76.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.innerCardBg,
                border = BorderStroke(1.dp, colors.outlineBorderColor),
                modifier = Modifier.size(width = 60.dp, height = 66.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
            ) {
                when (fileItem.state) {
                    FileConversionState.SELECTED -> {
                        Surface(
                            shape = CircleShape,
                            color = documentAccentColor,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    FileConversionState.CONVERTING -> {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    color = documentAccentColor,
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }
                    FileConversionState.DONE -> {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF22C55E),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Done",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    FileConversionState.FAILED -> {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Failed",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!isConverting && fileItem.state == FileConversionState.SELECTED) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF64748B)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = when (fileItem.state) {
                FileConversionState.SELECTED -> "1 file selected"
                FileConversionState.CONVERTING -> "Converting..."
                FileConversionState.DONE -> "Done"
                FileConversionState.FAILED -> "Failed"
            },
            color = when (fileItem.state) {
                FileConversionState.CONVERTING -> documentAccentColor
                FileConversionState.DONE -> Color(0xFF22C55E)
                FileConversionState.FAILED -> Color(0xFFEF4444)
                else -> colors.textSecondary
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}