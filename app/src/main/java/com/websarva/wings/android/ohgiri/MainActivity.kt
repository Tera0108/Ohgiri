package com.websarva.wings.android.ohgiri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.WorkerThread
import com.google.android.material.textfield.TextInputEditText
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Executors

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

            val urlFull = "https://api.openai.com/v1/chat/completions"
            receiveChatGptResponse(urlFull)
//            output.text = "お題だよ"
        }

    }

    private fun receiveChatGptResponse(urlFull: String ){
        val backgroundReceiver = ChatGptResponseBackGroundReceiver(urlFull)
        val executeService = Executors.newSingleThreadExecutor()//
        val future = executeService.submit(backgroundReceiver)
        val result = future.get()
    }
    private inner class ChatGptResponseBackGroundReceiver(url: String): Callable<String>{
        //api reference : https://platform.openai.com/docs/api-reference/chat/create
        private val _url = url

        @WorkerThread
        override fun call(): String {
            var result = ""
            val url = URL(_url)
            val con = url.openConnection() as HttpURLConnection
            con.connectTimeout = 10000
            con.readTimeout = 10000
            con.requestMethod = "POST"
            // Bodyへ書き込むを行う
            con.doOutput = true

            // リクエストBodyのストリーミング有効化（どちらか片方を有効化）
            // connection.setFixedLengthStreamingMode(bodyData.size)
            con.setChunkedStreamingMode(0)

            // ヘッダーの設定
            con.setRequestProperty("Content-type", "application/json")
            con.setRequestProperty("Authorization", "Bearer YOUR_API_KEY")

            val bodyData = "{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\": \"system\", \"content\":\"あなたはお笑い芸人です。\"},{\"role\": \"user\", \"content\":\"大喜利のお題を考えてください。\"}],\"temperature\":0.0,\"max_tokens\":256}"

            try {
                con.connect()
                // Bodyの書き込み
                val outputStream = con.outputStream
                outputStream.write(bodyData.toByteArray())
                outputStream.flush()
                outputStream.close()

                val stream = con.inputStream
                result = is2JSON(stream)
                stream.close()
            }
            catch(ex: SocketTimeoutException) {
                Log.w("debug", "通信タイムアウト", ex)
            } catch (exception: Exception) {
                Log.e("Error", exception.toString())
            } finally {
                con.disconnect()
                Log.d("debug",result)
                return result
            }
        }

        private fun is2JSON(stream: InputStream): String {
            val sb = StringBuilder()
            val reader = BufferedReader(InputStreamReader(
                stream, StandardCharsets.UTF_8))
            var line = reader.readLine()
            while (line != null){
                sb.append(line)
                line = reader.readLine()
            }
            reader.close()

            // NOTE: StringからJSONに変換する処理を追加
            // 教科書p.294を見てね
            return sb.toString()
        }
    }


    private inner class ScoringListener: View.OnClickListener {
        override fun onClick(v: View?) {
            val answer = findViewById<TextInputEditText>(R.id.answer)

            Log.d("debug",answer.text.toString())
        }

    }
}