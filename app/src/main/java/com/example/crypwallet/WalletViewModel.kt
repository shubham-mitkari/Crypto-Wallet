package com.example.crypwallet

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.example.crypwaller.WalletManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.wallet.Wallet
import java.util.Date

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val walletManager = WalletManager(application)

    private val _walletState = MutableLiveData<WalletState>()
    val walletState: LiveData<WalletState> = _walletState

    private val _qrBitmap = MutableLiveData<Bitmap>()
    val qrBitmap: LiveData<Bitmap> = _qrBitmap

    private val _coinsReceived = MutableLiveData<Coin>()
    val coinsReceived: LiveData<Coin> = _coinsReceived

    //    val syncProgress: LiveData<Double> get() = walletManager.syncProgress
    private val _syncProgress = MutableLiveData<Double>()
    val syncProgress: LiveData<Double> = _syncProgress

    private var isWalletInitialized = false

    private val _syncStatusText = MutableLiveData<String>()
    val syncStatusText: LiveData<String> = _syncStatusText


    private val _txList = MutableLiveData<List<TransactionItem>>()
    val txList: LiveData<List<TransactionItem>> = _txList

    val progressTracker = object : DownloadProgressTracker() {
        override fun startDownload(blocks: Int) {
            Log.i("Wallet SyncDebug", "Sync starting: $blocks blocks to download")
            _syncStatusText.postValue("Syncing... 0.00%")
        }

        override fun progress(pct: Double, blocksSoFar: Int, date: Date?) {
            Log.i("Wallet SyncDebug", "Sync progress: %.2f%%".format(pct))
            _syncProgress.postValue(pct)
            _syncStatusText.postValue("Syncing... %.2f%%".format(pct))
        }

        override fun doneDownload() {
            Log.i("Wallet SyncDebug", "Sync completed.")
            _syncProgress.postValue(1.0)
            _syncStatusText.postValue("Sync completed")
            val wallet = walletManager.getWallet()
            updateWalletState(wallet)
        }
    }

    init {
        generateOrLoadWallet(force = false)
    }

    fun generateOrLoadWallet(force: Boolean = false) {
//        if (isWalletInitialized && !force) return
        isWalletInitialized = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                walletManager.setupKit(
                    progressTracker = progressTracker,
                    onSetupComplete = { wallet ->
                        wallet.allowSpendingUnconfirmedTransactions()

                        Log.i("Wallet", "My address = ${wallet.currentReceiveAddress()}")

                        wallet.addCoinsReceivedEventListener { _, tx, _, newBalance ->
                            Log.i("WalletViewModel", "Coins received: $newBalance")
                            _coinsReceived.postValue(newBalance)
                            updateWalletState(wallet)
                        }

                        updateWalletState(wallet)
                    }, onPeerConnected = {
                        // ✅ Only once: first time peer connected
                        _syncStatusText.postValue("Connected to peer. Waiting for sync...")
                    }
                )
            } catch (e: Exception) {
                isWalletInitialized = false
                Log.e("WalletViewModel", "Wallet setup failed", e)
            }
        }
    }


    private fun updateWalletState(wallet: Wallet) {
        val address =
            findFirstUsedReceiveAddress(wallet) ?: wallet.currentReceiveAddress().toString()
//        val address = wallet.currentReceiveAddress().toString()
        val estimated = wallet.getBalance(Wallet.BalanceType.ESTIMATED)
        val balance = String.format("%.8f BTC", estimated.toPlainString().toDouble())

        Log.i("WalletVM", "Address: $address | Balance: $balance")
        wallet.transactionsByTime.forEach {
            Log.i(
                "WalletTx",
                "TX: ${it.txId} | Value: ${it.getValueSentToMe(wallet)} | Confidence: ${it.confidence.confidenceType}"
            )
        }

        _walletState.postValue(WalletState(address, balance))
        _qrBitmap.postValue(generateQRCodeBitmap(address))

        if (_syncStatusText.value?.contains("Syncing") == true &&
            wallet.isConsistent && wallet.lastBlockSeenHeight > 0
        ) {
            _syncStatusText.postValue("Wallet ready ✅")
        }
        loadTxHistory(wallet)
    }

    private fun generateQRCodeBitmap(data: String): Bitmap {
        val context = getApplication<Application>()
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val isDarkTheme = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val foregroundColor =
            if (isDarkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        val transparent = android.graphics.Color.TRANSPARENT

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) foregroundColor else transparent)
            }
        }

        return bitmap
    }

    fun sendCoins(toAddress: String, amountBtc: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wallet = walletManager.getWallet()
                val address = Address.fromString(wallet.params, toAddress)
                val amount = Coin.parseCoin(amountBtc.toString())
                val sendResult = wallet.sendCoins(walletManager.kit.peerGroup(), address, amount)

                sendResult.broadcastComplete.addListener({
                    updateWalletState(wallet)
                }, ContextCompat.getMainExecutor(getApplication()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun findFirstUsedReceiveAddress(wallet: Wallet): String? {
        return wallet.issuedReceiveAddresses.firstOrNull { address ->
            // If any transaction sends value to this address
            wallet.transactionsByTime.any { tx ->
                tx.outputs.any { output ->
                    output.scriptPubKey.getToAddress(wallet.params).toString() == address.toString()
                }
            }
        }?.toString()
    }


    private fun loadTxHistory(wallet: Wallet) {
        val ourAddrs = wallet.issuedReceiveAddresses.map { it.toString() }.toSet()

        val txItems = wallet.getTransactions(true).map { tx ->
            val net: Coin = tx.getValue(wallet)
            val type = if (net > Coin.ZERO) TxType.RECEIVED else TxType.SENT
            val prefix = if (net > Coin.ZERO) "+ " else " "
            val amountStr = prefix + net.toFriendlyString()

            val counterparty = if (type == TxType.RECEIVED) {
                tx.outputs
                    .map { it.scriptPubKey.getToAddress(wallet.params).toString() }
                    .firstOrNull { it !in ourAddrs } ?: "unknown"
            } else {
                tx.outputs
                    .map { it.scriptPubKey.getToAddress(wallet.params).toString() }
                    .firstOrNull { it !in ourAddrs } ?: "change"
            }
            TransactionItem(
                type = type,
                addressOrHash = tx.txId.toString(),
                timestamp = tx.updateTime.time,
                amount = amountStr,
                counterparty = counterparty
            )
        }
            .sortedByDescending { it.timestamp }

        _txList.postValue(txItems)
    }
}
