package com.example.itemmanager.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.itemmanager.R
import com.example.itemmanager.data.local.ItemEntity
import com.example.itemmanager.util.PrivacyManager
import java.io.File

/** 加密箱界面状态 */
private enum class VaultState {
    CHOOSE_TYPE,
    SETUP_PIN_1,
    SETUP_PIN_2,
    SETUP_PATTERN_1,
    SETUP_PATTERN_2,
    UNLOCK_PIN,
    UNLOCK_PATTERN,
    CONTENT
}

/**
 * 加密箱页
 * 首次进入选择并设置解锁方式（6 位数字密码 / 手势密码）；
 * 之后进入需验证；验证通过显示隐私物品，可移出加密箱。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    viewModel: PrivacyViewModel,
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { PrivacyManager.init(context) }

    // 初始状态：已设密码则进入对应解锁页，否则选择类型
    var state by remember {
        mutableStateOf(
            when (PrivacyManager.passwordType()) {
                PrivacyManager.TYPE_PIN -> VaultState.UNLOCK_PIN
                PrivacyManager.TYPE_PATTERN -> VaultState.UNLOCK_PATTERN
                else -> VaultState.CHOOSE_TYPE
            }
        )
    }

    // 临时保存第一次输入
    var firstPin by remember { mutableStateOf("") }
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }

    // 错误反馈
    var pinError by remember { mutableStateOf(false) }
    var patternError by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }

    if (state == VaultState.CONTENT) {
        VaultContent(
            viewModel = viewModel,
            onBack = onBack,
            onItemClick = onItemClick
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.privacy_vault),
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Lock, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            when (state) {
                // ---------- 首次：选择密码类型 ----------
                VaultState.CHOOSE_TYPE -> {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.vault_choose_type),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(40.dp))
                    Button(
                        onClick = { state = VaultState.SETUP_PIN_1; hint = "" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.vault_use_pin)) }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { state = VaultState.SETUP_PATTERN_1; hint = "" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.vault_use_pattern)) }
                }

                // ---------- 数字密码：设置/确认/解锁 ----------
                VaultState.SETUP_PIN_1,
                VaultState.SETUP_PIN_2,
                VaultState.UNLOCK_PIN -> {
                    val title = when (state) {
                        VaultState.SETUP_PIN_1 -> stringResource(R.string.vault_setup_pin)
                        VaultState.SETUP_PIN_2 -> stringResource(R.string.vault_confirm_pin)
                        else -> stringResource(R.string.vault_enter_pin)
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    if (hint.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(hint, color = Color(0xFFEF4444))
                    }
                    Spacer(Modifier.height(40.dp))
                    PinInput(
                        isError = pinError,
                        resetKey = state,
                        onErrorShown = { pinError = false },
                        onComplete = { pin ->
                            when (state) {
                                VaultState.SETUP_PIN_1 -> {
                                    firstPin = pin
                                    hint = ""
                                    state = VaultState.SETUP_PIN_2
                                }
                                VaultState.SETUP_PIN_2 -> {
                                    if (pin == firstPin) {
                                        PrivacyManager.setPin(pin)
                                        state = VaultState.CONTENT
                                    } else {
                                        hint = context.getString(R.string.vault_pin_mismatch)
                                        firstPin = ""
                                        state = VaultState.SETUP_PIN_1
                                    }
                                }
                                VaultState.UNLOCK_PIN -> {
                                    if (PrivacyManager.verifyPin(pin)) {
                                        state = VaultState.CONTENT
                                    } else {
                                        pinError = true
                                    }
                                }
                                else -> {}
                            }
                        }
                    )
                }

                // ---------- 手势密码：设置/确认/解锁 ----------
                VaultState.SETUP_PATTERN_1,
                VaultState.SETUP_PATTERN_2,
                VaultState.UNLOCK_PATTERN -> {
                    val title = when (state) {
                        VaultState.SETUP_PATTERN_1 -> stringResource(R.string.vault_setup_pattern)
                        VaultState.SETUP_PATTERN_2 -> stringResource(R.string.vault_confirm_pattern)
                        else -> stringResource(R.string.vault_enter_pattern)
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    if (hint.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(hint, color = Color(0xFFEF4444))
                    }
                    Spacer(Modifier.height(24.dp))
                    Box(modifier = Modifier.size(300.dp)) {
                        GestureLock(
                            activeColor = MaterialTheme.colorScheme.primary,
                            isError = patternError,
                            resetKey = state,
                            onReset = { patternError = false },
                            onComplete = { points ->
                                when (state) {
                                    VaultState.SETUP_PATTERN_1 -> {
                                        firstPattern = points
                                        hint = ""
                                        state = VaultState.SETUP_PATTERN_2
                                    }
                                    VaultState.SETUP_PATTERN_2 -> {
                                        if (points == firstPattern) {
                                            PrivacyManager.setPattern(points)
                                            state = VaultState.CONTENT
                                        } else {
                                            hint = context.getString(R.string.vault_pattern_mismatch)
                                            firstPattern = emptyList()
                                            state = VaultState.SETUP_PATTERN_1
                                        }
                                    }
                                    VaultState.UNLOCK_PATTERN -> {
                                        if (PrivacyManager.verifyPattern(points)) {
                                            state = VaultState.CONTENT
                                        } else {
                                            patternError = true
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.vault_pattern_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                VaultState.CONTENT -> {}
            }
        }
    }
}

/**
 * 已解锁后的加密箱内容：隐私物品列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultContent(
    viewModel: PrivacyViewModel,
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val items by viewModel.items.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.privacy_vault),
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.LockOpen, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.LightGray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.vault_empty),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    VaultItemCard(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        onRemove = { viewModel.removeFromVault(item) }
                    )
                }
            }
        }
    }
}

/**
 * 加密箱内的物品卡片
 */
@Composable
private fun VaultItemCard(
    item: ItemEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val firstImage = item.imagePaths.firstOrNull()
            if (firstImage != null && File(firstImage).exists()) {
                AsyncImage(
                    model = File(firstImage),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append(item.category)
                        if (item.brand.isNotBlank()) append(" · ").append(item.brand)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 移出加密箱
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.LockOpen,
                    contentDescription = stringResource(R.string.remove_from_vault),
                    tint = Color(0xFF10B981)
                )
            }
        }
    }
}
