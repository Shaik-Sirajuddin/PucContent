package com.puccontent.org.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.puccontent.org.Models.Subject
import com.puccontent.org.R

class SubjectsAdapter(private val context: Context, private val list:ArrayList<Subject>,private val listner:SubjectClicked):RecyclerView.Adapter<SubjectsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectsViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.any_item,parent,false)
        val holder = SubjectsViewHolder(view)
        view.setOnClickListener {
            listner.subClicked(holder.adapterPosition)
        }
        return holder
    }

    override fun onBindViewHolder(holder: SubjectsViewHolder, position: Int) {
        Glide.with(context).load(list[position].image).into(holder.image)
        holder.name.text = list[position].name
    }

    override fun getItemCount(): Int {
      return list.size
    }
}

class SubjectsViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView) {
   val image: ImageView = itemView.findViewById(R.id.subImage)
    val name: TextView = itemView.findViewById(R.id.subName)
}
interface SubjectClicked{
    fun subClicked(position: Int)
}
