package com.puccontent.org.Adapters

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.puccontent.org.Models.Donor
import com.puccontent.org.R
import com.puccontent.org.databinding.DonorItemBinding

class LeaderBoardAdapter(
    private val context: Context,
    private val list: ArrayList<Donor>,
) : RecyclerView.Adapter<LeaderBoardHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderBoardHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.donor_item, parent, false)
        return LeaderBoardHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderBoardHolder, pos: Int) {
        with(holder.binding) {
            sno.text = (pos + 1).toString()
            name.text = list[pos].name
            points.text = list[pos].points.toString()
            with(name) {
                setHorizontallyScrolling(true)
                isSingleLine = true
                marqueeRepeatLimit = -1
                ellipsize = TextUtils.TruncateAt.MARQUEE
                isSelected = true
            }
        }

        when (pos) {
            0 -> {
                holder.binding.root.setBackgroundResource(android.R.color.holo_orange_dark)
                with(holder.binding) {
                    sno.setTextColor(Color.parseColor("#FFFFFF"))
                    name.setTextColor(Color.parseColor("#FFFFFF"))
                    points.setTextColor(Color.parseColor("#FFFFFF"))
                }
            }
            1 -> {
                holder.binding.root.setBackgroundResource(R.color.teal_200)
                with(holder.binding) {
                    sno.setTextColor(Color.parseColor("#FFFFFF"))
                    name.setTextColor(Color.parseColor("#FFFFFF"))
                    points.setTextColor(Color.parseColor("#FFFFFF"))
                }
            }
            2 -> {
                holder.binding.root.setBackgroundResource(android.R.color.holo_purple)
                with(holder.binding) {
                    sno.setTextColor(Color.parseColor("#FFFFFF"))
                    name.setTextColor(Color.parseColor("#FFFFFF"))
                    points.setTextColor(Color.parseColor("#FFFFFF"))
                }
            }
            else -> {

            }
        }
    }

    override fun getItemCount() = list.size

}

class LeaderBoardHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = DonorItemBinding.bind(itemView)

}