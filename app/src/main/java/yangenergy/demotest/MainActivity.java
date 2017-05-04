package yangenergy.demotest;

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  implements ViewPager.OnPageChangeListener{

    private CollapsingToolbarLayout collapsingToolbarLayout;
    private ImagePagerAdapter adapter;
    private int dotsCount;
    private ImageView[] dots;
    private LinearLayout pager_indicator;
    private CategoryPageFragment pageAdapter;
    private ViewPager mViewPager;
    private TabLayout tabLayout;
    private ArrayList<ItemGetSet> Lists;
    private int ListSize= 40;
    private ListFragments fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        getSupportActionBar().setDisplayShowHomeEnabled(true);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.collapsedappbar);
        collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.expandedappbar);
        mViewPager =(ViewPager) findViewById(R.id.pager);
        tabLayout=(TabLayout)findViewById(R.id.tabs);

        initView();

    }

    private void initView() {
        Lists = new ArrayList<>(32);

        for (int i =0 ; i<= ListSize ; i++){
            ItemGetSet items= new ItemGetSet();
            items.setDetails(i+" More than 20 categories are open for selling on Amazon; products in these categories can be listed without specific permission from Amazon. Some categories only allow listings for new products. Some categories have additional guidelines that sellers must follow. ");
            items.setTitle_name("Item No."+i+" Open Categories");
            Lists.add(items);
        }
        Toast.makeText(this,Lists.size()+" s",Toast.LENGTH_SHORT).show();
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        pager_indicator = (LinearLayout) findViewById(R.id.viewPagerCountDots);


        adapter = new ImagePagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
        viewPager.setOnPageChangeListener(this);
        setUiPageViewController();
        initializeViewPager();

    }

    public void initializeViewPager (){
        pageAdapter = new CategoryPageFragment(getSupportFragmentManager());
        mViewPager.setAdapter(pageAdapter);
        tabLayout.setupWithViewPager(mViewPager);
//        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.colorPrimary));
//        tabLayout.setBackgroundColor(getResources().getColor(R.color.white));
        tabLayout.setTabMode(TabLayout.MODE_FIXED);

        setViewPagerData();

    }

    public void addFragment(ArrayList<ItemGetSet> Lists , Integer module){
        fragment = ListFragments.newInstance(Lists);
        pageAdapter.addFragment(fragment);
    }


    public void setViewPagerData(){
        addFragment(Lists,0);
        addFragment(Lists,1);
        addFragment(Lists,2);
        pageAdapter.addPageTitles(getPageTitles());
        pageAdapter.notifyDataSetChanged();
    }
    private void setUiPageViewController() {

        dotsCount = adapter.getCount();
        dots = new ImageView[dotsCount];

        for (int i = 0; i < dotsCount; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(getResources().getDrawable(R.drawable.nonselecteditem_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(4, 0, 4, 0);

            pager_indicator.addView(dots[i], params);
        }

        dots[0].setImageDrawable(getResources().getDrawable(R.drawable.selecteditem_dot));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        for (int i = 0; i < dotsCount; i++) {
            dots[i].setImageDrawable(getResources().getDrawable(R.drawable.nonselecteditem_dot));
        }

        dots[position].setImageDrawable(getResources().getDrawable(R.drawable.selecteditem_dot));
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
    private ArrayList<String> getPageTitles(){
        ArrayList<String> titles = new ArrayList<>();
        titles.add("VIDEOS");
        titles.add("IMAGES");
        titles.add("MILESTONE");
        return titles;
    }


    private int[] imageResId = {
            R.mipmap.select_video,
            R.mipmap.image,
            R.mipmap.milestone
    };
}
