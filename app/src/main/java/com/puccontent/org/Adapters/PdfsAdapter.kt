package com.puccontent.org.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.puccontent.org.Models.PdfItem
import com.puccontent.org.R

class PdfsAdapter(val context: Context, private val listener: PdfClicked):RecyclerView.Adapter<PdfsViewHolder>() {
    private val list = ArrayList<PdfItem>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfsViewHolder {
       val view = LayoutInflater.from(context).inflate(R.layout.pdf_item,parent,false)
        val holder = PdfsViewHolder(view)
        view.setOnClickListener {
            listener.click(holder.adapterPosition)
        }
        return holder
    }

    override fun onBindViewHolder(holder: PdfsViewHolder, position: Int) {
        holder.pdfTitle.text = list[position].name
        holder.pdfTitle.isSelected = true
        if(list[position].size!=null){
            holder.pdfSize.text = list[position].size
        }
        if(listener.checkIt(holder.adapterPosition)){
            Glide.with(context).load(R.drawable.ic_baseline_delete_outline_24).into(holder.pdfStatus)
           holder.pdfStatus.setColorFilter(ContextCompat.getColor(context,R.color.red),android.graphics.PorterDuff.Mode.MULTIPLY)
        }
        else{
            Glide.with(context).load(R.drawable.download_circular_button).into(holder.pdfStatus)
           // holder.pdfStatus.setColorFilter(ContextCompat.getColor(context, R.color.red),android.graphics.PorterDuff.Mode.MULTIPLY)

        }
        if(listener.checkQuick(holder.adapterPosition)){
            holder.quickImg.visibility = View.VISIBLE
        }
        else{
            holder.quickImg.visibility = View.GONE
        }
        holder.pdfIcon.setOnClickListener {
            listener.quickAccesss(holder.adapterPosition)
        }
        holder.pdfStatus.setOnClickListener {
            listener.downloadOrDelete(holder.adapterPosition)
        }
    }
    fun updateData(lit:ArrayList<PdfItem>){
        list.clear()
        list.addAll(lit)
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
     return list.size
    }
}

class PdfsViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView) {
     val pdfTitle: TextView = itemView.findViewById(R.id.pdfName)
     val pdfStatus:ImageView = itemView.findViewById(R.id.downloadState)
     val pdfIcon:ImageView = itemView.findViewById(R.id.pdfImg)
     val quickImg:ImageView = itemView.findViewById(R.id.quickAccImg)
     val pdfSize:TextView = itemView.findViewById(R.id.pdfSize)
}
interface PdfClicked{
    fun click(position: Int)
    fun checkIt(position: Int):Boolean
    fun checkQuick(position: Int):Boolean
    fun quickAccesss(position: Int)
    fun downloadOrDelete(position: Int)
}