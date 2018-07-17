package com.binance.api.client.impl;

import com.binance.api.client.BinanceApiError;
import com.binance.api.client.constant.BinanceApiConstants;
import com.binance.api.client.exception.BinanceApiException;
import com.binance.api.client.security.AuthenticationInterceptor;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import com.julienviet.retrofit.vertx.VertxCallFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;

/**
 * Generates a Binance API implementation based on @see {@link BinanceApiService}.
 */
public class BinanceApiServiceGenerator {

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static Retrofit.Builder builder =
        new Retrofit.Builder()
            .baseUrl(BinanceApiConstants.API_BASE_URL)
            .addConverterFactory(JacksonConverterFactory.create());

    private static Retrofit retrofit = builder.build();

    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, null, null);
    }

    public static <S> S createService(Class<S> serviceClass, String apiKey, String secret) {
        retrofit = setupAuth(apiKey, secret, retrofit);
        return retrofit.create(serviceClass);
    }

    public static <S> S createService(Class<S> serviceClass, String apiKey, String secret, Vertx vertex) {
        HttpClient client = vertex.createHttpClient();
        Retrofit vertexRetrofit = new Retrofit.Builder()
                .callFactory(new VertxCallFactory(client))
                .baseUrl(BinanceApiConstants.API_BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        vertexRetrofit = setupAuth(apiKey, secret, vertexRetrofit);
        return vertexRetrofit.create(serviceClass);
    }

    private static Retrofit setupAuth(String apiKey, String secret, Retrofit retrofit) {
        if (!StringUtils.isEmpty(apiKey) && !StringUtils.isEmpty(secret)) {
            AuthenticationInterceptor interceptor = new AuthenticationInterceptor(apiKey, secret);
            if (!httpClient.interceptors().contains(interceptor)) {
                httpClient.addInterceptor(interceptor);
                builder.client(httpClient.build());
                retrofit = builder.build();
            }
        }
        return retrofit;
    }

    /**
     * Execute a REST call and block until the response is received.
     */
    public static <T> T executeSync(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                BinanceApiError apiError = getBinanceApiError(response);
                throw new BinanceApiException(apiError);
            }
        } catch (IOException e) {
            throw new BinanceApiException(e);
        }
    }

    /**
     * Extracts and converts the response error body into an object.
     */
    public static BinanceApiError getBinanceApiError(Response<?> response) throws IOException, BinanceApiException {
        return (BinanceApiError)retrofit.responseBodyConverter(BinanceApiError.class, new Annotation[0])
            .convert(response.errorBody());
    }
}