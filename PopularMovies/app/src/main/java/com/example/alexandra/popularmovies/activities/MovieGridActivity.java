package com.example.alexandra.popularmovies.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.GridView;

import com.example.alexandra.popularmovies.BuildConfig;
import com.example.alexandra.popularmovies.R;
import com.example.alexandra.popularmovies.fragments.MovieDetailFragment;
import com.example.alexandra.popularmovies.fragments.MovieGridFragment;
import com.example.alexandra.popularmovies.models.Movie;
import com.example.alexandra.popularmovies.network.MainApi;
import com.example.alexandra.popularmovies.network.responses.MoviesListResponse;
import com.example.alexandra.popularmovies.views.MovieGridAdapter;
import com.squareup.okhttp.OkHttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.InjectView;


public class MovieGridActivity
        extends RoboActionBarActivity
        implements MovieGridFragment.Callbacks {
    private static final int   MOST_POPULAR = 0;
    private static final int   HIGHEST_RATED = 1;
    private boolean            mTwoPane;
    private MainApi            mainApi;
    private ArrayList<Movie>   mMovieArrayList;
    private MovieGridAdapter   mMovieGridAdapter;
    @InjectView(R.id.gridViewMovies)
    private GridView           mGridViewMovies;
    @InjectView(R.id.swipeRefreshGridView)
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_grid);

        mMovieArrayList = new ArrayList<Movie>();

        mMovieGridAdapter = new MovieGridAdapter(this,
                R.layout.item_movie,
                mMovieArrayList);
        mGridViewMovies.setAdapter(mMovieGridAdapter);

        if (findViewById(R.id.movie_detail_container) != null) {
            mTwoPane = true;
            ((MovieGridFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.movie_grid))
                    .setActivateOnItemClick(true);
        }

        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(
                30,
                TimeUnit.SECONDS
        );
        client.setReadTimeout(
                120,
                TimeUnit.SECONDS
        );

        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(BuildConfig.ENDPOINT
                ).setClient(new OkClient(client)
                ).build();
        mainApi = restAdapter.create(MainApi.class);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getMoviesPopular(MOST_POPULAR);
            }
        });

    }

    @Override
    public void onStart(){
        super.onStart();
        getMoviesPopular(MOST_POPULAR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_movie_grid,
                menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_most_popular:
               getMoviesPopular(MOST_POPULAR);
                return true;
            case R.id.sort_highest_rated:
                getMoviesPopular(HIGHEST_RATED);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemSelected(String id) {
        Movie movie = mMovieArrayList.get(Integer.parseInt(id));
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putParcelable(MovieDetailFragment.ARG_ITEM_ID,
                    movie);
            MovieDetailFragment fragment = new MovieDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.movie_detail_container,
                            fragment)
                    .commit();

        } else {
            Intent detailIntent = new Intent(this,
                    MovieDetailActivity.class);
            detailIntent.putExtra(MovieDetailFragment.ARG_ITEM_ID,
                    movie);
            startActivity(detailIntent);
        }
    }

    public void getMoviesPopular(final int sortBy) {
        mainApi.getMoviesPopular(BuildConfig.API_KEY,
                new Callback<MoviesListResponse>() {
                    @Override
                    public void success(MoviesListResponse moviesListResponse,
                                        Response response) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        if (!mMovieArrayList.containsAll(moviesListResponse.getMovieArrayList())){
                            mMovieArrayList.clear();
                            mMovieArrayList.addAll(moviesListResponse.getMovieArrayList());
                        }else if (mMovieArrayList.size() != moviesListResponse.getMovieArrayList()
                                .size()) {
                            for (Movie movie : moviesListResponse.getMovieArrayList()) {
                                if (movie != null && !mMovieArrayList.contains(movie)) {
                                    mMovieArrayList.add(movie);
                                }
                            }
                        }
                        if(sortBy == MOST_POPULAR){
                            Collections.sort(mMovieArrayList,
                                    new ComparatorHighestRated());
                        }else if(sortBy == HIGHEST_RATED){
                            Collections.sort(mMovieArrayList,
                                    new ComparatorMostPopular());
                        }
                        mMovieGridAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        mSwipeRefreshLayout.setRefreshing(false);

                    }
                });
    }

    private class ComparatorMostPopular
            implements Comparator<Movie> {
        @Override
        public int compare(Movie o1, Movie o2) {
            if(o1.getPopularity() == o2.getPopularity())
                return 0;
            return o1.getPopularity() > o2.getPopularity()? -1 : 1;
        }


    }

    private class ComparatorHighestRated
            implements Comparator<Movie> {
        @Override
        public int compare(Movie o1, Movie o2) {
            if(o1.getVoteAverage() == o2.getVoteAverage())
                return 0;
            return o1.getVoteAverage() > o2.getVoteAverage()? -1 : 1;
        }


    }
}
