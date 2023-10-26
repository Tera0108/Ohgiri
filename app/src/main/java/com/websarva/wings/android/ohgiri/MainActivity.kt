package com.websarva.wings.android.ohgiri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val getThemeBtn = findViewById<Button>(R.id.getThemeBtn)
        val getThemeListener = GetThemeListener()
        getThemeBtn.setOnClickListener(getThemeListener)

        Log.d("debug","console log create")


        val scoringBtn = findViewById<Button>(R.id.scoringBtn)
        val scoringListener = ScoringListener()
        scoringBtn.setOnClickListener(scoringListener)
    }

    private inner class GetThemeListener: View.OnClickListener {
        override fun onClick(v: View?) {
            val output = findViewById<TextView>(R.id.theme)

            output.text = "お題だよ"
        }

    }

    private inner class ScoringListener: View.OnClickListener {
        override fun onClick(v: View?) {
            val answer = findViewById<TextInputEditText>(R.id.answer)

            Log.d("debug",answer.text.toString())
        }

    }
}