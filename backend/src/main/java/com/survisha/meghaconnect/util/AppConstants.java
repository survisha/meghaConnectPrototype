package com.survisha.meghaconnect.util;

public final class AppConstants {
    private AppConstants() {}

    public static final String API_BASE = "/api/v1";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    // Application ID format
    public static final String APP_ID_PREFIX = "MC";
    public static final String APP_ID_FORMAT = "%s-%d-%05d";
    
    // File storage
    public static final long MAX_FILE_SIZE_MB = 10;
    public static final String[] ALLOWED_FILE_TYPES = {"pdf", "jpg", "jpeg", "png", "doc", "docx"};
}
