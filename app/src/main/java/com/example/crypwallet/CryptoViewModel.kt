package com.example.crypwallet

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CryptoViewModel : ViewModel() {
    private val api: CryptoApi = Retrofit.Builder()
        .baseUrl("https://api.coingecko.com/api/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { level =
                    HttpLoggingInterceptor.Level.BODY
                })
                .build()
        )
        .build()
        .create(CryptoApi::class.java)

    val prices = MutableLiveData<Map<String, Double>>()
    val singlePrice = MutableLiveData<Pair<String, Double>>()

    fun fetchPrices() = viewModelScope.launch {
        try {
            val result = api.getPrices("bitcoin,ethereum,binancecoin")
            prices.postValue(result.mapValues { it.value["inr"] ?: 0.0 })
        } catch (e: Exception) {
            Log.e("Crypto", "Error fetching prices", e)
        }
    }
    fun fetchSinglePrice(coin: String) = viewModelScope.launch {
        try {
            val result = api.getPrices(coin, "inr")
            val price = result[coin]?.get("inr") ?: 0.0
            singlePrice.postValue(coin to price)
        } catch(e: Exception) { /* handle error */ }
    }
    val allPrices = MutableLiveData<List<Pair<String, Double>>>()

    fun fetchAllPrices() = viewModelScope.launch {
        try {
            val response = api.getMarketCoins(vsCurrency = "inr", perPage = 50, page = 1)
            val list = response.map {
                it.id to it.current_price
            }
            allPrices.postValue(list)
        } catch (e: Exception) {
            Log.e("CryptoVM", "fetchAllPrices failed", e)
        }
    }
}