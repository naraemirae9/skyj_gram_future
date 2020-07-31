package com.example.instagram_future.navigation.util

import okhttp3.MediaType
import android.media.MediaTimestamp
import com.example.instagram_future.navigation.Model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.*
import java.io.IOException

class FcmPush{
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url ="https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAABYlKSec:APA91bFD4A2JOKorbhc0KsWGCzsq2Vt5-pOOwQSNOrfgfUIfcJE333LAVDjvjt9vZ-WZfVFmWXNEv0ZO4Jf7wXB6dmUs6jgiajDJOWpigB1NnZjLjz5ezM_CvF4-tt8iTRT6kVHk-Tnx"
    var gson : Gson?=null
    var okHttpClient : OkHttpClient? =null

    companion object {
    var instance =FcmPush()
    }
    init {
        gson = Gson()
        okHttpClient= OkHttpClient()
    }
    fun sendMessage(destinationUid : String, title : String, message : String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                var token = task?.result?.get("pushToken").toString()

                var pushDTO = PushDTO()
                pushDTO.notification.title = title
                pushDTO.notification.body = message

                var body = RequestBody.create(JSON,gson?.toJson(pushDTO))
                var request = Request.Builder()
                        .addHeader("Content-Type","application/json")
                        .addHeader("Authorization","key="+serverKey)
                        .url(url)
                        .post(body)
                        .build()

                okHttpClient?.newCall(request)?.enqueue(object  : Callback{
                    override fun onFailure(call: Call?, e: IOException?) {

                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        println(response?.body()?.string())
                    }

                })

            }
        }
    }
}