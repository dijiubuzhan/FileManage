package com.ui.myapplication;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import static com.ui.myapplication.R.layout.item;

/**
 * Created by wilson on 2017/4/18.
 */

public class RecycleAdapter extends RecyclerView.Adapter {
    private Context context;

    private List<String> mDatas;
    private LayoutInflater m_layoutInflater;
    private OnItemClickListener mOnItemClickListener;

    public RecycleAdapter(Context context, List<String> mDatas) {
        this.mDatas = mDatas;
        this.context = context;
        m_layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemViewHolder holder = new ItemViewHolder(m_layoutInflater.inflate(item, parent, false));
        return holder;
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewholder, int position) {
        ItemViewHolder holder = (ItemViewHolder) viewholder;
        holder.m_text.setText(mDatas.get(position));
        if(mOnItemClickListener != null) {
            /**
             * 这里加了判断，itemViewHolder.itemView.hasOnClickListeners()
             * 目的是减少对象的创建，如果已经为view设置了click监听事件,就不用重复设置了
             * 不然每次调用onBindViewHolder方法，都会创建两个监听事件对象，增加了内存的开销
             */
            if(!viewholder.itemView.hasOnClickListeners()) {
                Log.d("ListAdapter", "setOnClickListener");
                viewholder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = viewholder.getPosition();
                        mOnItemClickListener.onItemClick(v, pos);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView m_text;
        private ImageView m_imageView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            m_imageView = (ImageView) itemView.findViewById(R.id.img);
            m_text = (TextView) itemView.findViewById(R.id.txt);
        }
    }

    interface OnItemClickListener {
         void onItemClick(View view, int position);
     }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public void refresh(int position){
        notifyItemChanged(position);

    }

}
