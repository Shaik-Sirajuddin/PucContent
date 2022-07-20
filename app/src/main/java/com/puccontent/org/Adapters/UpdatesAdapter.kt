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
import com.puccontent.org.activities.MainActivity
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UpdatesAdapter(
    val context: Context,
    private val listener: MainActivity
) : RecyclerView.Adapter<UpdatesViewHolder>() {
    private val list = ArrayList<Update>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdatesViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recent_updates_item, parent, false)
        val holder = UpdatesViewHolder(view)

        view.setOnClickListener {
            listener.pdfClicked(holder.absoluteAdapterPosition)
        }

        return holder
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: UpdatesViewHolder, position: Int) {
        try {
            holder.name.text = list[position].name
            holder.image.setOnClickListener {
                listener.openWith(holder.absoluteAdapterPosition)
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
    val image: ImageView = itemView.findViewById(R.id.openWith)
}

interface UpdateClicked {
    fun pdfClicked(position: Int)
    fun openWith(position: Int)
}