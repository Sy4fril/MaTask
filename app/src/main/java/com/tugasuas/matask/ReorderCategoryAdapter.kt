package com.tugasuas.matask

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tugasuas.matask.databinding.ItemReorderCategoryBinding
import java.util.Collections

class ReorderCategoryAdapter(private val categories: MutableList<Category>) : RecyclerView.Adapter<ReorderCategoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReorderCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        Collections.swap(categories, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getCategories(): List<Category> = categories

    inner class ViewHolder(private val binding: ItemReorderCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            binding.tvCategoryName.text = category.name
        }
    }
}
