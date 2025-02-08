package com.VideoCall.application.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class GoogleAuthController {

    private static final String CLIENT_ID = "164257842675-aksqluat6fub2tohjp9tjfdkbn9er894.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "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";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    /**
     * Redirects the user to Google's OAuth 2.0 authorization endpoint.
     */
    @GetMapping("/google-auth")
    public void redirectToGoogle(HttpServletResponse response) {
        try {
            // Combine scopes for Google Calendar and Google Drive
            String scopes = URLEncoder.encode(
                    "https://www.googleapis.com/auth/calendar " + // Calendar scope
                            "https://www.googleapis.com/auth/calendar.events " + // Calendar events scope
                            "https://www.googleapis.com/auth/drive", // Drive scope
                    StandardCharsets.UTF_8
            );

            // Construct the Google OAuth 2.0 authorization URL
            String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "scope=" + scopes + "&" +
                    "response_type=code&" +
                    "redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) + "&" +
                    "client_id=" + CLIENT_ID;

            // Redirect the user to Google's authorization page
            response.sendRedirect(googleAuthUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the callback from Google and exchanges the authorization code for access and refresh tokens.
     */
    @GetMapping("/callback")
    public void handleGoogleCallback(@RequestParam("code") String authorizationCode,
                                     HttpServletRequest request, HttpServletResponse response) {
        HttpPost post = new HttpPost(TOKEN_URL);

        try {
            String params = "code=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8) +
                    "&client_id=" + CLIENT_ID +
                    "&client_secret=" + CLIENT_SECRET +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code";

            post.setEntity(new StringEntity(params, StandardCharsets.UTF_8));
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            try (CloseableHttpResponse httpResponse = HttpClientBuilder.create().build().execute(post)) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                JSONObject jsonResponse = new JSONObject(responseBody);

                if (jsonResponse.has("access_token")) {
                    String accessToken = jsonResponse.getString("access_token");
                    String refreshToken = jsonResponse.optString("refresh_token", null);

                    // Store tokens in session
                    request.getSession().setAttribute("accessToken", accessToken);
                    request.getSession().setAttribute("refreshToken", refreshToken);

                    // Redirect to /googleevents after authentication
                    response.sendRedirect("http://localhost:8080/googleevents.html");
                } else {
                    response.getWriter().write("Error: " + responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.getWriter().write("Error during authentication: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Retrieves the user's Google Calendar events.
     */
    @GetMapping("/get-events")
    public String getCalendarEvents(HttpServletRequest request) {
        String accessToken = (String) request.getSession().getAttribute("accessToken");

        if (accessToken == null) {
            return "Access token is not available. Authenticate first.";
        }

        String calendarUrl = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
        HttpGet get = new HttpGet(calendarUrl);
        get.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpResponse response = HttpClientBuilder.create().build().execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject jsonResponse = new JSONObject(responseBody);

            JSONArray events = jsonResponse.optJSONArray("items");
            if (events != null) {
                StringBuilder eventList = new StringBuilder("Your Calendar Events:\n");
                for (int i = 0; i < events.length(); i++) {
                    JSONObject event = events.getJSONObject(i);
                    eventList.append("Event: ").append(event.optString("summary", "No Title")).append("\n");
                    eventList.append("Start: ").append(event.optJSONObject("start").optString("dateTime", "N/A")).append("\n");
                    eventList.append("End: ").append(event.optJSONObject("end").optString("dateTime", "N/A")).append("\n\n");
                }
                return eventList.toString();
            }
            return "No events found in your calendar.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to retrieve events.";
        }
    }

    /**
     * Adds a new event to the user's Google Calendar.
     */
    @PostMapping("/add-event")
    public String addCalendarEvent(@RequestBody String eventDetails, HttpServletRequest request) {
        String accessToken = (String) request.getSession().getAttribute("accessToken");

        if (accessToken == null) {
            return "Access token is not available. Authenticate first.";
        }

        String calendarUrl = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
        HttpPost post = new HttpPost(calendarUrl);
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/json");

        try {
            post.setEntity(new StringEntity(eventDetails, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = HttpClientBuilder.create().build().execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject jsonResponse = new JSONObject(responseBody);

                if (jsonResponse.has("id")) {
                    return "Event created successfully with ID: " + jsonResponse.getString("id");
                } else {
                    return "Failed to create event: " + responseBody;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error while creating event: " + e.getMessage();
        }
    }

    /**
     * Retrieves the list of files from the user's Google Drive.
     */
    @GetMapping("/get-files")
    public String getDriveFiles(HttpServletRequest request) {
        String accessToken = (String) request.getSession().getAttribute("accessToken");

        if (accessToken == null) {
            return "Access token is not available. Authenticate first.";
        }

        String driveUrl = "https://www.googleapis.com/drive/v3/files";
        HttpGet get = new HttpGet(driveUrl);
        get.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpResponse response = HttpClientBuilder.create().build().execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject jsonResponse = new JSONObject(responseBody);

            JSONArray files = jsonResponse.optJSONArray("files");
            if (files != null) {
                StringBuilder fileList = new StringBuilder("Your Google Drive Files:\n");
                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.getJSONObject(i);
                    fileList.append("File Name: ").append(file.optString("name", "No Name")).append("\n");
                    fileList.append("File ID: ").append(file.optString("id", "N/A")).append("\n");
                    fileList.append("MIME Type: ").append(file.optString("mimeType", "N/A")).append("\n\n");
                }
                return fileList.toString();
            }
            return "No files found in your Google Drive.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to retrieve files.";
        }
    }

    /**
     * Uploads a file to the user's Google Drive.
     */
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadFileToDrive(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String accessToken = (String) request.getSession().getAttribute("accessToken");

        if (accessToken == null) {
            return "Access token is not available. Authenticate first.";
        }

        String uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=media";
        HttpPost post = new HttpPost(uploadUrl);
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/octet-stream");

        try {
            post.setEntity(new InputStreamEntity(file.getInputStream()));

            try (CloseableHttpResponse response = HttpClientBuilder.create().build().execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject jsonResponse = new JSONObject(responseBody);

                if (jsonResponse.has("id")) {
                    return "File uploaded successfully with ID: " + jsonResponse.getString("id");
                } else {
                    return "Failed to upload file: " + responseBody;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error while uploading file: " + e.getMessage();
        }
    }
}