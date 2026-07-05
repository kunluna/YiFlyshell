package com.yishell.app.presentation.sftp

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yishell.app.data.model.SftpItem
import com.yishell.app.presentation.theme.*
import com.yishell.app.presentation.util.FormatUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SftpScreen(
    connectionId: String,
    onBack: () -> Unit,
    viewModel: SftpViewModel = hiltViewModel()
) {
    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "upload_file"
            val cacheFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(it)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.uploadFile(cacheFile)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf<SftpItem?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<SftpItem?>(null) }
    var showDetailDialog by remember { mutableStateOf<SftpItem?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                TopAppBar(
                    title = { Text("已选 ${selectedFiles.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "取消选择"
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Text("全选")
                        }
                        TextButton(onClick = { showBatchDeleteDialog = true }) {
                            Text("删除", color = Danger)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LightBackground
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("文件管理") },
                    navigationIcon = {
                        IconButton(onClick = { showExitDialog = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LightBackground
                    )
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = LightBackground
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "新建文件夹",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建文件夹", fontSize = 12.sp)
                    }
                    FilledTonalButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "上传",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("上传", fontSize = 12.sp)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PathBreadcrumb(
                currentPath = currentPath,
                onPathClick = { index -> viewModel.navigateToPath(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // P2-4：文件传输进度条，传输中显示在列表顶部。
            transferProgress?.let { progress ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${if (progress.isDownload) "下载" else "上传"}: ${progress.fileName} (${progress.percent}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (progress.total > 0) {
                        LinearProgressIndicator(
                            progress = { progress.percent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            HorizontalDivider(color = LightSurfaceVariant)

            when {
                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = AnsiRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error ?: "",
                                color = AnsiRed,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.clearError(); viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                isLoading && files.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                !isLoading && files.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "此文件夹为空",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (currentPath != "/") {
                                item {
                                    FileListItem(
                                        item = SftpItem(
                                            name = "..",
                                            path = "",
                                            isDir = true
                                        ),
                                        onClick = { viewModel.navigateUp() },
                                        onLongClick = {},
                                        isSelected = false,
                                        isMultiSelectMode = false
                                    )
                                }
                            }

                            items(files) { item ->
                                FileListItem(
                                    item = item,
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            viewModel.toggleSelection(item.name)
                                        } else if (item.isDir) {
                                            viewModel.navigateToFolder(item.path)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(item.name)
                                    },
                                    isSelected = selectedFiles.contains(item.name),
                                    isMultiSelectMode = isMultiSelectMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    showContextMenu?.let { item ->
        FileContextMenu(
            item = item,
            onDismiss = { showContextMenu = null },
            onDownload = {
                viewModel.downloadFile(item)
                showContextMenu = null
            },
            onRename = {
                showRenameDialog = item
                showContextMenu = null
            },
            onDetail = {
                showDetailDialog = item
                showContextMenu = null
            },
            onDelete = {
                viewModel.deleteFile(item)
                showContextMenu = null
            }
        )
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("文件夹名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createFolder(folderName)
                    showCreateFolderDialog = false
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("取消") }
            }
        )
    }

    showRenameDialog?.let { item ->
        var newName by remember(item) { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameItem(item, newName)
                    showRenameDialog = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("取消") }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("确认退出") },
            text = { Text("确定要退出文件管理吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBack()
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("取消") }
            }
        )
    }

    showDetailDialog?.let { item ->
        val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { Text(item.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("类型", if (item.isDir) "文件夹" else "文件")
                    DetailRow("大小", FormatUtils.formatBytes(item.size))
                    DetailRow("路径", item.path)
                    if (item.modTime > 0) {
                        DetailRow("修改时间", dateFormat.format(java.util.Date(item.modTime)))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailDialog = null }) { Text("关闭") }
            }
        )
    }

    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selectedFiles.size} 个项目吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelected()
                    showBatchDeleteDialog = false
                }) { Text("删除", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
fun PathBreadcrumb(
    currentPath: String,
    onPathClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = currentPath.split("/").filter { it.isNotEmpty() }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "/",
            color = PrimaryBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onPathClick(-1) }
        )

        segments.forEachIndexed { index, segment ->
            Text(
                text = " / ",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = segment,
                color = if (index < segments.size - 1) PrimaryBlue else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (index == segments.size - 1) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable { onPathClick(index) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: SftpItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(if (isSelected) Modifier.background(PrimaryBlue.copy(alpha = 0.1f)) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) PrimaryBlue else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (item.isDir) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .blueAcrylicGlass(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = TextPrimary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.isDir && item.modTime > 0) {
                Text(
                    text = dateFormat.format(Date(item.modTime)),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (!item.isDir) {
            Text(
                text = FormatUtils.formatBytes(item.size),
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }

    HorizontalDivider(
        color = LightSurfaceVariant,
        modifier = Modifier.padding(start = 52.dp)
    )
}

@Composable
fun FileContextMenu(
    item: SftpItem,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDetail: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("详情") },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onDetail() }
                )
                ListItem(
                    headlineContent = { Text("重命名") },
                    leadingContent = {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onRename() }
                )
                if (!item.isDir) {
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("下载") },
                        leadingContent = {
                            Icon(Icons.Default.Download, contentDescription = null)
                        },
                        modifier = Modifier.clickable { onDownload() }
                    )
                }
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("删除") },
                    leadingContent = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Danger)
                    },
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = cursor.getString(index)
                }
            }
        }
    }
    if (name == null) {
        name = uri.path?.substringAfterLast('/')
    }
    return name
}
