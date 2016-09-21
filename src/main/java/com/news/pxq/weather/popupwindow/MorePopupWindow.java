package com.news.pxq.weather.popupwindow;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.news.pxq.weather.R;
import com.news.pxq.weather.activity.EditCityActivity;
import com.news.pxq.weather.activity.SettingsActivity;

/**
 * Created by pxq on 2016/8/1.
 */
public class MorePopupWindow extends PopupWindow {

    private View mContentView;

    private static MorePopupWindow mMorePopupWindow;

    private MorePopupWindow(final Activity context){
        mContentView = LayoutInflater.from(context).inflate(R.layout.more_popup_dialog, null);

        //设置popupwindow的宽和高
        DisplayMetrics dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int w = dm.widthPixels;
        int popupW = w / 3;

        //final PopupWindow mPopupWindow = new PopupWindow(mContentView, popupW, Toolbar.LayoutParams.WRAP_CONTENT, true);
        setContentView(mContentView);
        setWidth(popupW);
        setHeight(Toolbar.LayoutParams.WRAP_CONTENT);

        //设置可点击
        //setTouchable(true);
        setFocusable(true);

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        ColorDrawable cd = new ColorDrawable(000);   //透明
        setBackgroundDrawable(cd);

        //具体操作
        LinearLayout aboutLin = (LinearLayout) mContentView.findViewById(R.id.about_lin);
        aboutLin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "关于", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout editLin = (LinearLayout) mContentView.findViewById(R.id.edit_lin);
        editLin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, EditCityActivity.class);
                context.startActivity(intent);
                context.finish();
                dismiss();   //隐藏popupwindow
                return;
            }
        });

        LinearLayout setLin = (LinearLayout) mContentView.findViewById(R.id.set_lin);
        setLin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SettingsActivity.class);
                context.startActivity(intent);
                //context.finish();
                dismiss();   //隐藏popupwindow
                return;
            }
        });

        LinearLayout quitLin = (LinearLayout) mContentView.findViewById(R.id.quit_lin);
        quitLin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });

    }

    public static MorePopupWindow getInstance(Activity context){
        if (mMorePopupWindow == null){
            mMorePopupWindow = new MorePopupWindow(context);
        }

        return mMorePopupWindow;
    }
    public void showMorePopupWindow(View view){
        if (!this.isShowing()) {
            int locateW = view.getLayoutParams().width;
            int locateH = view.getLayoutParams().height;
            this.showAsDropDown(view, locateW, locateH);
        } else
            this.dismiss();
    }
}
