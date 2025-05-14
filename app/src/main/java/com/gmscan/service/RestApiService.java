package com.gmscan.service;

import com.gmscan.model.BaseResponse;
import com.gmscan.model.getAllDocuments.GetAllDocumentsResponseItem;
import com.gmscan.model.getDocumentById.GetDocumentById;
import com.gmscan.model.loginRegister.LoginRegisterResponse;
import com.gmscan.model.loginRegister.User;
import com.gmscan.model.resetPassword.ResetPasswordRequest;
import com.gmscan.model.uploads.UploadsResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RestApiService {
    @POST("/auth/register")
    Call<LoginRegisterResponse> registerUser(@Body User user);

    @POST("/auth/login")
    Call<LoginRegisterResponse> loginUser(@Body User user);

    @POST("/auth/forgot-password")
    Call<BaseResponse> forgotPassword(@Body User user);

    @POST("/auth/reset-password")
    Call<BaseResponse> resetPassword(@Body ResetPasswordRequest resetPasswordRequest);

    @POST("/auth/verify-otp")
    Call<BaseResponse> verifyOtp(@Body ResetPasswordRequest resetPasswordRequest);

    @POST("auth/google/mobile")
    Call<LoginRegisterResponse> verifyGoogleToken(@Body Map<String, String> tokenData);

    // LinkedIn OAuth endpoints
    @GET("/auth/linkedin/")
    Call<Void> initiateLinkedinAuth();

    @GET("/api/documents/{documentId}")
    Call<GetDocumentById> getDocumentById(@Path("documentId") String documentId);

    @GET("/api/documents")
    Call<List<GetAllDocumentsResponseItem>> getAllDocuments();

    @GET("/api/documents/favorites")
    Call<List<GetAllDocumentsResponseItem>> getFavoriteDocuments();

    // delete document endpoint
    @DELETE("/api/documents/{documentId}")
    Call<BaseResponse> deleteDocument(@Path("documentId") String documentId);

    @GET("api/documents/type/{scanType}")
    Call<List<GetAllDocumentsResponseItem>> getDocumentsByScanType(@Path("scanType") String scanType);

    @GET("api/documents/search/")
    Call<ResponseBody> searchDocuments(@Query("query") String query);

    @POST("auth/logout")
    Call<ResponseBody> logout();

    ///  Image upload using the multipart
    @Multipart
    @PUT("auth/update-user-profile")
    Call<LoginRegisterResponse> updateUserProfile(
            @Part("first_name") RequestBody firstName,
            @Part("last_name") RequestBody lastName,
            @Part("file_url") RequestBody fileUrl,
            @Part("email") RequestBody email,
            @Part("mobile_number") RequestBody mobileNumber
    );

    @Multipart
    @POST("/api/documents")
    Call<ResponseBody> createIDCard(
            @Part("document_name") RequestBody documentName,
            @Part("scan_type") RequestBody scanType,
            @Part("name") RequestBody name,
            @Part("profession") RequestBody profession,
            @Part("email") RequestBody email,
            @Part("mobile_number") RequestBody mobileNumber,
            @Part("address") RequestBody address,
            @Part("company_name") RequestBody companyName,
            @Part("website") RequestBody website,
            @Part("file_url") RequestBody fileUrl
    );

    @Multipart
    @POST("/api/documents")
    Call<ResponseBody> documentScan(
            @Part("document_name") RequestBody documentName,
            @Part("scan_type") RequestBody scanType,
            @Part("summary") RequestBody summary,
            @Part("file_url") RequestBody fileUrl
    );

    @Multipart
    @POST("/api/documents")
    Call<ResponseBody> createBook(
            @Part("isbn_no") RequestBody isbnNo,
            @Part("book_name") RequestBody bookName,
            @Part("document_name") RequestBody documentName,
            @Part("scan_type") RequestBody scanType,
            @Part("number_of_pages") RequestBody numberOfPages,
            @Part("subject") RequestBody subject,
            @Part("author_name") RequestBody authorName,
            @Part("publication") RequestBody publication,
            @Part("file_url") RequestBody fileUrl);

    ///  File upload using the multipart

    @Multipart
    @POST("api/uploads")
    Call<UploadsResponse> uploadImage(
            @Part MultipartBody.Part file
    );

    @Multipart
    @PUT("/api/documents/{documentId}")
    Call<com.gmscan.model.UpdateDocumentResponse> updateDocumentWithFormData(
            @Path("documentId") String documentId,
            @Part("document_name") RequestBody documentName,
            @Part("scan_type") RequestBody scanType,
            @Part("is_favorite") RequestBody isFavorite,
            @Part("file_url") RequestBody fileUrl,
            @Part("name") RequestBody name,
            @Part("profession") RequestBody profession,
            @Part("email") RequestBody email,
            @Part("mobile_number") RequestBody mobileNumber,
            @Part("address") RequestBody address,
            @Part("company_name") RequestBody companyName,
            @Part("website") RequestBody website,
            @Part("isbn_no") RequestBody isbnNo,
            @Part("book_name") RequestBody bookName,
            @Part("author_name") RequestBody authorName,
            @Part("publication") RequestBody publication,
            @Part("number_of_pages") RequestBody numberOfPages,
            @Part("subject") RequestBody subject,
            @Part("summary") RequestBody summary,
            @Part("content") RequestBody content
    );


    @Multipart
    @PUT("/api/documents/{documentId}")
    Call<ResponseBody> updateDocumentWithFormData(
            @Path("documentId") String documentId,
            @Part("document_name") RequestBody documentName,
            @Part("scan_type") RequestBody scanType,
            @Part("file_url") RequestBody fileUrl,
            @Part("summary") RequestBody summary
    );


    @Multipart
    @PUT("/api/documents/{documentId}")
    Call<ResponseBody> updateDocumentWithFormData(
            @Path("documentId") String documentId,
            @Part("document_name") RequestBody documentName,
            @Part("book_name") RequestBody bookName,
            @Part("scan_type") RequestBody scanType,
            @Part("file_url") RequestBody fileUrl,
            @Part("isbn_no") RequestBody isbnNo,
            @Part("author_name") RequestBody authorName,
            @Part("publication") RequestBody publication,
            @Part("number_of_pages") RequestBody numberOfPages,
            @Part("subject") RequestBody subject
    );

    @GET("/profile")
    Call<User> getUserProfile();

    Call<LoginRegisterResponse> linkedinAuthCallback(String fullUrl);
}