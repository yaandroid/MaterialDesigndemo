package yangenergy.demotest;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

/**
 * Created by Admin on 04-05-2017.
 */
public class CategoryPageFragment extends FragmentStatePagerAdapter {

    private ArrayList<String> pageTitles;
    private ArrayList<Fragment> arrFragments;

    public CategoryPageFragment(FragmentManager fm) {
        super(fm);
        arrFragments = new ArrayList<>();
        pageTitles = new ArrayList<>();
    }

    /**
     *
     * @param position
     * @return
     */
    @Override
    public Fragment getItem(int position) {
        return arrFragments.get(position);

    }

    /***
     *
     * @return
     */
    @Override
    public int getCount() {
        return arrFragments.size();
    }

    /***
     * Add title on page tab
     * @param position
     * @return
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return pageTitles.get(position);
    }

    void addPageTitles(ArrayList<String> titles){
        pageTitles = titles;
    }
    void addFragment(Fragment fragment) {
        arrFragments.add(fragment);
    }
}
