package com.tugasuas.matask

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tugasuas.matask.databinding.ActivityReorderCategoriesBinding

class ReorderCategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReorderCategoriesBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var adapter: ReorderCategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReorderCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        viewModel.categories.observe(this) { categories ->
            if (::adapter.isInitialized) {
                // Do nothing if adapter is already initialized
            } else {
                adapter = ReorderCategoryAdapter(categories.toMutableList())
                binding.rvCategories.adapter = adapter
                binding.rvCategories.layoutManager = LinearLayoutManager(this)

                val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                        adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                        return true
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
                })
                itemTouchHelper.attachToRecyclerView(binding.rvCategories)
            }
        }

        viewModel.getCategories()
    }

    override fun onPause() {
        super.onPause()
        if (::adapter.isInitialized) {
            viewModel.updateCategoryOrder(adapter.getCategories())
        }
    }
}
