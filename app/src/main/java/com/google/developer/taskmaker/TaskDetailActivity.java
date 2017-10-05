package com.google.developer.taskmaker;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.developer.taskmaker.data.DatabaseContract;
import com.google.developer.taskmaker.data.TaskUpdateService;
import com.google.developer.taskmaker.reminders.AlarmScheduler;
import com.google.developer.taskmaker.views.DatePickerFragment;

import java.sql.Timestamp;
import java.util.Calendar;

import static com.google.developer.taskmaker.data.DatabaseContract.getColumnInt;
import static com.google.developer.taskmaker.data.DatabaseContract.getColumnLong;
import static com.google.developer.taskmaker.data.DatabaseContract.getColumnString;

public class TaskDetailActivity extends AppCompatActivity implements
        DatePickerDialog.OnDateSetListener {

    private TextView tvDueDate;
    private Uri taskUri;
    private long dueDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Task must be passed to this activity as a valid provider Uri
        taskUri = getIntent().getData();
        Cursor cursor = getContentResolver().query(taskUri, null, null, null, DatabaseContract.DEFAULT_SORT);

        if(!cursor.moveToNext()){
            return;
        }

        //TODO: Display attributes of the provided task in the UI
        setContentView(R.layout.activity_detail);

        TextView tvDescription = (TextView)findViewById(R.id.tv_description);
        tvDescription.setText(getColumnString(cursor, DatabaseContract.TaskColumns.DESCRIPTION));

        int isPriority = getColumnInt(cursor, DatabaseContract.TaskColumns.IS_PRIORITY);
        ImageView ivPriority = (ImageView) findViewById(R.id.iv_priority);
        if(isPriority == 1){
            ivPriority.setImageResource(R.drawable.ic_priority);
        } else {
            ivPriority.setImageResource(R.drawable.ic_not_priority);
        }

        dueDate = getColumnLong(cursor, DatabaseContract.TaskColumns.DUE_DATE);
        tvDueDate = (TextView) findViewById(R.id.tv_set_duedate);

        if (dueDate == Long.MAX_VALUE) {
            tvDueDate.setText(R.string.date_empty);
        } else {
            CharSequence formatted = DateUtils.getRelativeTimeSpanString(this, dueDate);
            tvDueDate.setText(formatted);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_reminder) {
            //TODO: Remider
            DatePickerFragment dialogFragment = new DatePickerFragment();
            dialogFragment.show(getSupportFragmentManager(), "datePicker");

        }
        if(id == R.id.action_delete) {
            TaskUpdateService.deleteTask(this, taskUri);
            finish();
        }

        return super.onOptionsItemSelected(item);


    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        //TODO: Handle date selection from a DatePickerFragment

        //Set to noon on the selected day
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        Calendar calNow = Calendar.getInstance();
        Timestamp currentDateTime = new Timestamp(calNow.getTimeInMillis());
        Timestamp selectDateTime = new Timestamp(c.getTimeInMillis());
        Timestamp dueDateTime = new Timestamp(dueDate);

        if(selectDateTime.after(currentDateTime) && selectDateTime.before(dueDateTime)) {
            Toast.makeText(this, R.string.reminder_set, Toast.LENGTH_LONG).show();
            AlarmScheduler.scheduleAlarm(this, c.getTimeInMillis(), taskUri);
        } else {

            if (selectDateTime.before(currentDateTime)){
                Toast.makeText(this, R.string.reminder_past, Toast.LENGTH_LONG).show();
                DatePickerFragment dialogFragment = new DatePickerFragment();
                dialogFragment.show(getSupportFragmentManager(), "datePicker");
            }

            if(selectDateTime.after(dueDateTime)){
                Toast.makeText(this, R.string.reminder_over_due, Toast.LENGTH_LONG).show();
                DatePickerFragment dialogFragment = new DatePickerFragment();
                dialogFragment.show(getSupportFragmentManager(), "datePicker");
            }
        }





    }

}
