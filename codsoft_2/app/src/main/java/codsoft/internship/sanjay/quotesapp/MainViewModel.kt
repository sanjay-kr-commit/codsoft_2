package codsoft.internship.sanjay.quotesapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.gson.Gson

class MainViewModel : ViewModel() {

    val gson = Gson()
    lateinit var db : DB
    val listOfQuotes : MutableList<String> = mutableStateListOf()
    val listOfFavQuotes : MutableList<String> = mutableStateListOf()
    var timestamp : Long = 0
    var fetchingData : Boolean by mutableStateOf(
        false
    )
    var initializing : Boolean by mutableStateOf(
        true
    )
    var showFav : Boolean by mutableStateOf(
        false
    )
    lateinit var showToast : String.( Int ) -> Unit
    lateinit var addToFav : String.() -> Unit
    lateinit var removeFromFav : String.() -> Unit
    lateinit var shareQuote : String.() -> Unit

}