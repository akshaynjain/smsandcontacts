package app.tawallet.ta.smsapplication;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import app.tawallet.ta.smsapplication.ItemFragment.OnListFragmentInteractionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {


    private ArrayList<HashMap< String, String >> data;
    private final OnListFragmentInteractionListener mListener;

    public MyItemRecyclerViewAdapter(ArrayList<HashMap< String, String >> data, OnListFragmentInteractionListener listener) {
        this.data = data;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        HashMap < String, String > sms = new HashMap < String, String > ();
        sms = data.get(position);

        holder.inbox_user.setText(sms.get(Function.KEY_NAME));
        holder.inbox_msg.setText(sms.get(Function.KEY_MSG));
        holder.inbox_date.setText(sms.get(Function.KEY_TIME));

        String firstLetter = String.valueOf(sms.get(Function.KEY_NAME).charAt(0));
        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(holder.getAdapterPosition());
        TextDrawable drawable = TextDrawable.builder().buildRound(firstLetter, color);
        holder.inbox_thumb.setImageDrawable(drawable);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    HashMap < String, String > sms = data.get(holder.getAdapterPosition());
                    mListener.onListFragmentInteraction(sms);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView inbox_thumb;
        TextView inbox_user, inbox_msg, inbox_date;

        public ViewHolder(View view) {
            super(view);
            inbox_thumb = (ImageView) view.findViewById(R.id.inbox_thumb);
            inbox_user = (TextView) view.findViewById(R.id.inbox_user);
            inbox_msg = (TextView) view.findViewById(R.id.inbox_msg);
            inbox_date = (TextView) view.findViewById(R.id.inbox_date);
        }


    }
}
