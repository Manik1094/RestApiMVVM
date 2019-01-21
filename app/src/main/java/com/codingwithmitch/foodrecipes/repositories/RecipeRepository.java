package com.codingwithmitch.foodrecipes.repositories;

import android.app.Application;
import android.util.Log;

import com.codingwithmitch.foodrecipes.MyApplication;
import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.requests.RecipeApi;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeSearchResponse;
import com.codingwithmitch.foodrecipes.util.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class RecipeRepository implements RequestCancelListener{

    private static final String TAG = "RecipeRepository";

    private static RecipeRepository instance;
    private RecipeApi mRecipeApi;
    private RecipeListCallback mRecipeListCallback;
    private RecipeCallback mRecipeCallback;
    private String mQuery;
    private int mPageNumber;

    // Calls
    private Call<RecipeSearchResponse> mRecipeSearchCall = null;

    public static RecipeRepository getInstance(Application application){
        if(instance == null){
            instance = new RecipeRepository(((MyApplication)application).getRetrofit().create(RecipeApi.class));
        }
        return instance;
    }


    private RecipeRepository(RecipeApi recipeApi) {
        mRecipeApi = recipeApi;
        mQuery = "";
        mPageNumber = 0;
    }

    public void setRecipeCallback(RecipeCallback callback){
        this.mRecipeCallback = callback;
    }

    public void setRecipeListCallback(RecipeListCallback callback){
        mRecipeListCallback = callback;
    }

    public void searchApi(String query, int pageNumber){
        mQuery = query;
        mPageNumber = pageNumber;

        mRecipeListCallback.onQueryStart();
        mRecipeSearchCall = mRecipeApi
                .searchRecipe(
                        Constants.API_KEY,
                        mQuery,
                        String.valueOf(mPageNumber)
                );

        mRecipeSearchCall.enqueue(recipeListSearchCallback);
    }

    public void searchNextPage(){
        searchApi(mQuery, mPageNumber + 1);
    }


    public void searchForRecipe(String recipeId){
        Call<RecipeResponse> responseCall = mRecipeApi
                .getRecipe(
                        Constants.API_KEY,
                        recipeId
                );

        responseCall.enqueue(singleRecipeCallback);
    }

    /**
     * Callback for retrieving a single recipe given a recipe id.
     */
    private Callback<RecipeResponse>  singleRecipeCallback = new Callback<RecipeResponse>() {
        @Override
        public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
            if(response.code() == 200){
                Log.d(TAG, "onResponse: " + response.body().toString());
            }
            else {
                try {
                    Log.d(TAG, "onResponse: " + response.errorBody().string());
                    mRecipeCallback.onError(null);
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mRecipeCallback.setRecipe(response.body().getRecipe());
        }

        @Override
        public void onFailure(Call<RecipeResponse> call, Throwable t) {
            mRecipeCallback.onError(t);
        }
    };


    private Callback<RecipeSearchResponse> recipeListSearchCallback = new Callback<RecipeSearchResponse>() {
        @Override
        public void onResponse(Call<RecipeSearchResponse> call, Response<RecipeSearchResponse> response) {
            if(response.code() == 200){
                Log.d(TAG, "onResponse: " + response.body().toString());
            }
            else {
                try {
                    Log.d(TAG, "onResponse: " + response.errorBody().string());
                    mRecipeListCallback.onQueryExhausted();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Set results to mRecipes list
            try{
                if(mPageNumber == 0){
                    mRecipeListCallback.setRecipes(response.body().getRecipes());
                }
                else{
                    List<Recipe> newRecipes = new ArrayList<>(response.body().getRecipes());
                    mRecipeListCallback.appendRecipes(newRecipes);
                }
                if(response.body().getRecipes().size() < 30){
                    mRecipeListCallback.onQueryExhausted();
                }

            }catch (NullPointerException e){
                Log.e(TAG, "onResponse: NullPointerException: " + e.getMessage() );
            }

            mRecipeListCallback.onQueryDone();
        }

        @Override
        public void onFailure(Call<RecipeSearchResponse> call, Throwable t) {
            Log.d(TAG, "onResponse: ERROR: " + t.getMessage());
            mRecipeListCallback.onQueryDone();
        }
    };

    @Override
    public void onCancel() {
        if(mRecipeSearchCall != null){
            mRecipeSearchCall.cancel();
            mRecipeListCallback.onQueryDone();
            mRecipeSearchCall = null;
        }
    }
}
















