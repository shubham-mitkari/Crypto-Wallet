package com.example.crypwallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CryptoAdapter(private var items: MutableList<Pair<String, Double>>)
    : RecyclerView.Adapter<CryptoAdapter.CryptoVH>() {

    class CryptoVH(item: View) : RecyclerView.ViewHolder(item) {
        val name: TextView = item.findViewById(android.R.id.text1)
        val price: TextView = item.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CryptoVH(
            LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
        )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: CryptoVH, position: Int) {
        val (coin, price) = items[position]
        holder.name.text = coin.replaceFirstChar { it.uppercase() }
        holder.price.text = "₹${"%,.2f".format(price)}"
    }

    fun update(newItems: MutableList<Pair<String, Double>>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun addItem(item: Pair<String, Double>) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }
}
