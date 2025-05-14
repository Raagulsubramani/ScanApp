package com.gmscan.activity;

import static com.gmscan.GmScanApplication.getPreferenceManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.gmscan.R;
import com.gmscan.adapter.ViewPagerAdapter;
import com.zhpan.indicator.IndicatorView;
import com.zhpan.indicator.enums.IndicatorSlideMode;
import com.zhpan.indicator.enums.IndicatorStyle;

import java.util.Arrays;
import java.util.List;

public class OnBoardActivity extends AppCompatActivity {
    private Button btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_onboard);
        initializeView();
        setClickListeners();
    }

    /**
     * Initializes all views in the activity.
     */
    private void initializeView() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        IndicatorView indicatorView = findViewById(R.id.indicator_view);
        btnSkip = findViewById(R.id.btnSkip);

        List<Integer> pages = Arrays.asList(R.layout.layout_onboard_page_one, R.layout.layout_onboard_page_two, R.layout.layout_onboard_page_three);

        ViewPagerAdapter adapter = new ViewPagerAdapter(pages);
        viewPager.setAdapter(adapter);

        indicatorView.setSliderColor(getColor(R.color.deselected_color), getColor(R.color.purple_500)).setSlideMode(IndicatorSlideMode.WORM).setIndicatorStyle(IndicatorStyle.ROUND_RECT).setPageSize(pages.size()).setupWithViewPager(viewPager);
    }

    /**
     * Sets click listeners for all buttons.
     */
    private void setClickListeners() {
        btnSkip.setOnClickListener(v -> {
            getPreferenceManager().setOnboardingCompleted(true);
            Intent intent;
            boolean isLoggedIn = getPreferenceManager().isLoggedIn();
            if (!isLoggedIn) {
                intent = new Intent(OnBoardActivity.this, LoginActivity.class);
            } else {
                intent = new Intent(OnBoardActivity.this, SelectDocumentsActivity.class);
            }
            startActivity(intent);
            finish();
        });
    }
}

