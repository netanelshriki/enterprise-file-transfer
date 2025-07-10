package com.enterprise.filetransfer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.enterprise.filetransfer.R;
import com.enterprise.filetransfer.model.Device;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<Device> devices;
    private OnDeviceClickListener listener;
    private Device selectedDevice;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    public DeviceAdapter(List<Device> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.bind(device, device.equals(selectedDevice));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateDevices(List<Device> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }

    public void setSelectedDevice(Device device) {
        Device previousSelected = this.selectedDevice;
        this.selectedDevice = device;
        
        if (previousSelected != null) {
            int previousIndex = devices.indexOf(previousSelected);
            if (previousIndex != -1) {
                notifyItemChanged(previousIndex);
            }
        }
        
        int newIndex = devices.indexOf(device);
        if (newIndex != -1) {
            notifyItemChanged(newIndex);
        }
    }

    public Device getSelectedDevice() {
        return selectedDevice;
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDeviceName;
        private TextView tvDeviceInfo;
        private TextView tvDeviceStatus;
        private ImageView ivDeviceIcon;
        private View viewStatusIndicator;
        private View itemView;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceInfo = itemView.findViewById(R.id.tvDeviceInfo);
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
            viewStatusIndicator = itemView.findViewById(R.id.viewStatusIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeviceClick(devices.get(position));
                }
            });
        }

        public void bind(Device device, boolean isSelected) {
            tvDeviceName.setText(device.getName());
            tvDeviceInfo.setText(device.getIpAddress() + ":" + device.getPort());
            tvDeviceStatus.setText(device.getStatusText());

            switch (device.getType()) {
                case DESKTOP:
                    ivDeviceIcon.setImageResource(R.drawable.ic_device);
                    break;
                case MOBILE:
                    ivDeviceIcon.setImageResource(R.drawable.ic_device);
                    break;
                default:
                    ivDeviceIcon.setImageResource(R.drawable.ic_device);
                    break;
            }

            int statusColor;
            switch (device.getStatus()) {
                case ONLINE:
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_online);
                    break;
                case CONNECTING:
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_connecting);
                    break;
                case CONNECTED:
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_online);
                    break;
                case OFFLINE:
                default:
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_offline);
                    break;
            }
            viewStatusIndicator.setBackgroundTintList(
                ContextCompat.getColorStateList(itemView.getContext(), statusColor));

            if (isSelected) {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), R.color.primary_blue_light));
                itemView.setAlpha(0.8f);
            } else {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
                itemView.setAlpha(1.0f);
            }
        }
    }
}
