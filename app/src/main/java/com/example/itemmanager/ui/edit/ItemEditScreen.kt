package com.example.itemmanager.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.itemmanager.navigation.CategoryPresets
import com.example.itemmanager.util.DateUtils
import com.example.itemmanager.util.ImageUtils
import java.io.File

/**
 * 物品添加/编辑页
 * - 同一物品可添加多张照片（相机/相册），可单独删除
 * - 可设置购买日期、保质期、保修期
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
    val description by viewModel.description.collectAsState()
    val location by viewModel.location.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val imagePaths by viewModel.imagePaths.collectAsState()
    val purchaseDate by viewModel.purchaseDate.collectAsState()
    val expiryDate by viewModel.expiryDate.collectAsState()
    val warrantyDate by viewModel.warrantyDate.collectAsState()
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
                        if (viewModel.isEditing) "编辑物品" else "添加物品",
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
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
                "物品照片（${imagePaths.size}）",
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
                            contentDescription = "物品图片",
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
                                Icons.Default.Close, "移除", tint = Color.White,
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                    }
                }

                // 添加照片方块
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showImageSourceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, "添加照片", tint = MaterialTheme.colorScheme.primary)
                        Text("添加照片", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- 名称 ----
            OutlinedTextField(
                value = name,
                onValueChange = viewModel::onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("物品名称 *") },
                placeholder = { Text("例如：iPhone 15 Pro") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            // ---- 分类 ----
            OutlinedTextField(
                value = category,
                onValueChange = viewModel::onCategoryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("分类 *") },
                placeholder = { Text("输入或从下方选择分类") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("快捷分类：", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryPresets.forEach { cat ->
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

            Spacer(Modifier.height(12.dp))

            // ---- 数量 ----
            OutlinedTextField(
                value = quantity,
                onValueChange = viewModel::onQuantityChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("数量") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            // ---- 存放位置 ----
            OutlinedTextField(
                value = location,
                onValueChange = viewModel::onLocationChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("存放位置") },
                placeholder = { Text("例如：书房抽屉、客厅柜子") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))

            // ---- 有效期信息 ----
            Text(
                "有效期信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            DateField(
                label = "购买日期",
                value = purchaseDate,
                onClick = { datePickerTarget = 0 },
                onClear = { viewModel.setPurchaseDate(null) }
            )
            Spacer(Modifier.height(8.dp))
            DateField(
                label = "保质期 / 有效期至",
                value = expiryDate,
                onClick = { datePickerTarget = 1 },
                onClear = { viewModel.setExpiryDate(null) }
            )
            Spacer(Modifier.height(8.dp))
            DateField(
                label = "保修期至",
                value = warrantyDate,
                onClick = { datePickerTarget = 2 },
                onClear = { viewModel.setWarrantyDate(null) }
            )

            Spacer(Modifier.height(16.dp))

            // ---- 描述 ----
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::onDescriptionChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                label = { Text("描述/备注") },
                placeholder = { Text("记录物品的详细信息等") },
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
                    if (viewModel.isEditing) "保存修改" else "添加物品",
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
            title = { Text("选择添加方式") },
            text = {
                Column {
                    TextButton(onClick = {
                        showImageSourceDialog = false; launchCamera()
                    }) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(Modifier.size(8.dp)); Text("拍照")
                    }
                    TextButton(onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Spacer(Modifier.size(8.dp)); Text("从相册选择")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text("取消") }
            }
        )
    }

    // ---- 日期选择器 ----
    val target = datePickerTarget
    if (target != null) {
        val initial = when (target) {
            0 -> purchaseDate
            1 -> expiryDate
            else -> warrantyDate
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
                        else -> viewModel.setWarrantyDate(millis)
                    }
                    datePickerTarget = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ---- 草稿恢复对话框 ----
    if (pendingDraft != null) {
        AlertDialog(
            onDismissRequest = { viewModel.discardDraft() },
            title = { Text("恢复上次内容？") },
            text = { Text("检测到有未完成的物品信息，是否恢复继续编辑？") },
            confirmButton = { TextButton(onClick = { viewModel.restoreDraft() }) { Text("恢复") } },
            dismissButton = { TextButton(onClick = { viewModel.discardDraft() }) { Text("丢弃") } }
        )
    }
}

/**
 * 日期选择字段（只读，点击弹出日期选择器）
 */
@Composable
private fun DateField(
    label: String,
    value: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        OutlinedTextField(
            value = DateUtils.formatDate(value),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text("点击选择日期") },
            singleLine = true,
            readOnly = true,
            enabled = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                if (value != null) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "清除日期",
                        modifier = Modifier.clickable(onClick = onClear)
                    )
                } else {
                    Icon(Icons.Default.Event, contentDescription = null, tint = Color.Gray)
                }
            }
        )
    }
}
