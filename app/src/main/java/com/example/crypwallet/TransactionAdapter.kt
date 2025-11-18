package com.example.crypwallet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.crypwallet.databinding.ItemTransactionBinding
import java.text.DateFormat
import java.util.Date

class TransactionAdapter(
    private var items: List<TransactionItem> = listOf()
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionItem) {
            // Use custom colors that match your theme
            val colorReceived = ContextCompat.getColor(binding.root.context, R.color.accent)
            val colorSent = ContextCompat.getColor(binding.root.context, R.color.lavender)

            // Set icon
            binding.ivType.setImageResource(
                if (item.type == TxType.RECEIVED)
                    R.drawable.ic_received
                else
                    R.drawable.ic_sent
            )

            // Color the icon
            binding.ivType.setColorFilter(
                if (item.type == TxType.RECEIVED) colorReceived else colorSent,
                android.graphics.PorterDuff.Mode.SRC_IN
            )

            // Set text values
            binding.tvAddressOrHash.text = item.addressOrHash
            binding.tvTime.text = DateFormat.getDateTimeInstance().format(Date(item.timestamp))
            binding.tvAmount.text = item.amount

            // Color the amount
            binding.tvAmount.setTextColor(
                if (item.type == TxType.RECEIVED) colorReceived else colorSent
            )

            // Set counterparty with emoji
            binding.tvCounterparty.text =
                if (item.type == TxType.RECEIVED)
                    "↓ From: ${item.counterparty}"
                else
                    "↑ To: ${item.counterparty}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bind = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(bind)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun update(list: List<TransactionItem>) {
        items = list
        notifyDataSetChanged()
    }
}