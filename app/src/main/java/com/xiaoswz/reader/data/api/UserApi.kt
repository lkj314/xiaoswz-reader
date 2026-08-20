package com.xiaoswz.reader.data.api

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// 用户主页 & 互动（0.7.5）。从 BackendApi.kt 拆分（P2）。
interface UserApi {

    @GET("api/users/{id}")
    suspend fun getUserProfile(@Path("id") id: String): UserProfile

    @GET("api/users/{id}/posts")
    suspend fun getUserPosts(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
    ): PostListResponse

    @GET("api/users/{id}/booklists")
    suspend fun getUserBooklists(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
    ): BooklistListResponse

    @GET("api/users/{id}/bookshelf")
    suspend fun getUserBookshelf(@Path("id") id: String): UserBookshelfResponse

    @GET("api/users/{id}/followers")
    suspend fun getFollowers(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
    ): UserListResponse

    @GET("api/users/{id}/following")
    suspend fun getFollowing(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
    ): UserListResponse

    @POST("api/users/{id}/follow")
    suspend fun followUser(@Path("id") id: String): FollowResponse

    @POST("api/users/{id}/block")
    suspend fun blockUser(@Path("id") id: String): BlockResponse
}
