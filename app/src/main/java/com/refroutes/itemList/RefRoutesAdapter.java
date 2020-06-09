package com.refroutes.itemList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.refroutes.R;
import com.refroutes.model.RefRoute;

import java.util.List;

public class RefRoutesAdapter extends RecyclerView.Adapter<RefRoutesAdapter.ViewHolder> {
    private List<RefRoute> mRefRoutes;

    private ItemClickListener itemClickListenerVar;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView refRouteNameTextView;
        public TextView refRouteDescriptionTextView;
        public CheckBox refRouteCheckBox;
        public ImageButton refRouteDeleteButton;

        public ViewHolder (View itemView) {
            super (itemView);
            refRouteNameTextView = (TextView) itemView.findViewById(R.id.itemRefRouteName);
            refRouteCheckBox = (CheckBox) itemView.findViewById(R.id.itemCheckBox);
            refRouteDeleteButton = (ImageButton) itemView.findViewById(R.id.itemDeleteButton);
        }
    }

    public RefRoutesAdapter(List<RefRoute> refRoutes, ItemClickListener itemClickListener) {
        mRefRoutes = refRoutes;
        itemClickListenerVar = itemClickListener;
    }

    @Override
    public RefRoutesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View refRouteView = inflater.inflate(R.layout.item_refroute, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(refRouteView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RefRoutesAdapter.ViewHolder viewHolder, final int position) {
        // Get the data model based on position
        RefRoute refRoute = mRefRoutes.get(position);
        final int finalVar = position;

        // Set item views based on your views and data model
        TextView textView = viewHolder.refRouteNameTextView;
        textView.setText(refRoute.getRefRouteName());
        CheckBox checkBox = viewHolder.refRouteCheckBox;
        checkBox.setChecked(refRoute.isActive());
        checkBox.setText("aktiv");
        ImageButton imageButton = viewHolder.refRouteDeleteButton;

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListenerVar.itemClicked(v, finalVar);
            }
        };
        textView.setOnClickListener(onClickListener);
        checkBox.setOnClickListener(onClickListener);
        imageButton.setOnClickListener(onClickListener);
    }

    public interface ItemClickListener {
        public void itemClicked(View view, int index);
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRefRoutes.size();
    }

    @Override
    public long getItemId(int position) {
        return mRefRoutes.get(position).getRefRouteIndex();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}