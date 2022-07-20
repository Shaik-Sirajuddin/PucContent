package com.puccontent.org.Adapters

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.puccontent.org.Models.PdfItem
import com.puccontent.org.R

class PdfsAdapter(
    private val context: Context,
    private val listener: PdfClicked,
) :
    RecyclerView.Adapter<PdfsViewHolder>() {
    private val list = ArrayList<PdfItem>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfsViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.pdf_item, parent, false)
        val holder = PdfsViewHolder(view)
        view.setOnClickListener {
            listener.click(holder.adapterPosition)
        }
        return holder
    }

    override fun onBindViewHolder(holder: PdfsViewHolder, position: Int) {
        holder.pdfTitle.text = list[position].name
        holder.pdfTitle.isSelected = true
        Glide.with(context)
            .load(R.drawable.pdf1)
            .into(holder.pdfIcon)

        if (list[position].size != null) {
            holder.pdfSize.text = list[position].size
        }
        if (listener.checkIt(holder.absoluteAdapterPosition)) {
            Glide.with(context).load(R.drawable.openwith)
                .into(holder.pdfStatus)
        } else {
            Glide.with(context).load(R.drawable.download_icon).into(holder.pdfStatus)
        }
        if (listener.checkQuick(holder.absoluteAdapterPosition)) {
            holder.quickImg.visibility = View.VISIBLE
        } else {
            holder.quickImg.visibility = View.GONE
        }
        holder.pdfIcon.setOnClickListener {
            listener.quickAccess(holder.absoluteAdapterPosition)
        }
        holder.pdfStatus.setOnClickListener {
            listener.downloadOrOpen(holder.absoluteAdapterPosition)
        }
//        holder.menu.setOnClickListener {
//            val popUpMenu = PopupMenu(context, holder.menu)
//            popUpMenu.inflate(R.menu.pdf_item_menu)
//            popUpMenu.setOnMenuItemClickListener {
//                when (it.itemId) {
//                    R.id.share -> {
//                        listener.shareFile(position)
//                        return@setOnMenuItemClickListener true
//                    }
//                    R.id.extract -> {
//                        listener.extractPdf(position)
//                        return@setOnMenuItemClickListener true
//                    }
//                    R.id.open -> {
//                        listener.openWith(position)
//                        return@setOnMenuItemClickListener true
//                    }
//                }
//                true
//            }
//            popUpMenu.show()
//        }
        with(holder.pdfTitle) {
            setHorizontallyScrolling(true);
            maxLines = 1;
            isSingleLine = true
            marqueeRepeatLimit = -1
            ellipsize = TextUtils.TruncateAt.MARQUEE;
            isSelected = true
        }
    }

    fun updateData(lit: ArrayList<PdfItem>) {
        list.clear()
        list.addAll(lit)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

class PdfsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val pdfTitle: TextView = itemView.findViewById(R.id.pdfName)
    val pdfStatus: ImageView = itemView.findViewById(R.id.downloadState)
    val pdfIcon: ImageView = itemView.findViewById(R.id.pdfImg)
    val quickImg: ImageView = itemView.findViewById(R.id.quickAccImg)
    val pdfSize: TextView = itemView.findViewById(R.id.pdfSize)
}

interface PdfClicked {
    fun click(position: Int)
    fun checkIt(position: Int): Boolean
    fun checkQuick(position: Int): Boolean
    fun quickAccess(position: Int)
    fun downloadOrOpen(position: Int)
    fun openWith(position: Int)
}