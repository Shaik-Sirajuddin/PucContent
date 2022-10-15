package com.puccontent.org.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.puccontent.org.models.Link
import com.puccontent.org.R

class LinksAdapter(val context: Context, val list: ArrayList<Link> , val onClick : (position:Int) -> Unit) :
    RecyclerView.Adapter<LinksHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinksHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.link_item, parent, false)
        val holder = LinksHolder(view)
        view.setOnClickListener {
            onClick(holder.absoluteAdapterPosition)
        }
        return holder
    }

    override fun onBindViewHolder(holder: LinksHolder, position: Int) {
        holder.name.text = list[position].linkName
    }
    override fun getItemCount(): Int = list.size
}

class LinksHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name: TextView = itemView.findViewById(R.id.name)
}
