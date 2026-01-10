package com.tugasuas.matask

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Date

enum class SortOrder {
    MY_ORDER,
    DATE,
    RECENTLY_STARRED,
    ALPHABETICAL
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val listenerRegistrations = mutableMapOf<String, ListenerRegistration>()
    private val notificationHelper = NotificationHelper(application)

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    override fun onCleared() {
        super.onCleared()
        listenerRegistrations.values.forEach { it.remove() }
        listenerRegistrations.clear()
    }

    private fun getUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun getBaseQuery(categoryId: String?, isFavorites: Boolean, isOverdue: Boolean = false): Query? {
        val userId = getUserId() ?: return null
        val collection = db.collection("users").document(userId).collection("tasks")
        
        return when {
            isOverdue -> collection
            isFavorites -> collection.whereEqualTo("favorite", true)
            else -> collection.whereEqualTo("categoryId", categoryId)
        }
    }

    private fun getSortedQuery(baseQuery: Query, sortOrder: SortOrder): Query {
        return when (sortOrder) {
            SortOrder.MY_ORDER -> baseQuery.orderBy("position", Query.Direction.ASCENDING)
            SortOrder.DATE -> baseQuery.orderBy("deadline", Query.Direction.ASCENDING)
            SortOrder.RECENTLY_STARRED -> baseQuery.orderBy("favoritedTimestamp", Query.Direction.DESCENDING)
            SortOrder.ALPHABETICAL -> baseQuery.orderBy("title", Query.Direction.ASCENDING)
        }
    }

    fun getIncompleteTasks(categoryId: String?, isFavorites: Boolean, sortOrder: SortOrder = SortOrder.MY_ORDER, isOverdue: Boolean = false): LiveData<List<Task>> {
        val key = "incomplete_${if (isOverdue) "overdue" else if (isFavorites) "favorites" else categoryId}_${sortOrder.name}"
        listenerRegistrations[key]?.remove()

        val liveData = MutableLiveData<List<Task>>()
        val query = getBaseQuery(categoryId, isFavorites, isOverdue)
        
        if (query != null) {
            val registration = getSortedQuery(query, sortOrder).addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("TaskViewModel", "Listen failed: ${e.message}")
                    return@addSnapshotListener
                }
                val allTasks = snapshots?.toObjects(Task::class.java) ?: emptyList()
                val now = Date()
                
                liveData.value = allTasks.filter { task ->
                    if (isOverdue) {
                        val deadline = task.deadline
                        !task.completed && deadline != null && deadline.toDate().before(now)
                    } else {
                        !task.completed
                    }
                }
            }
            listenerRegistrations[key] = registration
        }
        return liveData
    }

    fun getCompletedTasks(categoryId: String?, isFavorites: Boolean): LiveData<List<Task>> {
        val key = "completed_${if (isFavorites) "favorites" else categoryId}"
        listenerRegistrations[key]?.remove()

        val liveData = MutableLiveData<List<Task>>()
        val query = getBaseQuery(categoryId, isFavorites)
        
        if (query != null) {
            val registration = query.orderBy("position", Query.Direction.ASCENDING).addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val allTasks = snapshots?.toObjects(Task::class.java) ?: emptyList()
                liveData.value = allTasks.filter { it.completed }
            }
            listenerRegistrations[key] = registration
        }
        return liveData
    }

    fun getCategories() {
        val userId = getUserId() ?: return
        db.collection("users").document(userId).collection("categories")
            .orderBy("position", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                _categories.value = snapshots?.toObjects(Category::class.java) ?: emptyList()
            }
    }

    fun addTask(task: Task) {
        val userId = getUserId() ?: return
        db.collection("users").document(userId).collection("tasks").add(task)
            .addOnSuccessListener { docRef ->
                val taskWithId = task.apply { id = docRef.id }
                if (taskWithId.deadline != null && !taskWithId.completed) {
                    notificationHelper.scheduleNotification(taskWithId)
                }
            }
    }

    fun updateTask(task: Task) {
        val userId = getUserId() ?: return
        
        if (task.completed && task.completedTimestamp == null) {
            task.completedTimestamp = Timestamp.now()
        } else if (!task.completed) {
            task.completedTimestamp = null
        }

        if (task.favorite && task.favoritedTimestamp == null) {
            task.favoritedTimestamp = Timestamp.now()
        }
        
        db.collection("users").document(userId).collection("tasks").document(task.id).set(task)
            .addOnSuccessListener {
                if (task.deadline != null && !task.completed) {
                    notificationHelper.scheduleNotification(task)
                }
            }
    }

    fun updateTaskOrder(tasks: List<Task>) {
        val userId = getUserId() ?: return
        val batch = db.batch()
        tasks.forEachIndexed { index, task ->
            val docRef = db.collection("users").document(userId).collection("tasks").document(task.id)
            batch.update(docRef, "position", index.toLong())
        }
        batch.commit()
    }

    fun deleteTask(task: Task) {
        val userId = getUserId() ?: return
        db.collection("users").document(userId).collection("tasks").document(task.id).delete()
    }

    fun addCategory(category: Category) {
        val userId = getUserId() ?: return
        val newPosition = (_categories.value?.size ?: 0).toLong()
        val newCategory = category.copy(position = newPosition)
        db.collection("users").document(userId).collection("categories").add(newCategory)
    }

    fun updateCategory(category: Category) {
        val userId = getUserId() ?: return
        db.collection("users").document(userId).collection("categories").document(category.id).set(category)
    }

    fun updateCategoryOrder(categories: List<Category>) {
        val userId = getUserId() ?: return
        val batch = db.batch()
        categories.forEachIndexed { index, category ->
            val docRef = db.collection("users").document(userId).collection("categories").document(category.id)
            batch.update(docRef, "position", index.toLong())
        }
        batch.commit()
    }

    fun deleteCategory(category: Category) {
        val userId = getUserId() ?: return
        val batch = db.batch()
        db.collection("users").document(userId).collection("tasks")
            .whereEqualTo("categoryId", category.id)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    batch.delete(document.reference)
                }
                val categoryRef = db.collection("users").document(userId).collection("categories").document(category.id)
                batch.delete(categoryRef)
                batch.commit()
            }
    }

    fun deleteAllCompletedTasks(categoryId: String?, isFavorites: Boolean) {
        val userId = getUserId() ?: return
        val query = if (isFavorites) {
            db.collection("users").document(userId).collection("tasks")
                .whereEqualTo("favorite", true)
                .whereEqualTo("completed", true)
        } else {
            db.collection("users").document(userId).collection("tasks")
                .whereEqualTo("categoryId", categoryId)
                .whereEqualTo("completed", true)
        }

        query.get().addOnSuccessListener { documents ->
            val batch = db.batch()
            for (document in documents) {
                batch.delete(document.reference)
            }
            batch.commit()
        }
    }
}
