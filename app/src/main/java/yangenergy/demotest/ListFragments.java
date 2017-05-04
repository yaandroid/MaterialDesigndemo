package yangenergy.demotest;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Admin on 04-05-2017.
 */
public class ListFragments extends Fragment {

    ArrayList<ItemGetSet> Lists= null;

    private FragmentListAdapter adapter;
    private RecyclerView recycler_view;
    private ArrayList<ItemGetSet> list;

    public static ListFragments newInstance(ArrayList<ItemGetSet> lists) {
        
        Bundle args = new Bundle();
        
        ListFragments fragment = new ListFragments();
        args.putSerializable("list",lists);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       list= (ArrayList<ItemGetSet>)getArguments().get("list");
        View view = inflater.inflate(R.layout.fragment_layout, container, false);
        recycler_view= (RecyclerView)view.findViewById(R.id.recycler_view);
//        Toast.makeText(getActivity(),"On create "+list.get(5).getTitle_name(),Toast.LENGTH_SHORT).show();

        setAdapter();
        return view;
    }
    public void setAdapter () {
        adapter = new FragmentListAdapter(list,getActivity());
        LinearLayoutManager manager = new LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL,false);
        recycler_view.setLayoutManager(manager);
        recycler_view.setAdapter(adapter);
    }
}
