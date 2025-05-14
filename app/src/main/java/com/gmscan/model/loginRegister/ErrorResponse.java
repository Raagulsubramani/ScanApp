package com.gmscan.model.loginRegister;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("detail")
    private String detail;

    public String getDetail() {
        return detail;
    }
}