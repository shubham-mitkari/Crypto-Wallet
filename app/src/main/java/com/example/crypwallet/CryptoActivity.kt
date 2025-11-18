package com.example.crypwallet

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CryptoActivity : AppCompatActivity() {
    private lateinit var btnViewAll: Button
    private lateinit var vm: CryptoViewModel
    private lateinit var adapter: CryptoAdapter
    private lateinit var etNew: EditText
    private lateinit var btnLoad: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crypto)

        vm = ViewModelProvider(this)[CryptoViewModel::class.java]
        adapter = CryptoAdapter(mutableListOf())

        etNew = findViewById(R.id.new_crypto_edit_text)
        btnLoad = findViewById(R.id.btn_load)
        btnViewAll = findViewById<Button>(R.id.btn_view_all)
        findViewById<RecyclerView>(R.id.rv_crypto).apply {
            layoutManager = LinearLayoutManager(this@CryptoActivity)
            adapter = this@CryptoActivity.adapter
        }

        btnLoad.setOnClickListener {
            val name = etNew.text.toString().trim().lowercase()
            if (name.isNotEmpty()) {
                vm.fetchSinglePrice(name)
                etNew.text.clear()
            }
        }
        vm.singlePrice.observe(this) { (coin, price) ->
            adapter.addItem(coin to price)
        }

        // Pre-load popular coins
        vm.fetchPrices()
        vm.prices.observe(this) { map ->
            map.forEach { (coin, price) -> adapter.addItem(coin to price) }
        }
        btnViewAll.setOnClickListener {
            vm.fetchAllPrices()
        }

        vm.allPrices.observe(this) { list ->
            list.forEach { adapter.addItem(it) }
        }
    }
}