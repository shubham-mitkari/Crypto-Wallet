package com.example.crypwaller

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.TestNet3Params
import java.io.File

class WalletManager(private val context: Context) {

    private var connectedToPeer: Boolean = false
    private val params = TestNet3Params.get()
    private val kitDir = File(context.filesDir, "wallet-kit")
    private val filePrefix = "wallet"

    lateinit var kit: WalletAppKit
    val syncProgress = MutableLiveData<Double>()

    fun setupKit(
        progressTracker: DownloadProgressTracker,
        onSetupComplete: (Wallet) -> Unit,
        onPeerConnected: (() -> Unit)? = null
    ) {

        val chainFile = File(kitDir, "$filePrefix.spvchain")
//        if (chainFile.exists()) {
//            chainFile.delete()
//            Log.i("WalletManager", "Deleted old .spvchain file – forcing full resync")
//        }
        kit = object : WalletAppKit(params, kitDir, filePrefix) {
            override fun onSetupCompleted() {
                wallet().allowSpendingUnconfirmedTransactions()
                Log.i(
                    "WalletManager",
                    "Kit setup complete. Address: ${wallet().currentReceiveAddress()}"
                )
                onSetupComplete(wallet())
            }
        }
        // **Load the real TestNet3 checkpoint** from assets
        val cpStream = context.assets.open("testnet3.checkpoints")
        kit.setCheckpoints(cpStream)

        kit.setDownloadListener(progressTracker)
        kit.setBlockingStartup(false)
        kit.setAutoSave(true)
        kit.startAsync()
        kit.awaitRunning()
        kit.peerGroup().addConnectedEventListener { peer, _ ->
            if (!connectedToPeer) {
                connectedToPeer = true
                Log.i("WalletManager", "Connected to peer: ${peer.address}")
                onPeerConnected?.invoke()  // Notify ViewModel once
            }
        }
    }

    fun getWallet(): Wallet = kit.wallet()
}