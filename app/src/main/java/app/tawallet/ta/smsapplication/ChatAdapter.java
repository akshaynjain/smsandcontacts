package app.tawallet.ta.smsapplication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private ArrayList<HashMap<String, String>> smsList;
    private Context mContext;

    ChatAdapter(ArrayList<HashMap<String, String>> smsList, Context context) {
        this.smsList = smsList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HashMap < String, String > sms = smsList.get(position);
        try {
            if(sms.get(Function.KEY_TYPE).contentEquals("1"))
            {
                holder.lblMsgFrom.setText(sms.get(Function.KEY_NAME));
                holder.txtMsgFrom.setText(sms.get(Function.KEY_MSG));
                holder.timeMsgFrom.setText(sms.get(Function.KEY_TIME));
                holder.msgFrom.setVisibility(View.VISIBLE);
                holder.msgYou.setVisibility(View.GONE);
            }else{
                holder.lblMsgYou.setText(R.string.sub_title_you);
                holder.txtMsgYou.setText(sms.get(Function.KEY_MSG));
                holder.timeMsgYou.setText(sms.get(Function.KEY_TIME));
                holder.msgFrom.setVisibility(View.GONE);
                holder.msgYou.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout msgFrom, msgYou;
        TextView txtMsgYou, lblMsgYou, timeMsgYou, lblMsgFrom, txtMsgFrom, timeMsgFrom;
        ViewHolder(View itemView) {
            super(itemView);
            txtMsgYou = itemView.findViewById(R.id.txtMsgYou);
            lblMsgYou = itemView.findViewById(R.id.lblMsgYou);
            timeMsgYou = itemView.findViewById(R.id.timeMsgYou);
            lblMsgFrom = itemView.findViewById(R.id.lblMsgFrom);
            timeMsgFrom = itemView.findViewById(R.id.timeMsgFrom);
            txtMsgFrom = itemView.findViewById(R.id.txtMsgFrom);
            msgFrom = itemView.findViewById(R.id.msgFrom);
            msgYou = itemView.findViewById(R.id.msgYou);
        }
    }
    
    @Override
    public int getItemCount() {
        return smsList.size();
    }
}
