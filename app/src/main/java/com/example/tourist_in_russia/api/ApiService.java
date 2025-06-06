package com.example.tourist_in_russia.api;

import android.util.Log;
import com.example.tourist_in_russia.api.responses.RegisterResponse;
import com.example.tourist_in_russia.api.responses.TokenResponse;
import com.example.tourist_in_russia.api.responses.UserProfile;
import com.example.tourist_in_russia.api.models.Place;
import com.example.tourist_in_russia.api.models.Review;
import com.example.tourist_in_russia.api.requests.LoginRequest;
import com.example.tourist_in_russia.api.requests.PasswordChangeRequest;
import com.example.tourist_in_russia.api.requests.RegistrationRequest;
import com.example.tourist_in_russia.api.requests.ReviewCreateRequest;
import com.example.tourist_in_russia.api.requests.PlaceCreateRequest;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;

public interface ApiService {
    String TAG = "ApiService";
    String BASE_URL = "http://212.192.31.136:8000/";

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegistrationRequest request);

    @FormUrlEncoded
    @POST("auth/token")
    Call<TokenResponse> login(
        @Field("username") String username,
        @Field("password") String password
    );

    @GET("places")
    Call<List<Place>> getPlaces(@Query("skip") int skip, @Query("limit") int limit, @Query("sort_by_rating") boolean sortByRating);

    @GET("places/{id}")
    Call<Place> getPlace(@Path("id") String id);

    @GET("places/{id}")
    Call<Place> getPlaceById(@Path("id") String id);

    @GET("places/{id}/reviews")
    Call<List<Review>> getPlaceReviews(@Path("id") String id);

    @POST("places/{id}/reviews")
    Call<Review> createReview(@Path("id") String id, @Header("Authorization") String token, @Body ReviewCreateRequest request);

    @GET("auth/me")
    Call<UserProfile> getProfile(@Header("Authorization") String token);

    @PUT("auth/change-password")
    Call<Void> changePassword(@Header("Authorization") String token, @Body PasswordChangeRequest request);

    @Multipart
    @PUT("auth/avatar")
    Call<UserProfile> uploadPhoto(@Header("Authorization") String token, @Part MultipartBody.Part file);

    @Multipart
    @POST("places/{id}/main-photo")
    Call<Place> uploadPlaceMainPhoto(@Path("id") String id, @Header("Authorization") String token, @Part MultipartBody.Part file);

    @POST("places")
    Call<Place> createPlace(@Header("Authorization") String token, @Body PlaceCreateRequest request);

    @DELETE("reviews/{id}")
    Call<Void> deleteReview(@Path("id") String id, @Header("Authorization") String token);

    @DELETE("places/{id}")
    Call<Void> deletePlace(@Path("id") String id, @Header("Authorization") String token);

    class Factory {
        private static ApiService instance;

        public static ApiService getInstance() {
            if (instance == null) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
                    Log.d(TAG, "API Request/Response: " + message);
                });
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Log.d(TAG, "Request URL: " + original.url());
                        Log.d(TAG, "Request Headers: " + original.headers());
                        Log.d(TAG, "Request Method: " + original.method());
                        
                        Request request = original.newBuilder()
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body())
                            .build();

                        return chain.proceed(request);
                    })
                    .addInterceptor(logging)
                    .build();

                Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

                instance = retrofit.create(ApiService.class);
            }
            return instance;
        }
    }
} 