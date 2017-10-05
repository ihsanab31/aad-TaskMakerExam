package com.google.developer.taskmaker;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.developer.taskmaker.data.DatabaseContract;
import com.google.developer.taskmaker.data.TaskAdapter;
import com.google.developer.taskmaker.data.TaskUpdateService;

import static com.google.developer.taskmaker.data.DatabaseContract.getColumnInt;
import static com.google.developer.taskmaker.data.DatabaseContract.getColumnLong;
import static com.google.developer.taskmaker.data.DatabaseContract.getColumnString;

public class MainActivity extends AppCompatActivity implements
        TaskAdapter.OnItemClickListener,
        View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int LOADER = 300;
    private TaskAdapter mAdapter;
    private RecyclerView recyclerView;
    private String order;

    private SharedPreferences mSharedPreferences;
    private Cursor mCursor;
    private MyObserver observer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        getLoaderManager().initLoader(LOADER, null, this);
        observer = new MyObserver(new Handler());


        // Get the instance of SharedPreferences object
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String value = mSharedPreferences.getString(getString(R.string.pref_sortBy_key), DatabaseContract.DEFAULT_SORT);
        if(value.equals(getResources().getString(R.string.pref_sortBy_default))){
            order = DatabaseContract.DEFAULT_SORT;
        } else {
            order = DatabaseContract.DATE_SORT;
        }

        mAdapter = new TaskAdapter();
        mAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mAdapter);


    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
        .registerOnSharedPreferenceChangeListener(this);

        getContentResolver().registerContentObserver(DatabaseContract.CONTENT_URI, true, observer);

    }

    @Override
    protected void onStart() {
        super.onStart();
        getLoaderManager().restartLoader(LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Click events in Floating Action Button */
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, AddTaskActivity.class);
        startActivity(intent);
    }

    /* Click events in RecyclerView items */
    @Override
    public void onItemClick(View v, int position) {
        //TODO: Handle list item click event
        Intent intent = new Intent(this, TaskDetailActivity.class);

        mCursor.moveToPosition(position);
        long id = getColumnLong(mCursor, DatabaseContract.TaskColumns._ID);
        Uri uri = ContentUris.withAppendedId(DatabaseContract.CONTENT_URI, id);
        intent.setData(uri);
        startActivity(intent);

    }

    /* Click events on RecyclerView item checkboxes */
    @Override
    public void onItemToggled(boolean active, int position) {
        //TODO: Handle task item checkbox event

        mCursor.moveToPosition(position);
        long id = getColumnLong(mCursor, DatabaseContract.TaskColumns._ID);
        Uri uri = ContentUris.withAppendedId(DatabaseContract.CONTENT_URI, id);

        ContentValues values = new ContentValues(4);
        values.put(DatabaseContract.TaskColumns.DESCRIPTION, getColumnString(mCursor, DatabaseContract.TaskColumns.DESCRIPTION));
        values.put(DatabaseContract.TaskColumns.IS_PRIORITY, getColumnInt(mCursor, DatabaseContract.TaskColumns.IS_PRIORITY));
        values.put(DatabaseContract.TaskColumns.IS_COMPLETE, active);
        values.put(DatabaseContract.TaskColumns.DUE_DATE, getColumnLong(mCursor, DatabaseContract.TaskColumns.DUE_DATE));
        TaskUpdateService.updateTask(this, uri, values);

    }

    class MyObserver extends ContentObserver {
        public MyObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            getLoaderManager().restartLoader(LOADER, null, MainActivity.this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(this, DatabaseContract.CONTENT_URI, null, null, null, order);

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        mCursor = getContentResolver().query(DatabaseContract.CONTENT_URI, null, null, null, order);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> data) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String value = mSharedPreferences.getString(getString(R.string.pref_sortBy_key), DatabaseContract.DEFAULT_SORT);

        if(value.equals(getResources().getString(R.string.pref_sortBy_default))){
            order = DatabaseContract.DEFAULT_SORT;
        } else {
            order = DatabaseContract.DATE_SORT;
        }
        getLoaderManager().restartLoader(LOADER, null, this);

    }
}
