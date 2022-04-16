package com.pang.notetoself

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pang.notetoself.databinding.ActivityMainBinding
import com.pang.notetoself.utils.Utils
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var mSerializer: JSONSerializer? = null
    private var noteList: ArrayList<Note>? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: NoteAdapter? = null

    private var showDividers: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            val dialog = DialogNewNote(adapter!!)
            dialog.show(supportFragmentManager, "123")
//            adapter!!.notifyDataSetChanged()
        }

        mSerializer = JSONSerializer("NoteToSelf.json", applicationContext)

        try {
            noteList = mSerializer!!.load()
        } catch (e: Exception){
            noteList = ArrayList()
            Log.e("Error loading notes: ", "", e)
        }

        noteList!!.sortWith(compareBy({ note -> note.done }, { note -> note.d_time }))

        recyclerView = findViewById(R.id.recyclerView)

        adapter = NoteAdapter(this, noteList!!)

        val layoutManager = LinearLayoutManager(applicationContext)

        recyclerView!!.layoutManager = layoutManager
        recyclerView!!.itemAnimator = DefaultItemAnimator()

//        Toast.makeText(this, "onCreate called, showDividers is $showDividers", Toast.LENGTH_SHORT).show()

        Utils.setApplicationContext(applicationContext)
        createNotificationChannel()

        var builder = NotificationCompat.Builder(this, Utils.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hello world!")
            .setContentText(("This is a good day!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(Utils.getNotifyID(), builder.build())
        }


        recyclerView!!.adapter = adapter

    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(Utils.CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun deleteNote(n: Note) {
        noteList!!.remove(n)
        noteList!!.sortBy { note -> note.d_time }
        adapter!!.notifyDataSetChanged()
    }

    fun updateNote() {
        noteList!!.sortWith(compareBy({ note -> note.done }, { note -> note.d_time }))
        adapter!!.notifyDataSetChanged()
    }

    fun createNewNote(n: Note) {
        noteList!!.add(n)
        noteList!!.sortWith(compareBy({ note -> note.done }, { note -> note.d_time }))
        adapter!!.notifyDataSetChanged()
    }

    fun showNote(noteToShow: Int) {
        val dialog = DialogShowNote(adapter!!)

        dialog.sendNoteSelected(noteList!![noteToShow])
        dialog.show(supportFragmentManager, "")
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("Note to self", Context.MODE_PRIVATE)

        showDividers = prefs.getBoolean("dividers", true)

//        Toast.makeText(this, "onResume called, showDividers is $showDividers", Toast.LENGTH_SHORT).show()

        if(showDividers)
            recyclerView!!.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        else
            if(recyclerView!!.itemDecorationCount > 0)
                recyclerView!!.removeItemDecorationAt(0)
    }

    private fun saveNotes() {
        try {
            mSerializer!!.save(this.noteList!!)
        } catch (e: Exception) {
            Log.e("Error Saving Notes", "", e)
        }
    }

    override fun onPause() {
        super.onPause()

        this.saveNotes()
    }

}