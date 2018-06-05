package com.vmloft.develop.app.demo.udptools.scan;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ScanItemDecoration extends RecyclerView.ItemDecoration {

    private int space;

    public ScanItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        //super.getItemOffsets(outRect, view, parent, state);
        outRect.top = space;
        outRect.right = space;
        outRect.bottom = space;
        outRect.left = space;
    }
}
