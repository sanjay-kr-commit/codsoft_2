package codsoft.internship.sanjay.quotesapp

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel
) = mainViewModel.run {

    Box (
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp) ,
        contentAlignment = Alignment.Center
    ) {

        AnimatedVisibility(visible = fetchingData ) {
            FetchingQuotes()
        }
        AnimatedVisibility(visible = !fetchingData && showFav ) {
            QuotesScreen(
                listOfFavQuotes
            )
        }
        AnimatedVisibility(visible = !fetchingData && !showFav ) {
            QuotesScreen(
                listOfQuotes
            )
        }

    }
}

@Composable
fun MainViewModel.QuotesScreen(
    selectedList: MutableList<String>
) {

    var index by remember {
        mutableIntStateOf( 0 )
    }

    var job : Job? = remember {
        null
    }
    var gx by remember {
        mutableFloatStateOf( 0f )
   }
    var gy by remember {
        mutableFloatStateOf( 0f )
    }

    var isBookmarked by remember {
        mutableStateOf(
            if ( index > -1 && index < selectedList.size ) listOfFavQuotes.contains( selectedList[index] )
            else false
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
        ,
        contentAlignment = Alignment.Center ) {
        if ( selectedList.isEmpty() ) Text(text = "List Empty" )
        else Text(text = selectedList[index] )
    }

    Row(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                job?.let {
                    gx += dragAmount.x
                    gy += dragAmount.y
                } ?: run {
                    job = CoroutineScope(Dispatchers.Main).launch {
                        val (x, y) = dragAmount
                        gx = x
                        gy = y
                        delay(200)
                        Log.d("debug", "x = $x , gx = $gx")
                        Log.d("debug", "previous Index : $index")
                        when {
                            gx < x -> {
                                index = (index + 1) % selectedList.size
                            }

                            gx > x -> {
                                index = if (index == 0) (selectedList.size - 1) else index - 1
                            }
                        }
                        Log.d("debug", "new Index : $index")
                        job = null
                    }
                }
            }
        }
        , horizontalArrangement = Arrangement.SpaceEvenly ,
        verticalAlignment = Alignment.Bottom
    ) {

        Icon(imageVector = Icons.Filled.Refresh , contentDescription = "Force Refresh" ,
            modifier = Modifier.clickable {
                fetchingData = true
            }
        )

        Icon(
            imageVector = if ( isBookmarked ) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
            , contentDescription = "" ,
            modifier = Modifier
                .clickable {
                    if ( index > -1 && index < selectedList.size ) {
                        if (!isBookmarked) selectedList[index].addToFav()
                        else selectedList[index].removeFromFav()
                        isBookmarked = !isBookmarked
                    } else "List Is Empty".showToast( Toast.LENGTH_SHORT )
                }
        )

        Text(text =  "${index+1} / ${selectedList.size}" )

        Icon(
            imageVector = Icons.Filled.Share
            , contentDescription = "" ,
            modifier = Modifier
                .clickable {

                   if ( index > -1 && index < selectedList.size ) selectedList[index].shareQuote()
                   else "List Is Empty".showToast( Toast.LENGTH_SHORT )
                }
        )

        Icon(
            imageVector =
                if ( selectedList == listOfQuotes ) Icons.Filled.Menu
                else Icons.Filled.Star
            , contentDescription = "" ,
            modifier = Modifier
                .clickable {
                    showFav = !showFav
                }
        )

    }

    LaunchedEffect(key1 = index ) {
        isBookmarked =  if ( index > -1 && index < selectedList.size ) listOfFavQuotes.contains( selectedList[index] )
        else false
    }

}

@Composable
fun MainViewModel.FetchingQuotes() {

    val message = remember {
        mutableStateOf( "Fetching Quotes" )
    }
    val job : MutableState<Job?> = remember {
        mutableStateOf( null )
    }

    Column ( modifier = Modifier.fillMaxSize() ,
        verticalArrangement = Arrangement.Center ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height( 10.dp ) )
        Text(text = message.value )
    }

    LaunchedEffect(key1 = null ) {
        fetchData(
            message , job
        )
    }

    DisposableEffect(key1 = null ) {
        onDispose {
            job.value?.cancel()
        }
    }

}

private suspend inline fun MainViewModel.fetchData(
    message : MutableState<String> ,
    job : MutableState<Job?>
) {
    if (job.value == null) job.value = CoroutineScope(Dispatchers.IO).launch {
        listOfQuotes.clear()

        try {
            val quoteCount = 100

            message.value = "Fetching Data"
            val json: String = Request.Builder()
                .url("https://www.reddit.com/r/showerthoughts/top.json?sort=top&t=all&limit=$quoteCount")
                .get()
                .build()
                .let { request ->
                    OkHttpClient()
                        .newCall(request)
                        .execute().body.string()
                }

            message.value = "Extracting Quotes"

            Regex("\"title\" ?: ?\"[^=\"]*\" ?,")
                .findAll(json)
                .iterator()
                .forEach {
                    try {
                        listOfQuotes += it.value
                            .let {
                                it.substring(
                                    0,
                                    it.lastIndexOf("\"")
                                ).let {
                                    it.substring(
                                        it.lastIndexOf("\"") + 1
                                    )
                                }
                            }
                    } catch (e: Exception) {
                        Log.e("FailedToExtractString", "${it.value}\n${e.stackTraceToString()}")
                    }
                }

            message.value = "Syncing With Database"

        } catch (e: Exception) {
            Log.d("ErrorFetchingData", e.stackTraceToString())
            message.value = e.stackTraceToString()
            delay(2000)
        }

        try {
            with(db) {
                if ( listOfQuotes.isNotEmpty() ) {
                    KEYS.TIMESTAMP.name update System.currentTimeMillis().toString()
                    KEYS.LIST_OF_QUOTES.name update gson.toJson(listOfQuotes)
                    showFav = false
                } else gson.fromJson(
                    KEYS.LIST_OF_QUOTES.name.value ,
                    List::class.java
                ).forEach { listOfQuotes += it.toString() }
            }
        } catch (e: Exception) {
            Log.e("DATABASE_SYNC_FAILED", e.stackTraceToString())
            "Local Sync Failed".showToast(Toast.LENGTH_SHORT)
        }

        message.value = "Done"

        job.value = null
        fetchingData = false
    }
}