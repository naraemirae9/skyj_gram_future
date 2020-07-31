package com.example.instagram_future.navigation.Model


data class PushDTO(
        var to : String? =null,
        var notification : Notification =Notification()
){
    data class Notification(
            var body : String?= null,
            var title: String? =null
    )
}