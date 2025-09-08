package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.RealmOOMAnalyzerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MemoryTestViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(this)
        
        enableEdgeToEdge()
        setContent {
            RealmOOMAnalyzerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MemoryTestScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryTestScreen(
    viewModel: MemoryTestViewModel,
    modifier: Modifier = Modifier
) {
    val logs = viewModel.logs
    val isRunning by viewModel.isRunning
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Realm OOM Memory Test",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.createDuplicateData() },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("1. 重複データ作成 (主キーなし)")
            }
            
            Button(
                onClick = { viewModel.createNormalizedData() },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("2. 正規化データ作成 (主キーあり)")
            }
            
            Button(
                onClick = { viewModel.startFlowMonitoring() },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("3. Flow監視開始")
            }
            
            Button(
                onClick = { viewModel.getAllTasks() },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("4. Task一覧取得 (バックグラウンド)")
            }
            
            Button(
                onClick = { viewModel.getAllTasksInBatches() },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("5. Task一覧取得 (バッチ処理)")
            }
            
            Button(
                onClick = { viewModel.getAllTasksOnMainThread() },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("6. Task一覧取得 (メインスレッド)")
            }
            
            Button(
                onClick = { viewModel.showMemoryUsage() },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("7. メモリ使用量表示")
            }
        }
        
        if (isRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("実行中...")
            }
        }
        
        Divider()
        
        // Log display
        Text(
            text = "ログ:",
            style = MaterialTheme.typography.titleMedium
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(logs.reversed()) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = log,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

