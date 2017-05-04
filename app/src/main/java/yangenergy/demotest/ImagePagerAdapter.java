package yangenergy.demotest;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by Admin on 04-05-2017.
 */
    public class ImagePagerAdapter extends PagerAdapter {

    private final Context context;

    public ImagePagerAdapter(Context context ) {
        this.context = context;
    }

    private int[] mImages = new int[] {
            R.drawable.p5,
            R.drawable.p2,
            R.drawable.p3,
            R.drawable.p4,
            R.drawable.p1
    };

    @Override
    public int getCount() {
        return mImages.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ( object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_benner_layout,container,false);

//        View view = inflater.inflate(R.layout.fragment_benner_layout, container, false);
        ImageView imageView =(ImageView) view.findViewById(R.id.imageView);
        //imageView
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
//        int padding = context.getResources().getDimensionPixelSize(
//                R.dimen.padding_medium);
//        imageView.setPadding(padding, padding, padding, padding);
        imageView.setImageResource(mImages[position]);
        ((ViewPager) container).addView(view);
        return view;
    }
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
//        ((ViewPager) container).removeView((ImageView) object);
    }
}
