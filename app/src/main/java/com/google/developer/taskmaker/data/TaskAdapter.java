package com.google.developer.taskmaker.data;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.developer.taskmaker.R;
import com.google.developer.taskmaker.views.TaskTitleView;

import java.sql.Timestamp;
import java.util.Calendar;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {

    public TaskAdapter() {

    }

    /* Callback for list item click events */
    public interface OnItemClickListener {
        void onItemClick(View v, int position);

        void onItemToggled(boolean active, int position);
    }

    /* ViewHolder for each task item */
    public class TaskHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TaskTitleView nameView;
        public TextView dateView;
        public ImageView priorityView;
        public CheckBox checkBox;

        public TaskHolder(View itemView) {
            super(itemView);

            nameView = (TaskTitleView) itemView.findViewById(R.id.text_description);
            dateView = (TextView) itemView.findViewById(R.id.text_date);
            priorityView = (ImageView) itemView.findViewById(R.id.priority);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);

            itemView.setOnClickListener(this);
            checkBox.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == checkBox) {
                completionToggled(this);
            } else {
                postItemClick(this);
            }
        }
    }

    private Cursor mCursor;
    private OnItemClickListener mOnItemClickListener;
    private Context mContext;

    public TaskAdapter(Cursor cursor) {
        mCursor = cursor;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private void completionToggled(TaskHolder holder) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemToggled(holder.checkBox.isChecked(), holder.getAdapterPosition());
        }
    }

    private void postItemClick(TaskHolder holder) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(holder.itemView, holder.getAdapterPosition());
        }
    }

    @Override
    public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View itemView = LayoutInflater.from(mContext)
                .inflate(R.layout.list_item_task, parent, false);

        return new TaskHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TaskHolder holder, int position) {

        //TODO: Bind the task data to the views
        mCursor.moveToPosition(position);

        Task task = new Task(mCursor);
        setState(holder.nameView, task);
        holder.checkBox.setChecked(task.isComplete);

        if (task.dueDateMillis == Long.MAX_VALUE) {
            holder.dateView.setText(R.string.date_empty);
            holder.dateView.setVisibility(View.GONE);
        } else {
            CharSequence formatted = DateUtils.getRelativeTimeSpanString(mContext, task.dueDateMillis);
            holder.dateView.setText(formatted);
            holder.dateView.setVisibility(View.VISIBLE);
        }

        if(task.isPriority){
            holder.priorityView.setImageResource(R.drawable.ic_priority);
        } else {
            holder.priorityView.setImageResource(R.drawable.ic_not_priority);
        }

    }

    private void setState(TaskTitleView nameView, Task task) {
        nameView.setText(task.description);

        Calendar calNow = Calendar.getInstance();
        Timestamp current_time = new Timestamp(calNow.getTimeInMillis());

        if (task.isComplete) {
            nameView.setState(TaskTitleView.DONE);
        } else if(current_time.after(new Timestamp(task.dueDateMillis))) {
            nameView.setState(TaskTitleView.OVERDUE);
        } else {
            nameView.setState(TaskTitleView.NORMAL);
        }

    }

    @Override
    public int getItemCount() {
        return (mCursor != null) ? mCursor.getCount() : 0;
    }

    /**
     * Retrieve a {@link Task} for the data at the given position.
     *
     * @param position Adapter item position.
     *
     * @return A new {@link Task} filled with the position's attributes.
     */
    public Task getItem(int position) {
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Invalid item position requested");
        }

        return new Task(mCursor);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    public void swapCursor(Cursor cursor) {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = cursor;
        notifyDataSetChanged();
    }
}
