package dev.langchain4j.model.nomic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.langchain4j.internal.Utils;
import lombok.Builder;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.time.Duration;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;

class NomicClient {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();

    private final NomicApi nomicApi;
    private final String authorizationHeader;

    @Builder
    NomicClient(String baseUrl, String apiKey, Duration timeout, Boolean logRequests, Boolean logResponses) {

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout);

        if (logRequests) {
            okHttpClientBuilder.addInterceptor(new RequestLoggingInterceptor());
        }
        if (logResponses) {
            okHttpClientBuilder.addInterceptor(new ResponseLoggingInterceptor());
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Utils.ensureTrailingForwardSlash(baseUrl))
                .client(okHttpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .build();

        this.nomicApi = retrofit.create(NomicApi.class);
        this.authorizationHeader = "Bearer " + ensureNotBlank(apiKey, "apiKey");
    }

    public EmbeddingResponse embed(EmbeddingRequest request) {
        try {
            retrofit2.Response<EmbeddingResponse> retrofitResponse
                    = nomicApi.embed(request, authorizationHeader).execute();

            if (retrofitResponse.isSuccessful()) {
                return retrofitResponse.body();
            } else {
                throw toException(retrofitResponse);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static RuntimeException toException(retrofit2.Response<?> response) throws IOException {
        int code = response.code();
        String body = response.errorBody().string();
        String errorMessage = String.format("status code: %s; body: %s", code, body);
        return new RuntimeException(errorMessage);
    }
}
