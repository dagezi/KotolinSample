package com.example.kotlinsample.app

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.UI
import org.jetbrains.anko.textView
import retrofit.RestAdapter
import retrofit.converter.GsonConverter
import retrofit.http.GET
import retrofit.http.Query
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers.mainThread

data class GitHubRepository(
        val id: Int,
        val fullName: String)

data class GitHubSubject(
        val title: String,
        val type: String)

data class GitHubNotification(
        val id: String,
        val repository: GitHubRepository,
        val subject: GitHubSubject)

interface GitHubNotificationService {
    @GET("/notifications")
    fun notifications(@Query("access_token") accessToken: String ) : Observable<List<GitHubNotification>>
}

public class MainActivity : Activity() {

    var miscText: TextView? = null
    var button: Button? = null
    var notificationList: ListView? = null

    var githubSubscription: Subscription? = null

    val apiGson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

    val service : GitHubNotificationService = RestAdapter.Builder()
            .setEndpoint("https://api.github.com")
            .setConverter(GsonConverter(apiGson))
            .build()
            .create(GitHubNotificationService::class.java)

    var notificationsAdapter: NotificationsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button) as Button?
        miscText = findViewById(R.id.miscText) as TextView?
        notificationList = findViewById(R.id.notificationList) as ListView?
        notificationsAdapter =  NotificationsAdapter(this, arrayListOf())
        notificationList?.setAdapter(notificationsAdapter)
        button?.setOnClickListener { v -> loadNotification() }

        loadNotification()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu)

        return true
    }

    fun loadNotification() {
        miscText?.setText("Loading")
        githubSubscription = service.
                notifications(BuildConfig.GITHUB_AUTH_TOKEN).
                observeOn(mainThread()).
                subscribe({notifications ->
                    miscText?.setText("Notifications: " + notifications.size())
                    notificationsAdapter?.clear()
                    notificationsAdapter?.addAll(notifications)
                }, { throwable ->
                    miscText?.setText("Error: " + throwable)
                })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item!!.getItemId()

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    inner class NotificationsAdapter(ctx: Context, notifications : List<GitHubNotification>) :
            ArrayAdapter<GitHubNotification>(ctx, 0, notifications) {
        public inline fun <T: Any> dsl(crossinline f: AnkoContext<Context>.() -> T): T {
            var view: T? = null
            getContext().UI { view = f() }
            return view!!
        }

        override fun getItemViewType(position: Int): Int {
            return 0
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val notification = getItem(position)
            var view : TextView =
               if (convertView == null || convertView !is TextView) {
                   dsl {
                       textView() {
                           textSize = 16f
                       }
                   }
               } else {
                   convertView
               }
            view.setText(notification.subject.title)
            return view
        }
    }
}