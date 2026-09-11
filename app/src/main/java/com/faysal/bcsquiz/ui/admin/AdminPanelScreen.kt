package com.faysal.bcsquiz.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faysal.bcsquiz.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(initialTab: Int = 0, initialEditId: String? = null, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val db = FirebaseFirestore.getInstance()
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var userProfile by remember { mutableStateOf<User?>(null) }
    var editTargetQuestionId by remember { mutableStateOf(initialEditId) }

    LaunchedEffect(Unit) {
        userProfile = db.collection("users").document(uid).get().await().toObject(User::class.java)
    }
    
    val isOwner = userProfile?.role == "owner"
    val adminName = userProfile?.name ?: "Admin"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Console") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.People, null) }, label = { Text("Users") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.AddCircle, null) }, label = { Text("Questions") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Category, null) }, label = { Text("Categories") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("History") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> UserManagementTab(isOwner = isOwner)
                1 -> QuestionManagementTab(uid = uid, adminName = adminName, initialEditId = editTargetQuestionId, onEditHandled = { editTargetQuestionId = null })
                2 -> CategoryManagementTab(adminUid = uid, adminName = adminName)
                3 -> AdminLogTab(showAll = true, isOwner = isOwner, onEditAction = { targetId, type -> 
                    if (type.contains("question")) {
                        editTargetQuestionId = targetId
                        selectedTab = 1
                    }
                })
            }
        }
    }
}

