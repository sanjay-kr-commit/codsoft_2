package codsoft.internship.sanjay.quotesapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codsoft.internship.sanjay.quotesapp.ui.theme.QuotesAppCODSOFTTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainViewModel by viewModels<MainViewModel>()
        mainViewModel.initialize()
        enableEdgeToEdge()
        setContent {
            QuotesAppCODSOFTTheme {
                AnimatedVisibility(visible = mainViewModel.initializing ) {
                    Column ( Modifier.fillMaxSize() ,
                      horizontalAlignment = Alignment.CenterHorizontally ,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height( 10.dp ) )
                        Text(text = "Initializing" )
                    }
                }
                AnimatedVisibility(visible = ! mainViewModel.initializing ) {
                    HomeScreen(
                        mainViewModel
                    )
                }
            }
        }
    }


    private fun MainViewModel.initialize() = CoroutineScope(Dispatchers.Main).launch {
        db = DB(this@MainActivity)

        try {
            with(db) {
                gson.fromJson(
                    KEYS.LIST_OF_QUOTES.name.value,
                    List::class.java
                ).forEach { listOfQuotes += it.toString() }
            }
        } catch (e: Exception) {
            Log.e("ListParseError", e.stackTraceToString())
        }

        try {
            with(db) {
                timestamp = KEYS.TIMESTAMP.name.value.toLong()
            }
        } catch (e: Exception) {
            Log.e("timeStampError", e.stackTraceToString())
            try {
                with(db) {
                    KEYS.TIMESTAMP.name update System.currentTimeMillis().toString()
                }
            } catch (e: Exception) {
                Log.e("timeStampUpdateError", e.stackTraceToString())
            }
        }

        try {
            with( db ) {
                listOfFavQuotes += gson.fromJson(
                    KEYS.LIST_OF_FAV_QUOTES.name.value ,
                    List::class.java
                ).map { it.toString() }
            }
        } catch ( e : Exception ) {
            Log.e( "FailedToExtractFavQuotes" , e.stackTraceToString() )
        }

        showToast = {
            Toast.makeText(
                this@MainActivity,
                this,
                it
            ).show()
        }

        addToFav = {
            listOfFavQuotes += this
            with( db ) {
                KEYS.LIST_OF_FAV_QUOTES.name update gson
                    .toJson( listOfFavQuotes )
            }
        }

        removeFromFav = {
            listOfFavQuotes -= this
            with( db ) {
                KEYS.LIST_OF_FAV_QUOTES.name update gson
                    .toJson( listOfFavQuotes )
            }
        }

        shareQuote = {
            val message = this
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT,
                    message
                )
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        fetchingData = ((timestamp + (24 * 60 * 60 * 1000) < System.currentTimeMillis()) || listOfQuotes.isEmpty() || timestamp == 0L)

        // this should always stay at bottom of this function body
        initializing = false

    }

}
