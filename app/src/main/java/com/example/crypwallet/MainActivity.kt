package com.example.crypwallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.crypwallet.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WalletViewModel
    private lateinit var binding: ActivityMainBinding

    private val api: TestnetExplorerApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://mempool.space/testnet/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TestnetExplorerApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable options menu
        setSupportActionBar(findViewById(R.id.toolbar))

        binding.main.isFocusableInTouchMode = true
        viewModel = ViewModelProvider(this)[WalletViewModel::class.java]
        viewModel.generateOrLoadWallet(force = false)

        setupListeners()
        observeViewModel()

        val trxAdapter = TransactionAdapter()
        binding.rvTransactions.adapter = trxAdapter

        viewModel.txList.observe(this) { list ->
            trxAdapter.update(list)
        }
    }

    // Create options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Handle menu item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_crypto_prices -> {
                startActivity(Intent(this, CryptoActivity::class.java))
                true
            }
            R.id.action_fetch_balance -> {
                fetchBalanceFromAPI()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setupListeners() {

        binding.main.setOnTouchListener { _, _ ->
            hideKeyboard()
            binding.main.requestFocus()
            false
        }

        binding.tvAddress.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Wallet Address", binding.tvAddress.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
            true
        }

        binding.btnGenerateWallet.setOnClickListener {
            binding.btnGenerateWallet.isEnabled = false
            viewModel.generateOrLoadWallet()
        }

        binding.btnSend.setOnClickListener {
            val address = binding.etToAddress.text.toString()
            val amount = binding.etAmount.text.toString().toDoubleOrNull()

            when {
                address.isBlank() -> binding.etToAddress.error = "Enter recipient address"
                amount == null || amount <= 0.0 -> binding.etAmount.error = "Enter valid amount"
                else -> {
                    viewModel.sendCoins(address, amount)
                    binding.etToAddress.text.clear()
                    binding.etAmount.text.clear()
                    hideKeyboard()
                }
            }
        }
    }

    private fun fetchBalanceFromAPI() {
        lifecycleScope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    api.getAddressInfo(viewModel.walletState.value!!.address)
                }

                val confirmedSats =
                    info.chain_stats.funded_txo_sum - info.chain_stats.spent_txo_sum
                val unconfirmedSats =
                    info.mempool_stats.funded_txo_sum - info.mempool_stats.spent_txo_sum
                val msg = buildString {
                    append("Confirmed: ${"%,.8f".format(confirmedSats / 1e8)} ₿")
                }
                Toast.makeText(this@MainActivity, msg, LENGTH_LONG).show()

                Log.i(
                    "Wallet",
                    "Confirmed: ${"%,.8f".format(confirmedSats / 1e8)}, Unconfirmed: ${
                        "%,.8f".format(unconfirmedSats / 1e8)
                    }"
                )
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", LENGTH_LONG).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.walletState.observe(this) { state ->
            val hasAddress = state.address.isNotBlank()
            binding.tvAddress.text = if (hasAddress) state.address else ""

            binding.ivQr.apply {
                visibility = if (hasAddress) ImageView.VISIBLE else ImageView.GONE
                if (!hasAddress) setImageDrawable(null)
            }

            binding.btnGenerateWallet.apply {
                visibility = if (hasAddress) Button.GONE else Button.VISIBLE
                isEnabled = true
            }

            binding.tvBalance.text = "₿ ${state.balance}"
            val btc = state.balance.replace("[^\\d.]".toRegex(), "").toDoubleOrNull() ?: 0.0
            binding.tvInr.text = "≈ ₹${"%,.2f".format(btc * 10078603.49)}"
        }

        viewModel.qrBitmap.observe(this) { bitmap ->
            binding.ivQr.setImageBitmap(bitmap)
        }

        viewModel.coinsReceived.observe(this) {
            Toast.makeText(this, "New coins received! Balance updated.", LENGTH_LONG).show()
        }

        viewModel.syncStatusText.observe(this) { status ->
            binding.tvSyncStatus.text = status
            binding.tvSyncStatus.visibility = TextView.VISIBLE
        }
    }

    private fun hideKeyboard() {
        currentFocus?.let { view ->
            val imm =
                getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
//    tb1qerzrlxcfu24davlur5sqmgzzgsal6wusda40er
