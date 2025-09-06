package com.example.myapplication

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ImageTitlesStorage
import com.example.myapplication.data.ImageTitlesStorageWithKey
import com.example.myapplication.data.TaskStorage
import com.example.myapplication.data.TaskStorageWithKey
import com.example.myapplication.domain.Task
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmChangeListener
import io.realm.RealmResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemoryTestViewModel : ViewModel() {
    private val TAG = "MemoryTest"
    val logs = mutableStateListOf<String>()
    val isRunning = mutableStateOf(false)
    
    // Global test iterations for both getAllTasks methods
    private val TEST_ITERATIONS = 10
    
    private var realm: Realm? = null
    private var taskStorageResults: RealmResults<TaskStorage>? = null
    private var taskStorageWithKeyResults: RealmResults<TaskStorageWithKey>? = null
    
    private lateinit var activityManager: ActivityManager
    
    fun init(context: Context) {
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        Realm.init(context)
        val config = RealmConfiguration.Builder()
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded()
            .allowWritesOnUiThread(true)
            .build()
        Realm.setDefaultConfiguration(config)
        
        realm = Realm.getDefaultInstance()
        
        // Clear all existing data on app launch
        realm?.executeTransaction { r ->
            log("Clearing all existing data...")
            r.delete(TaskStorage::class.java)
            r.delete(ImageTitlesStorage::class.java)
            r.delete(TaskStorageWithKey::class.java)
            r.delete(ImageTitlesStorageWithKey::class.java)
            log("All existing data cleared")
        }
        
        log("Realm initialized")
    }
    
    fun createDuplicateData() {
        viewModelScope.launch(Dispatchers.IO) {
            isRunning.value = true
            log("=== Starting duplicate data creation ===")
            logMemory("Before creation")
            
            val backgroundRealm = Realm.getDefaultInstance()
            backgroundRealm.executeTransaction { r ->
                // Clear existing data
                r.delete(TaskStorage::class.java)
                r.delete(ImageTitlesStorage::class.java)
                
                val startTime = System.currentTimeMillis()
                
                // Create 10,000 tasks with duplicate ImageTitlesStorage
                for (i in 1..1000) {
                    val task = r.createObject(TaskStorage::class.java, i.toLong())

                    // Add 5 duplicate ImageTitlesStorage objects
                    for (j in 1..50) {
                        val imageTitle = r.createObject(ImageTitlesStorage::class.java).apply {
                            title = "リビング"
                            category = "Inside"
                            portalCategory = "LivingDining"
                        }
                        task.imageTitles.add(imageTitle)
                    }
                    
                    if (i % 1000 == 0) {
                        log("Created $i tasks...")
                    }
                }
                
                val endTime = System.currentTimeMillis()
                log("Created 10,000 tasks with duplicate data in ${endTime - startTime}ms")
            }
            
            logMemory("After creation")
            log("Total ImageTitlesStorage objects: ${backgroundRealm.where(ImageTitlesStorage::class.java).count()}")
            log("Total TaskStorage objects: ${backgroundRealm.where(TaskStorage::class.java).count()}")
            backgroundRealm.close()
            isRunning.value = false
        }
    }
    
    fun createNormalizedData() {
        viewModelScope.launch(Dispatchers.IO) {
            isRunning.value = true
            log("=== Starting normalized data creation ===")
            logMemory("Before creation")
            
            val backgroundRealm = Realm.getDefaultInstance()
            backgroundRealm.executeTransaction { r ->
                // Clear existing data
                r.delete(TaskStorageWithKey::class.java)
                r.delete(ImageTitlesStorageWithKey::class.java)
                
                val startTime = System.currentTimeMillis()
                
                // Create single ImageTitlesStorage with key
                val sharedImageTitle = r.createObject(ImageTitlesStorageWithKey::class.java, "living_inside_1").apply {
                    title = "リビング"
                    category = "Inside"
                    portalCategory = "LivingDining"
                }
                
                // Create 10,000 tasks referencing the same ImageTitlesStorage
                for (i in 1..10000) {
                    val task = r.createObject(TaskStorageWithKey::class.java, i.toLong())
                    
                    // Reference the same object 5 times
                    for (j in 1..5) {
                        task.imageTitles.add(sharedImageTitle)
                    }
                    
                    if (i % 1000 == 0) {
                        log("Created $i tasks...")
                    }
                }
                
                val endTime = System.currentTimeMillis()
                log("Created 10,000 tasks with normalized data in ${endTime - startTime}ms")
            }
            
            logMemory("After creation")
            log("Total ImageTitlesStorageWithKey objects: ${backgroundRealm.where(ImageTitlesStorageWithKey::class.java).count()}")
            log("Total TaskStorageWithKey objects: ${backgroundRealm.where(TaskStorageWithKey::class.java).count()}")
            backgroundRealm.close()
            isRunning.value = false
        }
    }
    
    fun startFlowMonitoring() {
        // Remove existing listeners
        taskStorageResults?.removeAllChangeListeners()
        taskStorageWithKeyResults?.removeAllChangeListeners()
        
        log("=== Starting Realm monitoring ===")
        logMemory("Before monitoring start")
        
        // Monitor duplicate data
        val hasDuplicateData = realm?.where(TaskStorage::class.java)?.count() ?: 0 > 0
        val hasNormalizedData = realm?.where(TaskStorageWithKey::class.java)?.count() ?: 0 > 0
        
        when {
            hasDuplicateData -> {
                log("Monitoring duplicate data with RealmChangeListener...")
                taskStorageResults = realm?.where(TaskStorage::class.java)?.findAllAsync()
                
                taskStorageResults?.addChangeListener { tasks ->
                    log("Realm change detected: ${tasks.size} tasks")
                    logMemory("After change detection")
                    
                    // Convert to domain objects (this is where OOM might happen)
                    viewModelScope.launch {
                        try {
                            val domainTasks = tasks.map(::Task)
                            log("Converted ${domainTasks.size} tasks to domain objects")
                        } catch (e: OutOfMemoryError) {
                            log("⚠️ OutOfMemoryError during domain conversion!")
                        }
                    }
                }
            }
            hasNormalizedData -> {
                log("Monitoring normalized data with RealmChangeListener...")
                taskStorageWithKeyResults = realm?.where(TaskStorageWithKey::class.java)?.findAllAsync()
                
                taskStorageWithKeyResults?.addChangeListener { tasks ->
                    log("Realm change detected: ${tasks.size} tasks")
                    logMemory("After change detection")
                    
                    // Convert to domain objects (this is where OOM might happen)
                    viewModelScope.launch {
                        try {
                            val domainTasks = tasks.map(::Task)
                            log("Converted ${domainTasks.size} tasks to domain objects")
                        } catch (e: OutOfMemoryError) {
                            log("⚠️ OutOfMemoryError during domain conversion!")
                        }
                    }
                }
            }
            else -> {
                log("No data to monitor")
            }
        }
    }
    
    fun getAllTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            isRunning.value = true
            log("=== Starting $TEST_ITERATIONS continuous task retrievals ===")
            logMemory("Before continuous retrievals")
            
            val overallStartTime = System.currentTimeMillis()
            val backgroundRealm = Realm.getDefaultInstance()
            
            val hasDuplicateData = backgroundRealm.where(TaskStorage::class.java).count() > 0
            val hasNormalizedData = backgroundRealm.where(TaskStorageWithKey::class.java).count() > 0
            
            if (!hasDuplicateData && !hasNormalizedData) {
                log("No data found to retrieve")
                backgroundRealm.close()
                isRunning.value = false
                return@launch
            }
            
            var crashOccurred = false
            
            for (i in 1..TEST_ITERATIONS) {
                if (crashOccurred) break
                
                log("--- Retrieval #$i/$TEST_ITERATIONS ---")
                val startTime = System.currentTimeMillis()
                
                try {
                    when {
                        hasDuplicateData -> {
                            val tasks = backgroundRealm.where(TaskStorage::class.java)
                                .findAll()
                                .map(::Task)
                            
                            log("#$i: Retrieved ${tasks.size} tasks from duplicate data")
                            
                            // Count total objects created
                            var totalImageTitles = 0
                            tasks.forEach { task ->
                                totalImageTitles += task.imageTitles.size
                            }
                            log("#$i: Total TaskImageTitles objects created: $totalImageTitles")
                        }
                        hasNormalizedData -> {
                            val tasks = backgroundRealm.where(TaskStorageWithKey::class.java)
                                .findAll()
                                .map(::Task)
                            
                            log("#$i: Retrieved ${tasks.size} tasks from normalized data")
                            
                            // Count total objects created
                            var totalImageTitles = 0
                            tasks.forEach { task ->
                                totalImageTitles += task.imageTitles.size
                            }
                            log("#$i: Total TaskImageTitles objects created: $totalImageTitles")
                        }
                    }
                    
                    val endTime = System.currentTimeMillis()
                    log("#$i: Completed in ${endTime - startTime}ms")
                    
                    // Log memory every 5 iterations
                    if (i % 5 == 0) {
                        logMemory("After iteration $i")
                    }
                    
                } catch (e: OutOfMemoryError) {
                    log("⚠️ OutOfMemoryError occurred at iteration #$i!")
                    log("Error: ${e.message}")
                    crashOccurred = true
                } catch (e: Exception) {
                    log("⚠️ Exception occurred at iteration #$i: ${e.message}")
                    crashOccurred = true
                }
                
                // Small delay to prevent complete UI freeze
                Thread.sleep(50)
            }
            
            val overallEndTime = System.currentTimeMillis()
            log("=== Continuous retrieval completed ===")
            log("Total time: ${overallEndTime - overallStartTime}ms")
            logMemory("After all retrievals")
            
            // Force GC and log memory again
            System.gc()
            Thread.sleep(100)
            logMemory("After final GC")
            
            backgroundRealm.close()
            isRunning.value = false
        }
    }

    fun getAllTasksInBatches() {
        viewModelScope.launch(Dispatchers.IO) {
            isRunning.value = true
            log("=== Starting $TEST_ITERATIONS iterations of batch processing (100 items per batch) ===")
            logMemory("Before batch processing")
            
            val overallStartTime = System.currentTimeMillis()
            val backgroundRealm = Realm.getDefaultInstance()
            
            val hasDuplicateData = backgroundRealm.where(TaskStorage::class.java).count() > 0
            val hasNormalizedData = backgroundRealm.where(TaskStorageWithKey::class.java).count() > 0
            
            if (!hasDuplicateData && !hasNormalizedData) {
                log("No data found to retrieve")
                backgroundRealm.close()
                isRunning.value = false
                return@launch
            }
            
            val totalTasks = when {
                hasDuplicateData -> backgroundRealm.where(TaskStorage::class.java).count().toInt()
                hasNormalizedData -> backgroundRealm.where(TaskStorageWithKey::class.java).count().toInt()
                else -> 0
            }
            
            val batchSize = 100
            val totalBatches = (totalTasks + batchSize - 1) / batchSize
            
            log("Total tasks: $totalTasks, Batch size: $batchSize, Total batches per iteration: $totalBatches")
            
            var crashOccurred = false
            var grandTotalProcessedTasks = 0
            var grandTotalImageTitlesCreated = 0
            
            // Run TEST_ITERATIONS iterations of full batch processing
            for (iteration in 1..TEST_ITERATIONS) {
                if (crashOccurred) break
                
                log("=== Iteration $iteration/$TEST_ITERATIONS ===")
                var totalProcessedTasks = 0
                var totalImageTitlesCreated = 0
                
                for (batchIndex in 0 until totalBatches) {
                    if (crashOccurred) break
                
                    val startOffset = batchIndex * batchSize
                    val endOffset = minOf((batchIndex + 1) * batchSize, totalTasks)
                    val currentBatchSize = endOffset - startOffset
                    
                    log("--- Iter $iteration: Batch ${batchIndex + 1}/$totalBatches (offset: $startOffset, size: $currentBatchSize) ---")
                    val startTime = System.currentTimeMillis()
                    
                    try {
                        when {
                            hasDuplicateData -> {
                                val tasks = backgroundRealm.where(TaskStorage::class.java)
                                    .findAll()
                                    .let { allTasks ->
                                        allTasks.subList(startOffset, endOffset).map(::Task)
                                    }
                                
                                log("Iter $iteration Batch ${batchIndex + 1}: Retrieved ${tasks.size} tasks from duplicate data")
                                
                                // Count objects in this batch
                                var batchImageTitles = 0
                                tasks.forEach { task ->
                                    batchImageTitles += task.imageTitles.size
                                }
                                log("Iter $iteration Batch ${batchIndex + 1}: TaskImageTitles objects created: $batchImageTitles")
                                totalImageTitlesCreated += batchImageTitles
                                totalProcessedTasks += tasks.size
                            }
                            hasNormalizedData -> {
                                val tasks = backgroundRealm.where(TaskStorageWithKey::class.java)
                                    .findAll()
                                    .let { allTasks ->
                                        allTasks.subList(startOffset, endOffset).map(::Task)
                                    }
                                
                                log("Iter $iteration Batch ${batchIndex + 1}: Retrieved ${tasks.size} tasks from normalized data")
                                
                                // Count objects in this batch
                                var batchImageTitles = 0
                                tasks.forEach { task ->
                                    batchImageTitles += task.imageTitles.size
                                }
                                log("Iter $iteration Batch ${batchIndex + 1}: TaskImageTitles objects created: $batchImageTitles")
                                totalImageTitlesCreated += batchImageTitles
                                totalProcessedTasks += tasks.size
                            }
                        }
                        
                        val endTime = System.currentTimeMillis()
                        log("Iter $iteration Batch ${batchIndex + 1}: Completed in ${endTime - startTime}ms")
                        
                        // Log memory every 5 batches within iteration
                        if ((batchIndex + 1) % 5 == 0) {
                            logMemory("After iter $iteration batch ${batchIndex + 1}")
                        }
                        
                        // Force GC every 10 batches to prevent memory buildup
                        if ((batchIndex + 1) % 10 == 0) {
                            System.gc()
                            Thread.sleep(50)
                            log("Forced GC after iter $iteration batch ${batchIndex + 1}")
                        }
                        
                    } catch (e: OutOfMemoryError) {
                        log("⚠️ OutOfMemoryError occurred at iteration $iteration batch ${batchIndex + 1}!")
                        log("Error: ${e.message}")
                        crashOccurred = true
                    } catch (e: Exception) {
                        log("⚠️ Exception occurred at iteration $iteration batch ${batchIndex + 1}: ${e.message}")
                        crashOccurred = true
                    }
                    
                    // Small delay between batches
                    Thread.sleep(50)
                }
                
                // Log iteration summary
                log("--- Iteration $iteration completed ---")
                log("Iter $iteration: Processed $totalProcessedTasks tasks, Created $totalImageTitlesCreated TaskImageTitles")
                grandTotalProcessedTasks += totalProcessedTasks
                grandTotalImageTitlesCreated += totalImageTitlesCreated
                
                // Log memory after each iteration
                logMemory("After iteration $iteration")
                
                // Force GC between iterations
                System.gc()
                Thread.sleep(100)
            }
            
            val overallEndTime = System.currentTimeMillis()
            log("=== All batch processing iterations completed ===")
            log("Grand total processed tasks: $grandTotalProcessedTasks")
            log("Grand total ImageTitles objects created: $grandTotalImageTitlesCreated")
            log("Total time: ${overallEndTime - overallStartTime}ms")
            logMemory("After all batch processing")
            
            // Final GC
            System.gc()
            Thread.sleep(100)
            logMemory("After final GC")
            
            backgroundRealm.close()
            isRunning.value = false
        }
    }
    
    fun showMemoryUsage() {
        log("=== Current Memory Usage ===")
        logMemory("Current")
        
        // Get GC info
        val runtime = Runtime.getRuntime()
        log("Max heap: ${runtime.maxMemory() / 1024 / 1024} MB")
        log("Total heap: ${runtime.totalMemory() / 1024 / 1024} MB")
        log("Free heap: ${runtime.freeMemory() / 1024 / 1024} MB")
        log("Used heap: ${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024} MB")
        
        // Try allocation test to detect fragmentation
        testMemoryFragmentation()
    }
    
    private fun testMemoryFragmentation() {
        log("=== Memory Fragmentation Test ===")
        val testSizes = listOf(1024, 4096, 16384, 65536, 262144) // 1KB to 256KB
        
        for (size in testSizes) {
            try {
                val testArray = ByteArray(size)
                log("✅ Successfully allocated ${size / 1024}KB")
                // Immediately release
                @Suppress("UNUSED_VALUE")
                testArray.fill(0)
            } catch (e: OutOfMemoryError) {
                log("❌ Failed to allocate ${size / 1024}KB - Fragmentation detected!")
                break
            }
        }
        
        // Force GC and test again
        log("--- After GC ---")
        System.gc()
        Thread.sleep(100)
        
        for (size in testSizes) {
            try {
                val testArray = ByteArray(size)
                log("✅ Post-GC: Successfully allocated ${size / 1024}KB")
                @Suppress("UNUSED_VALUE")
                testArray.fill(0)
            } catch (e: OutOfMemoryError) {
                log("❌ Post-GC: Still failed to allocate ${size / 1024}KB")
                break
            }
        }
    }
    
    private fun logMemory(label: String) {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        
        log("[$label] Heap: $usedMemory MB used / $maxMemory MB max (${(usedMemory * 100 / maxMemory)}%)")
        log("[$label] Total heap allocated: $totalMemory MB, Free in allocated: $freeMemory MB")
        log("[$label] System available memory: ${memInfo.availMem / 1024 / 1024} MB")
        
        // Memory fragmentation indicators
        val freePercent = (freeMemory * 100 / totalMemory).toInt()
        val remainingCapacity = maxMemory - totalMemory
        
        log("[$label] Free within allocated: $freePercent%, Remaining capacity: $remainingCapacity MB")
        
        // Fragmentation warning signs
        when {
            freePercent > 50 && usedMemory > 100 -> {
                log("🟡 Possible fragmentation: High free space but significant usage")
            }
            totalMemory > maxMemory * 0.8 && freePercent > 30 -> {
                log("🟠 Fragmentation likely: Near max capacity with high free percentage")
            }
        }
        
        if (memInfo.lowMemory) {
            log("⚠️ SYSTEM LOW MEMORY WARNING (not app-specific)")
        }
        
        // App-specific memory pressure check
        val usagePercent = (usedMemory * 100 / maxMemory).toInt()
        when {
            usagePercent > 85 -> log("🔴 App memory usage HIGH: $usagePercent%")
            usagePercent > 70 -> log("🟡 App memory usage MODERATE: $usagePercent%")
            else -> log("🟢 App memory usage OK: $usagePercent%")
        }
    }
    
    private fun log(message: String) {
        Log.d(TAG, message)
        viewModelScope.launch {
            logs.add("${System.currentTimeMillis()}: $message")
            // Keep only last 100 logs
            while (logs.size > 100) {
                logs.removeAt(0)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        taskStorageResults?.removeAllChangeListeners()
        taskStorageWithKeyResults?.removeAllChangeListeners()
        realm?.close()
    }
}