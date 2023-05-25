package com.example.myapplication.api

import retrofit2.Call
import com.example.myapplication.data.Faculty
import retrofit2.http.GET
import retrofit2.http.Query

interface ServerAPI {
    @GET("?code=faculty")
    suspend fun getFaculty(): Call<List<Faculty>>

    @GET("?code=groups")
    suspend fun getGroups(@Query("faculty_id") id: Long): Call<List<Faculty>>
}