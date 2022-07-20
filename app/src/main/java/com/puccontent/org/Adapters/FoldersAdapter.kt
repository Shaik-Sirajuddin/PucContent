package com.puccontent.org.Adapters

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.puccontent.org.R
import com.puccontent.org.databinding.ChapterItemBinding

class FoldersAdapter(
    private val context: Context,
    private val list: ArrayList<String>,
    val onClick: (pos: Int) -> Unit,
) : RecyclerView.Adapter<FoldersHolder>() {
    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoldersHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.chapter_item, parent, false)
        val holder = FoldersHolder(view)
        view.setOnClickListener {
            onClick(holder.absoluteAdapterPosition)
        }
        return holder
    }

    override fun onBindViewHolder(holder: FoldersHolder, position: Int) {
        with(holder.binding) {
            chapterName.text = list[position]
            with(chapterName) {
                setHorizontallyScrolling(true);
                maxLines =2
                marqueeRepeatLimit = -1
                ellipsize = TextUtils.TruncateAt.MARQUEE;
                isSelected = true
            }
        }
    }
}

class FoldersHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ChapterItemBinding.bind(itemView)
}