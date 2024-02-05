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
import org.json.JSONObject
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

    var messageData = "";
    var theme = "";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val getThemeBtn = findViewById<Button>(R.id.getThemeBtn)
        val getThemeListener = GetThemeListener()
        getThemeBtn.setOnClickListener(getThemeListener)

        Log.d("debug", "console log create")

        val scoringBtn = findViewById<Button>(R.id.scoringBtn)
        val scoringListener = ScoringListener()
        scoringBtn.setOnClickListener(scoringListener)

        val chatgptAnswerBtn = findViewById<Button>(R.id.chatgptAnswerBtn)
        val chatgptAnswerListener = ChatgptAnswerListener()
        chatgptAnswerBtn.setOnClickListener(chatgptAnswerListener)
    }

    private inner class GetThemeListener : View.OnClickListener {
        override fun onClick(v: View?) {
            val output = findViewById<TextView>(R.id.theme)

            messageData =
                "{\"role\": \"system\", \"content\":\"あなたはお笑い芸人です。\"},{\"role\": \"user\", \"content\":\"大喜利のお題を考えてください。\"}"

            val urlFull = "https://api.openai.com/v1/chat/completions"
            val result = receiveChatGptResponse(urlFull)
            if (result != null) {
                theme = result.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                    .getString("content")
                output.text = theme
                messageData =
                    messageData + ",{\"role\": \"assistant\", \"content\":\"" + theme + "\"}"
                Log.d("debug", "お題追加：" + messageData)
            }
        }
    }

    private fun receiveChatGptResponse(urlFull: String): JSONObject? {
        val backgroundReceiver = ChatGptResponseBackGroundReceiver(urlFull)
        val executeService = Executors.newSingleThreadExecutor()//
        val future = executeService.submit(backgroundReceiver)
        return future.get()
    }

    private inner class ChatGptResponseBackGroundReceiver(url: String) : Callable<JSONObject> {
        //api reference : https://platform.openai.com/docs/api-reference/chat/create
        private val _url = url

        @WorkerThread
        override fun call(): JSONObject? {
            var result = JSONObject()
            val url = URL(_url)
            val con = url.openConnection() as HttpURLConnection
            // 1000だとタイムアウトになるので10000にのばした
            con.connectTimeout = 100000
            con.readTimeout = 10000
            con.requestMethod = "POST"
            // Bodyへ書き込むを行う
            con.doOutput = true

            // リクエストBodyのストリーミング有効化（どちらか片方を有効化）
            // connection.setFixedLengthStreamingMode(bodyData.size)
            con.setChunkedStreamingMode(0)

            // ヘッダーの設定
            con.setRequestProperty("Content-type", "application/json")
            con.setRequestProperty(
                "Authorization",
                "Bearer XXX"
            )

            val bodyData =
                "{\"model\":\"gpt-3.5-turbo\",\"messages\":[" + messageData + "],\"temperature\":1.0,\"max_tokens\":256}"

            Log.d("debug", "リクエスト：" + bodyData)

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
            } catch (ex: SocketTimeoutException) {
                Log.w("debug", "通信タイムアウト", ex)
            } catch (exception: Exception) {
                Log.e("Error", exception.toString())
            } finally {
                con.disconnect()
                Log.d("debug", result.toString())
                return result
            }
        }

        private fun is2JSON(stream: InputStream): JSONObject {
            val sb = StringBuilder()
            val reader = BufferedReader(
                InputStreamReader(
                    stream, StandardCharsets.UTF_8
                )
            )
            var line = reader.readLine()
            while (line != null) {
                sb.append(line)
                line = reader.readLine()
            }
            reader.close()

            // TODO: StringからJSONに変換する処理を追加
            return JSONObject(sb.toString())
        }
    }


    private inner class ScoringListener : View.OnClickListener {
        override fun onClick(v: View?) {
            val answer = findViewById<TextInputEditText>(R.id.answer)
            val output = findViewById<TextView>(R.id.scoreResult)

            val answerString = answer.text.toString()

            Log.d("debug", "入力した回答" + answerString)

            messageData =
                messageData + ",{\"role\": \"user\", \"content\":\"回答「" + answerString + "」を100点満点で採点してください。返答の形式は点数を示すint型で返して。\"}"

            val urlFull = "https://api.openai.com/v1/chat/completions"
            val result = receiveChatGptResponse(urlFull)
            if (result != null) {
                val score = result.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                    .getString("content")
                output.text = score
                Log.d("debug", "点数：" + score)
                messageData =
                    messageData + ",{\"role\": \"assistant\", \"content\":\"" + score + "\"}"
                Log.d("debug", "点数追加：" + messageData)
            }

        }

    }

    // ChatGPTの模範解答ボタン
    private inner class ChatgptAnswerListener : View.OnClickListener {
        override fun onClick(v: View?) {
            val output = findViewById<TextView>(R.id.chatgptAnswer)

            messageData = messageData + ",{\"role\": \"user\", \"content\":\"大喜利のお題の「" + theme + "」の模範解答を1つ考えてください。30文字以内でお願いします。\"}"

            val urlFull = "https://api.openai.com/v1/chat/completions"
            val result = receiveChatGptResponse(urlFull)
            if (result != null) {
                val out = result.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                    .getString("content")
                output.text = out
                Log.d("debug", "模範解答：" + out)
                messageData = ""
            }
        }
    }
}