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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.itemmanager.util.ImageUtils
import java.io.File

/**
 * 物品添加/编辑页
 * 支持调用系统相机拍照（使用手机原生相机算法）和从相册选择图片
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
    val imagePath by viewModel.imagePath.collectAsState()
    val tempImageUri by viewModel.tempImageUri.collectAsState()
    val cameraImagePath by viewModel.cameraImagePath.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val saveCompleted by viewModel.saveCompleted.collectAsState()
    val pendingDraft by viewModel.pendingDraft.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 保存拍照输出的 Uri（拍照前创建）
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    // 临时保存相机文件对象（用于拍照成功后获取路径）
    var cameraTempFile by remember { mutableStateOf<File?>(null) }

    // 保存完成后返回
    LaunchedEffect(saveCompleted) {
        if (saveCompleted) {
            onSaveComplete()
        }
    }

    // 错误提示
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 相册选择启动器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    // 系统相机拍照启动器（调用手机原生相机应用，使用原厂相机算法）
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempFile != null) {
            // 拍照成功，照片已直接写入内部存储文件
            viewModel.onPhotoTaken(cameraTempFile?.absolutePath)
        }
        pendingPhotoUri = null
        cameraTempFile = null
    }

    /**
     * 启动系统相机
     * 1. 创建临时图片文件（在 App 内部存储）
     * 2. 通过 FileProvider 获取 content Uri 并授权
     * 3. 调用系统相机应用，拍照后写入该 Uri
     */
    fun launchCamera() {
        val photoFile = ImageUtils.createCameraImageFile(context)
        if (photoFile != null) {
            val photoUri = ImageUtils.getUriForFile(context, photoFile)
            if (photoUri != null) {
                cameraTempFile = photoFile
                pendingPhotoUri = photoUri
                cameraLauncher.launch(photoUri)
            }
        }
    }

    // 决定显示哪张图片：新拍的 > 相册选的 > 已保存的
    val displayImage: Any? = when {
        cameraImagePath != null -> File(cameraImagePath!!)
        tempImageUri != null -> tempImageUri
        imagePath != null -> File(imagePath!!)
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isEditing) "编辑物品" else "添加物品",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ---- 图片预览区域 ----
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (displayImage != null) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = "物品图片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 清除图片按钮
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clickable { viewModel.clearImage() },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "移除图片",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(20.dp)
                        )
                    }
                } else {
                    // 无图片时的占位
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无图片",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- 拍照 / 相册 两个按钮 ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 调用系统相机拍照（使用手机原厂相机算法）
                Button(
                    onClick = { launchCamera() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "拍照", style = MaterialTheme.typography.labelLarge)
                }

                // 从相册选择
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "相册", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---- 表单字段 ----

            // 物品名称
            OutlinedTextField(
                value = name,
                onValueChange = viewModel::onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("物品名称 *") },
                placeholder = { Text("例如：iPhone 15 Pro") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 分类
            OutlinedTextField(
                value = category,
                onValueChange = viewModel::onCategoryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("分类 *") },
                placeholder = { Text("输入或从下方选择分类") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 分类快捷标签（横向滚动）
            Text(
                text = "快捷分类：",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryPresets.forEach { cat ->
                    val isSelected = category == cat
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { viewModel.onCategoryChanged(cat) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                        shape = CircleShape
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else Color.Gray,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 数量
            OutlinedTextField(
                value = quantity,
                onValueChange = viewModel::onQuantityChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("数量") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 存放位置
            OutlinedTextField(
                value = location,
                onValueChange = viewModel::onLocationChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("存放位置") },
                placeholder = { Text("例如：书房抽屉、客厅柜子") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 描述
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::onDescriptionChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("描述/备注") },
                placeholder = { Text("记录物品的详细信息、购买日期、保修等") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = { viewModel.saveItem() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (viewModel.isEditing) "保存修改" else "添加物品",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 草稿恢复对话框：添加模式下检测到上次未完成内容时弹出
    if (pendingDraft != null) {
        AlertDialog(
            onDismissRequest = { viewModel.discardDraft() },
            title = { Text("恢复上次内容？") },
            text = { Text("检测到有未完成的物品信息，是否恢复继续编辑？") },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreDraft() }) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.discardDraft() }) {
                    Text("丢弃")
                }
            }
        )
    }
}
