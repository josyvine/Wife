package com.tradeanalyst.app;

import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class GeminiRetrofitClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static GeminiApiService sService;

    // --- Data Transfer Objects ---

    public static class GenerateContentRequest {
        public List<Content> contents;
        public GenerationConfig generationConfig;
        public List<Tool> tools;
        public Content systemInstruction;

        public GenerateContentRequest(List<Content> contents) {
            this.contents = contents;
        }
    }

    public static class Content {
        public String role;
        public List<Part> parts;

        public Content() {}

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }
    }

    public static class Part {
        public String text;
        public InlineData inlineData;

        public Part() {}

        public Part(String text) {
            this.text = text;
        }

        public Part(InlineData inlineData) {
            this.inlineData = inlineData;
        }
    }

    public static class InlineData {
        public String mimeType;
        public String data; // Base64 representation

        public InlineData() {}

        public InlineData(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }
    }

    public static class GenerationConfig {
        public Double temperature;
        public Integer topK;
        public Double topP;
        public Integer maxOutputTokens;

        public GenerationConfig() {}

        public GenerationConfig(Double temperature) {
            this.temperature = temperature;
        }
    }

    public static class Tool {
        public GoogleSearch googleSearch;

        public Tool() {}

        public Tool(GoogleSearch googleSearch) {
            this.googleSearch = googleSearch;
        }
    }

    public static class GoogleSearch {
        // Empty object tells Gemini to use Google Search grounding
    }

    public static class GenerateContentResponse {
        public List<Candidate> candidates;
    }

    public static class Candidate {
        public Content content;
        public String finishReason;
    }

    public static class ModelsQueryResponse {
        public List<GeminiModelInfo> models;
    }

    public static class GeminiModelInfo {
        public String name;
        public String version;
        public String displayName;
        public String description;
        public List<String> supportedGenerationMethods;
    }

    // --- API Service Endpoint Interface ---

    public interface GeminiApiService {
        @POST("v1beta/{modelName}:generateContent")
        Call<GenerateContentResponse> generateContent(
            @Path(value = "modelName", encoded = true) String modelName,
            @Query("key") String apiKey,
            @Body GenerateContentRequest request
        );

        @GET("v1beta/models")
        Call<ModelsQueryResponse> listModels(
            @Query("key") String apiKey
        );
    }

    // --- Singleton Access ---

    public static synchronized GeminiApiService getService() {
        if (sService == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

            sService = retrofit.create(GeminiApiService.class);
        }
        return sService;
    }
}
