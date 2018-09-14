package com.arcsoft.sdk_demo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ItemHolder> {

    private Context context;
    private List<ResultBean> list;
    private int degree;
    private boolean isMirror;

    public ResultAdapter(Context context, List<ResultBean> list, int degree, boolean isMirror) {
        this.context = context;
        this.list = list;
        this.degree = degree;
        this.isMirror = isMirror;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(context).inflate(R.layout.item_result, parent, false);
        return new ItemHolder(item);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        ResultBean bean = list.get(position);
        holder.textView.setText(bean.name);
        holder.textView1.setText(bean.percent);
        holder.imageView.setRotation(degree);
        if (isMirror) {
            holder.imageView.setScaleY(-1);
        }
        holder.imageView.setImageBitmap(bean.bitmap);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ItemHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView textView;
        TextView textView1;

        private void assignViews(View itemView) {
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            textView = (TextView) itemView.findViewById(R.id.textView);
            textView1 = (TextView) itemView.findViewById(R.id.textView1);
        }

        public ItemHolder(View itemView) {
            super(itemView);
            assignViews(itemView);
        }
    }
}
