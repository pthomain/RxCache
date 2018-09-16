package uk.co.glass_software.android.cache_interceptor.demo

import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.RadioButton
import android.widget.TextView
import uk.co.glass_software.android.boilerplate.lambda.Callback1
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity.Method.RETROFIT
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity.Method.VOLLEY
import uk.co.glass_software.android.cache_interceptor.demo.R.id.result
import uk.co.glass_software.android.cache_interceptor.demo.retrofit.RetrofitDemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.volley.VolleyDemoPresenter

class DemoActivity : AppCompatActivity() {

    private var method = RETROFIT

    private lateinit var loadButton: Button
    private lateinit var refreshButton: Button
    private lateinit var clearButton: Button

    private lateinit var retrofitDemoPresenter: DemoPresenter
    private lateinit var volleyDemoPresenter: DemoPresenter
    private lateinit var listAdapter: ExpandableListAdapter

    private val demoPresenter: DemoPresenter
        get() {
            return when (method) {
                RETROFIT -> retrofitDemoPresenter
                VOLLEY -> volleyDemoPresenter
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadButton = findViewById(R.id.load_button)
        refreshButton = findViewById(R.id.refresh_button)
        clearButton = findViewById(R.id.clear_button)

        loadButton.setOnClickListener { _ -> onButtonClick(false) }
        refreshButton.setOnClickListener { _ -> onButtonClick(true) }
        clearButton.setOnClickListener { _ ->
            setButtonsEnabled(false)
            demoPresenter.clearEntries()
        }

        findViewById<View>(R.id.github).setOnClickListener { _ -> openGithub() }

        val retrofitRadioButton = findViewById<RadioButton>(R.id.radio_button_retrofit)
        val volleyRadioButton = findViewById<RadioButton>(R.id.radio_button_volley)

        retrofitRadioButton.setOnClickListener { _ -> method = RETROFIT }
        volleyRadioButton.setOnClickListener { _ -> method = VOLLEY }

        val jokeView = findViewById<TextView>(R.id.joke)
        listAdapter = ExpandableListAdapter(
                this,
                { text: CharSequence -> jokeView.text = text },
                { setButtonsEnabled(true) }
        )

        retrofitDemoPresenter = RetrofitDemoPresenter(this) { listAdapter.log(it) }
        volleyDemoPresenter = VolleyDemoPresenter(this) { listAdapter.log(it) }

        val result = findViewById<ExpandableListView>(R.id.result)
        result.setAdapter(listAdapter)
        listAdapter.notifyDataSetChanged()
    }

    private fun onButtonClick(isRefresh: Boolean) {
        setButtonsEnabled(false)
        listAdapter.loadJoke(demoPresenter.loadResponse(isRefresh))
    }

    private fun setButtonsEnabled(isEnabled: Boolean) {
        loadButton.isEnabled = isEnabled
        refreshButton.isEnabled = isEnabled
        clearButton.isEnabled = isEnabled
    }

    private fun openGithub() {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse("https://github.com/pthomain/RxCacheInterceptor"))
    }

    private enum class Method {
        RETROFIT,
        VOLLEY
    }
}
