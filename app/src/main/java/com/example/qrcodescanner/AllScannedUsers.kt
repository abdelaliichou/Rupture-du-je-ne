package com.example.qrcodescanner

import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.util.Log
import kotlinx.coroutines.delay
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class AllScannedUsers : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: usersAdapter
    private val userList = mutableListOf<User>()
    private val client = OkHttpClient()
    lateinit var view: LinearLayout
    lateinit var todayText: TextView
    lateinit var totaleText: TextView
    lateinit var validationsText: TextView
    lateinit var unvalidationsText: TextView
    lateinit var searchInput: EditText
    lateinit var searchIMG: ImageView
    lateinit var clearIMG: ImageView
    private var searchJob: Job? = null // To manage the debounce logic
    var isSearching = false
    final  var AllUsers : List<User> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_scanned_users)

        initilisation()
        settingText()
        fetchingUsers()
        AutoSearch()
        onClicks()
        KeyboardSearchButton()

    }

    fun settingText() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH) + 1
        val day = c.get(Calendar.DAY_OF_MONTH)
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        todayText.text = "Les inscriptions pour le repas du " + day + "/" + month + "/" + year + " entre 17:15 et 18:30 :"
        totaleText.text = AllUsers.count().toString()
        val (valid, inValid) = AllUsers.partition { it.validation_date == "true" }
        validationsText.text = valid.count().toString()
        unvalidationsText.text = inValid.count().toString()
    }

    fun KeyboardSearchButton() {
        // Set the "Search" action button on the keyboard
        searchInput.imeOptions = EditorInfo.IME_ACTION_SEARCH
        searchInput.setImeActionLabel("Search", EditorInfo.IME_ACTION_SEARCH)

        // Trigger search when the "Search" button is pressed
        searchInput.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                isSearching = true
                Search(searchInput.text.trim().toString().toLowerCase())
                true
            } else {
                false
            }
        })

        // Listen for other key presses to make the isSearch variable false so the auto search can works
        searchInput.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                isSearching = false
                false
            } else {
                isSearching = false
                false
            }
        }
    }

    fun AutoSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (s?.trim().toString().isNotEmpty()) {
                    clearIMG.visibility = View.VISIBLE
                } else {
                    clearIMG.visibility = View.GONE
                }

                // Cancel any existing search jobs
                searchJob?.cancel()

                // Start a new coroutine with a 2-second delay
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    s?.let {
                        if (s.trim().isEmpty()) {
                            isSearching = false
                            fetchingUsers()
                        } else {
                            isSearching = true
                            Search(it.toString().trim().toLowerCase())
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun Search(text: String) {
        val (searchList, _) = AllUsers.partition { it.nameSurname.toLowerCase().contains(text.toLowerCase()) }
        Log.d("SEARCHINGGGGGGGGGGGGG", searchList.toString())
        refreshRecyclerView(searchList)
    }

    fun onClicks() {
        searchIMG.setOnClickListener {
            Log.d("CLICKKKKKED ON SEARCH", searchInput.text!!.toString())
            if (searchInput.text.isEmpty()) {
                Toast.makeText(view.context, "Recherche vide!", Toast.LENGTH_SHORT).show()
                isSearching = false
                fetchingUsers()
            } else {
                isSearching = true
                Search(searchInput.text!!.toString().trim().toLowerCase())
            }
        }
        clearIMG.setOnClickListener {
            searchInput.text.clear()
            hideKeyboard()
        }
        view.setOnClickListener { hideKeyboard() }
    }

    fun hideKeyboard() {
        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocus = this.currentFocus
        if (currentFocus != null) {
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    fun initilisation() {
        view = findViewById(R.id.main)
        searchInput = findViewById(R.id.searchEditText)
        searchIMG = findViewById(R.id.searchIcon)
        clearIMG = findViewById(R.id.clearicon)
        recyclerView = findViewById(R.id.recyclerView)
        todayText = findViewById(R.id.todayText)
        totaleText = findViewById(R.id.totalNumber)
        validationsText = findViewById(R.id.validationsNumber)
        unvalidationsText = findViewById(R.id.unvalidationNumber)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = usersAdapter(userList)
        recyclerView.adapter = userAdapter
    }

    fun fetchingUsers() {
        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url("https://m.oussamanh.com/api/today")
                .get()
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    Log.d("ALL USERS RAW", responseBody) // ✅ Log full response
                    val gson = Gson()
                    try {
                        val userResponse = gson.fromJson(responseBody, UserResponse::class.java)
                        // ✅ Convert API response into our User data class
                        val users = userResponse.data.map { user ->
                            User(
                                nameSurname = user.nameSurname,
                                phoneNumber = user.phoneNumber,
                                email = user.email,
                                validation_date = (user.validation_date != null).toString() // If validation_date is not null, user is validated
                            )
                        }
                        withContext(Dispatchers.Main) {
                            Log.d("ALL USERS COUNTTT", userResponse.count.toString())
                            Log.d("ALL USERS MESSSSAGE", userResponse.message)
                            Log.d("ALL USERS PROCESSED", users.toString())
                            AllUsers = users // we need this list in the search
                            refreshRecyclerView(users)
                        }
                    } catch (e: Exception) {
                        Log.e("JSON_ERROR", "Parsing error: ${e.message}")
                    }
                } else {
                    Log.e("API_ERROR", "Failed to fetch all users: ${response.code}")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun refreshRecyclerView(users: List<User>) {
        userList.clear()
        userList.addAll(users)
        userList.sortBy { user: User -> user.nameSurname }
        userAdapter.notifyDataSetChanged()
        settingText()
    }
}