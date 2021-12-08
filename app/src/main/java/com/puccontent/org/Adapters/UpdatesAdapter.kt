package com.puccontent.org.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.puccontent.org.Models.Update
import com.puccontent.org.R
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UpdatesAdapter(
    val context: Context,
    private val listener: UpdateClicked,
    private val isUpdate: Boolean,
) : RecyclerView.Adapter<UpdatesViewHolder>() {
    private val list = ArrayList<Update>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdatesViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recent_updates_item, parent, false)
        val holder = UpdatesViewHolder(view)
        if (!isUpdate) {
            view.setOnClickListener {
                listener.updateClicked(holder.adapterPosition)
            }
        } else {
            view.setOnClickListener {
                listener.recentUpdateClicked(holder.adapterPosition)
            }
        }
        return holder
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: UpdatesViewHolder, position: Int) {
        try {
            holder.name.text = list[position].name
            if (isUpdate) {
                holder.image.visibility = View.GONE
                holder.date.visibility = View.VISIBLE
                val sdf = SimpleDateFormat("dd-MMM-yyyy")
                val ad = list[position].date?.let { Date(it) }
                if (ad != null) {
                    holder.date.text = sdf.format(ad)
                }
            } else {
                holder.image.visibility = View.VISIBLE
                holder.date.visibility = View.GONE
                holder.image.setOnClickListener {
                    listener.remove(holder.adapterPosition)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeItem(position: Int) {
        list.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateData(l: ArrayList<Update>) {
        list.clear()
        list.addAll(l)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

class UpdatesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name: TextView = itemView.findViewById(R.id.recentUpdate)
    val date: TextView = itemView.findViewById(R.id.rDate)
    val image: ImageView = itemView.findViewById(R.id.pImg)
}

interface UpdateClicked {
    fun updateClicked(position: Int)
    fun remove(position: Int)
    fun recentUpdateClicked(position: Int)
}