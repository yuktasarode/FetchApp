package com.example.fetchapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.fetchapp.ui.theme.FetchAppTheme
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

import androidx.compose.ui.Alignment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            FetchAppTheme {

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting()

                }
            }
        }
    }
}

@Serializable
data class Item(val id: Int, val listId: Int, val name: String?)

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val fetchedData = remember { mutableStateOf<Map<Int, List<Item>>>(emptyMap()) }


    LaunchedEffect(key1 = Unit) {
        try {
            val responseData = fetchData()
            val items = Json.decodeFromString<List<Item>>(responseData)
            val filteredItems = items.filter { !it.name.isNullOrBlank() }
            val filteredItemsLength = filteredItems.size


            // Group items by listId
            val groupedItems = filteredItems.groupBy { it.listId }

            // Sort each group by name
            val sortedGroups = groupedItems.mapValues { (_, items) ->
                items.sortedBy { it.name }
            }

            val sortedKeys = sortedGroups.keys.sorted()
            val sortedMap = sortedKeys.map { it to sortedGroups.getValue(it) }.toMap()

            fetchedData.value = sortedMap
            val length = fetchedData.value.size
            Log.d("FetchedDataLength", "Number of groups: $length")
            Log.d("FilteredDataLength", "Lists: $filteredItemsLength")

        } catch (e: Exception) {
            Log.e("FetchError", "Error fetching data: ${e.message}", e)
        }
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState()).fillMaxSize(),horizontalAlignment = Alignment.CenterHorizontally) {
        fetchedData.value.forEach { (listId, items) ->
            Text(
                text = "- - - - - - - - - - - List ID: $listId - - - - - - - - - - - -",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                items.forEach { item ->
                    Text(
                        text = item.name ?: "",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

}



suspend fun fetchData(): String = withContext(Dispatchers.IO) {
    val url = "https://fetch-hiring.s3.amazonaws.com/hiring.json"
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000

    try {
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            return@withContext connection.inputStream.bufferedReader().use {
                it.readText()
            }

        } else {
            throw RuntimeException("HTTP error ${connection.responseCode}")
        }
    } finally {
        connection.disconnect()
    }
}

