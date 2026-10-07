package com.example.itemmanager.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.itemmanager.R
import com.example.itemmanager.navigation.CategoryPresetRes
import com.example.itemmanager.util.ImageUtils
import java.io.File

/**
 * 物品添加/编辑页
 * - 同一物品可添加多张照片（相机/相册），可单独删除
 * - 日期支持直接输入（年-月-日）或点日历图标选择
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    viewModel: ItemEditViewModel,
    onBack: () -> Unit,
    onSaveComplete: () -> Unit
) {
    val name by viewModel.name.collectAsState()
    val category by viewModel.category.collectAsState()
    val brand by viewModel.brand.collectAsState()
    val description by viewModel.description.collectAsState()
    val isPrivate by viewModel.isPrivate.collectAsState()
    val location by viewModel.location.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val imagePaths by viewModel.imagePaths.collectAsState()
    val purchaseDate by viewModel.purchaseDate.collectAsState()
    val purchaseDateText by viewModel.purchaseDateText.collectAsState()
    val expiryDate by viewModel.expiryDate.collectAsState()
    val expiryDateText by viewModel.expiryDateText.collectAsState()
    val warrantyDate by viewModel.warrantyDate.collectAsState()
    val warrantyDateText by viewModel.warrantyDateText.collectAsState()
    val borrower by viewModel.borrower.collectAsState()
    val expectedReturnDate by viewModel.expectedReturnDate.collectAsState()
    val expectedReturnDateText by viewModel.expectedReturnDateText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val saveCompleted by viewModel.saveCompleted.collectAsState()
    val pendingDraft by viewModel.pendingDraft.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf<Int?>(null) }
    var cameraTempFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(saveCompleted) { if (saveCompleted) onSaveComplete() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.addImageFromUri(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempFile != null) {
            viewModel.addPhotoPath(cameraTempFile?.absolutePath)
        }
        cameraTempFile = null
    }

    fun launchCamera() {
        val photoFile = ImageUtils.createCameraImageFile(context)
        if (photoFile != null) {
            val photoUri = ImageUtils.getUriForFile(context, photoFile)
            if (photoUri != null) {
                cameraTempFile = photoFile
                cameraLauncher.launch(photoUri)
            }
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditing) stringResource(R.string.edit_item)
                        else stringResource(R.string.add_item),
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // ---- 多图横向列表 ----
            Text(
                stringResource(R.string.item_photos, imagePaths.size),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imagePaths.forEach { path ->
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .clickable { viewModel.removeImage(path) },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f)
                        ) {
                            Icon(
                                Icons.Default.Close, null, tint = Color.White,
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showImageSourceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.add_photo), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- 名称 ----
            OutlinedTextField(
                value = name,
                onValueChange = viewModel::onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.item_name)) },
                placeholder = { Text(stringResource(R.string.item_name_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            // ---- 分类 ----
            OutlinedTextField(
                value = category,
                onValueChange = viewModel::onCategoryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.category)) },
                placeholder = { Text(stringResource(R.string.category_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.quick_category), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryPresetRes.forEach { catRes ->
                    val cat = stringResource(catRes)
                    val selected = category == cat
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { viewModel.onCategoryChanged(cat) },
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else Color(0xFFF1F5F9),
                        shape = CircleShape
                    ) {
                        Text(
                            cat,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else Color.Gray,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ---- 品牌 ----
            OutlinedTextField(
                value = brand,
                onValueChange = viewModel::onBrandChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.brand)) },
                placeholder = { Text(stringResource(R.string.brand_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            // ---- 数量 ----
            OutlinedTextField(
                value = quantity,
                onValueChange = viewModel::onQuantityChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.quantity)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            // ---- 存放位置 ----
            OutlinedTextField(
                value = location,
                onValueChange = viewModel::onLocationChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.location)) },
                placeholder = { Text(stringResource(R.string.location_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))

            // ---- 借出信息 ----
            Text(
                stringResource(R.string.borrow_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = borrower,
                onValueChange = viewModel::onBorrowerChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.borrower)) },
                placeholder = { Text(stringResource(R.string.borrower_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            DateField(
                label = stringResource(R.string.expected_return),
                text = expectedReturnDateText,
                onTextChange = viewModel::onExpectedReturnDateTextChange,
                onPick = { datePickerTarget = 3 },
                onClear = { viewModel.setExpectedReturnDate(null) }
            )

            Spacer(Modifier.height(16.dp))

            // ---- 有效期信息 ----
            Text(
                stringResource(R.string.validity_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            DateField(
                label = stringResource(R.string.purchase_date),
                text = purchaseDateText,
                onTextChange = viewModel::onPurchaseDateTextChange,
                onPick = { datePickerTarget = 0 },
                onClear = { viewModel.setPurchaseDate(null) }
            )
            Spacer(Modifier.height(8.dp))
            DateField(
                label = stringResource(R.string.expiry_date),
                text = expiryDateText,
                onTextChange = viewModel::onExpiryDateTextChange,
                onPick = { datePickerTarget = 1 },
                onClear = { viewModel.setExpiryDate(null) }
            )
            Spacer(Modifier.height(8.dp))
            DateField(
                label = stringResource(R.string.warranty_date),
                text = warrantyDateText,
                onTextChange = viewModel::onWarrantyDateTextChange,
                onPick = { datePickerTarget = 2 },
                onClear = { viewModel.setWarrantyDate(null) }
            )

            // ---- 隐私设置：移入加密箱 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.move_to_vault),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isPrivate,
                    onCheckedChange = viewModel::setPrivate
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---- 描述 ----
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::onDescriptionChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                label = { Text(stringResource(R.string.description)) },
                placeholder = { Text(stringResource(R.string.description_hint)) },
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveItem() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (viewModel.isEditing) stringResource(R.string.save_changes)
                    else stringResource(R.string.add_item),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ---- 图片来源选择 ----
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text(stringResource(R.string.choose_method)) },
            text = {
                Column {
                    TextButton(onClick = {
                        showImageSourceDialog = false; launchCamera()
                    }) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.take_photo))
                    }
                    TextButton(onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.choose_gallery))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ---- 日历选择器 ----
    val target = datePickerTarget
    if (target != null) {
        val initial = when (target) {
            0 -> purchaseDate
            1 -> expiryDate
            2 -> warrantyDate
            else -> expectedReturnDate
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    when (target) {
                        0 -> viewModel.setPurchaseDate(millis)
                        1 -> viewModel.setExpiryDate(millis)
                        2 -> viewModel.setWarrantyDate(millis)
                        else -> viewModel.setExpectedReturnDate(millis)
                    }
                    datePickerTarget = null
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ---- 草稿恢复对话框 ----
    if (pendingDraft != null) {
        AlertDialog(
            onDismissRequest = { viewModel.discardDraft() },
            title = { Text(stringResource(R.string.restore_title)) },
            text = { Text(stringResource(R.string.restore_text)) },
            confirmButton = { TextButton(onClick = { viewModel.restoreDraft() }) { Text(stringResource(R.string.restore)) } },
            dismissButton = { TextButton(onClick = { viewModel.discardDraft() }) { Text(stringResource(R.string.discard)) } }
        )
    }
}

/**
 * 日期字段：可直接输入（年-月-日），也可点日历图标选择，可清除
 */
@Composable
private fun DateField(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(stringResource(R.string.date_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(28.dp)
                        .clickable(onClick = onPick)
                )
                if (text.isNotBlank()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                            .clickable(onClick = onClear)
                    )
                }
            }
        }
    )
}
