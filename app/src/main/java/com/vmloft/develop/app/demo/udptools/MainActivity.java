package com.vmloft.develop.app.demo.udptools;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.vmloft.develop.app.demo.udptools.scan.ScanFragment;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.widget_tab_layout) TabLayout tabLayout;
    @BindView(R.id.widget_view_pager) ViewPager viewPager;

    private Activity activity;
    private String version = "1.1.0";
    // TabLayout 装填的内容
    private String tabTitles[] = null;
    private Fragment fragments[];
    private PingFragment pingFragment;
    private ScanFragment scanFragment;
    private int currentTabIndex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;

        ButterKnife.bind(activity);

        init();
    }

    private void init() {
        toolbar.setTitle("UDPTools " + version);
        toolbar.setTitleTextColor(ContextCompat.getColor(activity, R.color.white));
        setSupportActionBar(toolbar);

        currentTabIndex = 0;
        tabTitles = new String[] { "Ping包", "端口扫描" };

        pingFragment = PingFragment.newInstance();
        scanFragment = ScanFragment.newInstance();
        fragments = new Fragment[] {
            pingFragment, scanFragment
        };
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), fragments, tabTitles);
        viewPager.setAdapter(adapter);
        // 设置 ViewPager 缓存个数
        viewPager.setOffscreenPageLimit(1);
        viewPager.setCurrentItem(currentTabIndex);
        // 添加 ViewPager 页面改变监听
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {}

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        tabLayout.setupWithViewPager(viewPager);
    }

    /**
     * 自定义 ViewPager 适配器子类
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {

        private String mTabTitles[];
        private Fragment mFragments[];

        public ViewPagerAdapter(FragmentManager fm, Fragment fragments[], String titles[]) {
            super(fm);
            mFragments = fragments;
            mTabTitles = titles;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabTitles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments[position];
        }

        @Override
        public int getCount() {
            return mTabTitles.length;
        }
    }
}
