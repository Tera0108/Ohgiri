package com.websarva.wings.android.ohgiri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
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

    var temperature = "";
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

        val retryBtn = findViewById<Button>(R.id.retryBtn)
        val retryListener = RetryListener()
        retryBtn.setOnClickListener(retryListener)
    }

    private inner class GetThemeListener : View.OnClickListener {
        override fun onClick(v: View?) {
            val output = findViewById<TextView>(R.id.theme)

            temperature = "1.0";

            messageData =
                "{\"role\": \"system\", \"content\":\"あなたは松本人志です。\"},{\"role\": \"user\", \"content\":\"大喜利のお題を考えてください。\"}"

            val urlFull = "https://api.openai.com/v1/chat/completions"
            val result = receiveChatGptResponse(urlFull)
            if (result != null) {
                theme = result.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                    .getString("content")
                output.text = theme
                messageData =
                    messageData + ",{\"role\": \"assistant\", \"content\":\"" + theme + "\"}"
                Log.d("debug", "お題追加：" + messageData + "temperature設定値" + temperature)
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
                "Bearer APIKEY"
            )

            val bodyData =
                "{\"model\":\"gpt-3.5-turbo\",\"messages\":[" + messageData + "],\"temperature\":" + temperature + ",\"max_tokens\":100}"

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

            temperature = "0.1";

            messageData =
                messageData + ",{\"role\": \"user\", \"content\":\"回答「" + answerString + "」を10点満点で採点してください。返答の形式は1から10の整数値で返して。 例「1」 \"}"

            val urlFull = "https://api.openai.com/v1/chat/completions"
            val result = receiveChatGptResponse(urlFull)
            if (result != null) {
                val score = result.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                    .getString("content")
                output.text = score
                Log.d("debug", "点数：" + score)
                messageData =
                    messageData + ",{\"role\": \"assistant\", \"content\":\"" + score + "\"}"
                Log.d("debug", "点数追加：" + messageData + "temperature設定値" + temperature)
            }

        }

    }

    // ChatGPTの模範解答ボタン
    private inner class ChatgptAnswerListener : View.OnClickListener {
        override fun onClick(v: View?) {
            val output = findViewById<TextView>(R.id.chatgptAnswer)

            temperature = "0.1";

            messageData = messageData + ",{\"role\": \"user\", \"content\":\"大喜利のお題の「" + theme + "」のシュールな解答を1つ答えてください。15文字以内で解答だけ返してください。例1は、お題「魔法使いが覚えたけど、結局使わない魔法とは？」で、回答「半透明人間になれる」です。例2は、お題「あだ名が「ダイナミック」な医者 その理由とは？」で、回答「脈拍をビートという」です。例3は、お題「芸能界の『暗黙の了解』を教えて下さい」で、回答「ガチャピンは手洗い ムックは丸洗い」です。例4は、お題「次のエピソードに、尾ひれを付けて話して下さい 『100円拾った』」で、回答「アメリカを敵にまわすかも知れない だけど俺は 100円拾った」です。例5は、お題「銭湯で長年番台を進めていた中村吉男さんが引退することに その理由とは？」で、回答「グラフィックデザインのほうに興味が出てきた」です。例6は、お題「「牛乳は噛んで飲むといい」みたいな事を教えて下さい」で、回答「ロッキーPART5は観なくてもいい」です。例7は、お題「「ナポリタン」という言葉を使って相手を震え上がらせて下さい」で、回答「幸せな家族の元に届いた不吉な一通のナポリタン」です。例8は、お題「殴られても蹴られてもチェ・ホンマンが離さなかったものとは？」で、回答「ドロー4」です。例9は、お題「0円でできる超スーパー暇つぶしとは？」で、回答「赤の他人の墓参り」です。例10は、お題「初めてドラゴンを退治しに行くのですが、アドバイスをお願いします」で、回答「手みやげ持ってくのと持ってかないのでは、全然対応が違うよ」です。\"}"

            val urlFull = "https://api.openai.com/v1/chat/completions"
            val result = receiveChatGptResponse(urlFull)
            if (result != null) {
                val out = result.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                    .getString("content")
                output.text = out
                Log.d("debug", "模範解答：" + out + "temperature設定値" + temperature)
                messageData = ""
            }
        }
    }

    // やり直すボタン
    private inner class RetryListener : View.OnClickListener {
        override fun onClick(v: View?) {
            val outputTheme = findViewById<TextView>(R.id.theme)
            outputTheme.text = ""
            val outputAnswer = findViewById<TextInputEditText>(R.id.answer)
            outputAnswer.text = Editable.Factory.getInstance().newEditable("")
            val outputScoreResult = findViewById<TextView>(R.id.scoreResult)
            outputScoreResult.text = "-"
            val outputChatgptAnswer = findViewById<TextView>(R.id.chatgptAnswer)
            outputChatgptAnswer.text = ""
            messageData = ""

            val sv = findViewById(R.id.scrollView) as ScrollView;
            sv.fullScroll(ScrollView.FOCUS_UP);

        }
    }
}