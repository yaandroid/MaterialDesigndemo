package yangenergy.demotest;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Admin on 04-05-2017.
 */
public class FragmentListAdapter extends RecyclerView.Adapter<FragmentListAdapter.ItemHolder> {


    private ArrayList<ItemGetSet> lists;
    private Context context;

    public FragmentListAdapter(ArrayList<ItemGetSet> lists, Context context) {
        this.lists = lists;
        this.context = context;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_list_item,parent,false);
        return new ItemHolder(view);    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        holder.title_txt.setText(lists.get(position).getTitle_name());
        holder.details_txt.setText(lists.get(position).getDetails());
        Log.v("Sandeep kushwah ","adapter "+lists.get(position).getTitle_name());

    }

    @Override
    public int getItemCount() {
        return lists.size();
    }

    public class ItemHolder extends RecyclerView.ViewHolder{
        private final ImageView item_img;
        private final TextView title_txt,details_txt;

        public ItemHolder(View itemView) {
            super(itemView);
            item_img=(ImageView)itemView.findViewById(R.id.item_img);
            title_txt= (TextView)itemView.findViewById(R.id.item_title);
            details_txt=(TextView)itemView.findViewById(R.id.item_details);
        }
    }
}
