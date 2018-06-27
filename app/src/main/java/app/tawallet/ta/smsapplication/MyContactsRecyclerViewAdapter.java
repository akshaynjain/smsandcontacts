package app.tawallet.ta.smsapplication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.ArrayList;

import app.tawallet.ta.smsapplication.ContactsFragment.OnListFragmentInteractionListener;
public class MyContactsRecyclerViewAdapter extends RecyclerView.Adapter<MyContactsRecyclerViewAdapter.ViewHolder> {

    private ArrayList<Contact> listContacts;
    private final OnListFragmentInteractionListener mListener;
    private Context context;

    MyContactsRecyclerViewAdapter(ArrayList<Contact> listContacts, OnListFragmentInteractionListener listener, Context context) {
        this.listContacts=listContacts;
        mListener = listener;
        this.context=context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        holder.inbox_user.setText(listContacts.get(position).getName());
        if (!listContacts.get(position).getNumbers().isEmpty()) {
            holder.inbox_msg.setText(listContacts.get(position).getNumbers().get(0).getNumber());
        }
        String firstLetter = String.valueOf(listContacts.get(position).getName().charAt(0));
        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(holder.getAdapterPosition());
        TextDrawable drawable = TextDrawable.builder().buildRound(firstLetter, color);
        holder.inbox_thumb.setImageDrawable(drawable);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    Contact contact=listContacts.get(holder.getAdapterPosition());
                    mListener.onListFragmentInteraction(contact);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return listContacts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView inbox_thumb;
        TextView inbox_user, inbox_msg;

        ViewHolder(View view) {
            super(view);
            inbox_thumb =  view.findViewById(R.id.inbox_thumb);
            inbox_user =  view.findViewById(R.id.inbox_user);
            inbox_msg =  view.findViewById(R.id.inbox_msg);
        }
    }
}
