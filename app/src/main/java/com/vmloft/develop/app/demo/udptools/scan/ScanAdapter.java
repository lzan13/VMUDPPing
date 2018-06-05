package com.vmloft.develop.app.demo.udptools.scan;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.vmloft.develop.app.demo.udptools.R;
import java.util.List;

public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ScanHodler> {

    private Context context;
    private LayoutInflater inflater;
    private List<PortBean> portBeanList;

    public ScanAdapter(Context context, List<PortBean> list) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        portBeanList = list;
    }

    @NonNull
    @Override
    public ScanHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_scan_port, parent, false);
        return new ScanHodler(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanHodler holder, int position) {
        PortBean portBean = portBeanList.get(position);
        holder.portView.setText(String.valueOf(portBean.getPort()));
        if (portBean.isOpen()) {
            holder.stateView.setText("开启");
            holder.stateView.setTextColor(ContextCompat.getColor(context, R.color.green));
        } else {
            holder.stateView.setText("关闭");
            holder.stateView.setTextColor(ContextCompat.getColor(context, R.color.red));
        }
    }

    @Override
    public int getItemCount() {
        return portBeanList.size();
    }

    class ScanHodler extends RecyclerView.ViewHolder {

        @BindView(R.id.text_port) TextView portView;
        @BindView(R.id.text_state) TextView stateView;

        public ScanHodler(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