@Composable
fun UserManagementTab(isOwner: Boolean) {
    var searchQuery by remember { mutableStateOf("") }
    var userList by remember { mutableStateOf<List<User>>(emptyList()) }
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("users").orderBy("joinedAt", Query.Direction.DESCENDING).limit(100).addSnapshotListener { s, _ ->
            val list = s?.toObjects(User::class.java) ?: emptyList()
            if (searchQuery.isEmpty()) {
                userList = list.sortedByDescending { it.joinedAt ?: com.google.firebase.Timestamp(0, 0) }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Gmail") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    scope.launch {
                        val snapshot = db.collection("users")
                            .whereEqualTo("email", searchQuery.lowercase().trim())
                            .get().await()
                        userList = snapshot.toObjects(User::class.java)
                    }
                }) { Icon(Icons.Default.Search, null) }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(userList) { user ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    val isOwnerProfile = user.email.lowercase() == "frfaysal6072@gmail.com"
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(user.name, fontWeight = FontWeight.Bold)
                        Text(user.email, style = MaterialTheme.typography.bodySmall)
                        val plan = if (user.isSubscribed) "PRO" else "FREE"
                        val roleText = if (isOwnerProfile) "OWNER" else user.role.uppercase()
                        Text("Role: $roleText" + (if (user.role == "user") " ($plan)" else ""), color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { scope.launch { db.collection("users").document(user.uid).update("isSubscribed", false, "role", "user").await() } }, enabled = !isOwnerProfile) { Text("Make Free") }
                            TextButton(onClick = { scope.launch { db.collection("users").document(user.uid).update("isSubscribed", true, "role", "user").await() } }, enabled = !isOwnerProfile) { Text("Make Pro") }
                            if (isOwner && user.role != "owner") {
                                TextButton(onClick = { scope.launch { db.collection("users").document(user.uid).update("role", "admin").await() } }, enabled = !isOwnerProfile) { Text("Make Admin") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionManagementTab(uid: String, adminName: String, initialEditId: String? = null, onEditHandled: () -> Unit) {
    var questionText by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "", "", "")) }
    var correctIndex by remember { mutableStateOf(0) }
    var selectedCategoryId by remember { mutableStateOf("") }
    var selectedSubCategoryId by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var subCategories by remember { mutableStateOf<List<SubCategory>>(emptyList()) }
    var searchKey by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Question>>(emptyList()) }
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        categories = db.collection("categories").get().await().toObjects(Category::class.java)
        if (categories.isNotEmpty() && selectedCategoryId.isEmpty()) selectedCategoryId = categories[0].id
        
        initialEditId?.let { id ->
            db.collection("questions").document(id).get().await().toObject(Question::class.java)?.let { q ->
                questionText = q.question; options = q.options; correctIndex = q.correctIndex
                selectedCategoryId = if (q.categoryId.isNotEmpty()) q.categoryId else q.subject
                selectedSubCategoryId = q.subCategoryId
                onEditHandled()
            }
        }
    }

    LaunchedEffect(selectedCategoryId) {
        if (selectedCategoryId.isNotEmpty()) {
            val snapshot = db.collection("categories").document(selectedCategoryId).collection("subcategories").get().await()
            subCategories = snapshot.toObjects(SubCategory::class.java)
            if (subCategories.none { it.id == selectedSubCategoryId }) selectedSubCategoryId = null
        }
    }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("Find Question", fontWeight = FontWeight.Bold)
            TextField(value = searchKey, onValueChange = { searchKey = it }, label = { Text("Search Keywords") }, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                IconButton(onClick = {
                    scope.launch {
                        val snapshot = db.collection("questions").orderBy("question").startAt(searchKey).endAt(searchKey + "\uf8ff").limit(10).get().await()
                        searchResults = snapshot.toObjects(Question::class.java)
                    }
                }) { Icon(Icons.Default.Search, null) }
            })
            searchResults.forEach { q ->
                TextButton(onClick = { 
                    questionText = q.question; options = q.options; correctIndex = q.correctIndex
                    selectedCategoryId = if (q.categoryId.isNotEmpty()) q.categoryId else q.subject
                    selectedSubCategoryId = q.subCategoryId
                }) { Text(q.question.take(50) + "...") }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Add / Edit Question", style = MaterialTheme.typography.titleLarge)
            
            Text("Main Category:", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
            categories.forEach { cat ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = selectedCategoryId == cat.id, onClick = { selectedCategoryId = cat.id })
                    Text(cat.name)
                }
            }

            if (subCategories.isNotEmpty()) {
                Text("Sub Category:", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = selectedSubCategoryId == null, onClick = { selectedSubCategoryId = null })
                    Text("None")
                }
                subCategories.forEach { sub ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = selectedSubCategoryId == sub.id, onClick = { selectedSubCategoryId = sub.id })
                        Text(sub.name)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TextField(value = questionText, onValueChange = { questionText = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
            options.forEachIndexed { i, s -> TextField(value = s, onValueChange = { val l = options.toMutableList(); l[i] = it; options = l }, label = { Text("Option ${i+1}") }, modifier = Modifier.padding(top = 4.dp).fillMaxWidth()) }
            Text("Correct Option:", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
            Row { (0..3).forEach { i -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(selected = correctIndex == i, onClick = { correctIndex = i }); Text("${i+1}") } } }
            Button(onClick = {
                if (questionText.isBlank()) return@Button
                scope.launch {
                    val qId = questionText.hashCode().toString()
                    val existing = db.collection("questions").document(qId).get().await()
                    val q = Question(id = qId, question = questionText, options = options, correctIndex = correctIndex, subject = selectedCategoryId, categoryId = selectedCategoryId, subCategoryId = selectedSubCategoryId, addedBy = uid)
                    db.collection("questions").document(qId).set(q).await()
                    val actionType = if (existing.exists()) "edit_question" else "add_question"
                    val logId = db.collection("admin_activities").document().id
                    db.collection("admin_activities").document(logId).set(AdminActivity(logId, uid, adminName, actionType, qId, if (existing.exists()) "Edited question" else "Added question", com.google.firebase.Timestamp.now())).await()
                    questionText = ""; options = listOf("", "", "", ""); searchResults = emptyList(); selectedSubCategoryId = null
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Save Question") }
        }
    }
}

@Composable
fun CategoryManagementTab(adminUid: String, adminName: String) {
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var subCategoriesMap by remember { mutableStateOf<Map<String, List<SubCategory>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        db.collection("categories").addSnapshotListener { s, _ -> 
            val cats = s?.toObjects(Category::class.java) ?: emptyList()
            categories = cats
            scope.launch {
                val newMap = mutableMapOf<String, List<SubCategory>>()
                cats.forEach { cat ->
                    val subs = db.collection("categories").document(cat.id).collection("subcategories").get().await().toObjects(SubCategory::class.java)
                    newMap[cat.id] = subs
                }
                subCategoriesMap = newMap
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Create / Edit Category", fontWeight = FontWeight.Bold)
        
        Text("Type:", modifier = Modifier.padding(top = 8.dp))
        Row {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = selectedParentId == null, onClick = { selectedParentId = null })
                Text("Main")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = selectedParentId != null, onClick = { if (categories.isNotEmpty()) selectedParentId = categories[0].id })
                Text("Sub")
            }
        }

        if (selectedParentId != null) {
            Text("Select Parent Category:")
            categories.forEach { cat ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = selectedParentId == cat.id, onClick = { selectedParentId = cat.id })
                    Text(cat.name)
                }
            }
        }

        TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        TextField(value = id, onValueChange = { id = it }, label = { Text("ID (Slug)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        
        Button(onClick = {
            if (name.isBlank() || id.isBlank()) return@Button
            scope.launch {
                if (selectedParentId == null) {
                    db.collection("categories").document(id).set(Category(id, name)).await()
                } else {
                    db.collection("categories").document(selectedParentId!!).collection("subcategories").document(id).set(SubCategory(id, selectedParentId!!, name)).await()
                }
                val logId = db.collection("admin_activities").document().id
                db.collection("admin_activities").document(logId).set(AdminActivity(logId, adminUid, adminName, "add_category", id, "Manage category: $name", com.google.firebase.Timestamp.now())).await()
                name = ""; id = ""
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Add / Update") }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("All Categories & Subs:", fontWeight = FontWeight.Bold)
        LazyColumn {
            items(categories) { cat ->
                ListItem(
                    headlineContent = { Text(cat.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(cat.id) },
                    trailingContent = {
                        IconButton(onClick = { scope.launch { db.collection("categories").document(cat.id).delete().await() } }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    }
                )
                subCategoriesMap[cat.id]?.forEach { sub ->
                    ListItem(
                        headlineContent = { Text("  ↳ ${sub.name}") },
                        supportingContent = { Text("    ID: ${sub.id}") },
                        trailingContent = {
                            IconButton(onClick = { scope.launch { db.collection("categories").document(cat.id).collection("subcategories").document(sub.id).delete().await() } }) { Icon(Icons.Default.Delete, null, tint = Color.LightGray) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminLogTab(uid: String = "", showAll: Boolean = false, isOwner: Boolean = false, onEditAction: (String, String) -> Unit = { _, _ -> }) {
    val db = FirebaseFirestore.getInstance()
    var logs by remember { mutableStateOf<List<AdminActivity>>(emptyList()) }
    LaunchedEffect(Unit) {
        val query = if (showAll) db.collection("admin_activities") else db.collection("admin_activities").whereEqualTo("adminUid", uid)
        query.get().await().let { s -> 
            val raw = s.toObjects(AdminActivity::class.java)
            logs = if (isOwner) raw else raw.filter { it.actionType !in listOf("handle_topup", "handle_withdrawal") }
            logs = logs.sortedByDescending { it.timestamp }
        }
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(if (showAll) "System Activity Log" else "My Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyColumn {
            items(logs) { log ->
                ListItem(
                    headlineContent = { Text(log.description) },
                    supportingContent = { Text("${log.adminName} at ${log.timestamp?.toDate()?.toLocaleString()}") },
                    trailingContent = {
                        if (log.actionType.contains("question")) {
                            IconButton(onClick = { onEditAction(log.targetId, log.actionType) }) { Icon(Icons.Default.Edit, null) }
                        }
                    }
                )
            }
        }
    }
}
