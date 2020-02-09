package ru.meefik.linuxdeploy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.meefik.linuxdeploy.R;
import ru.meefik.linuxdeploy.model.Mount;

public class MountAdapter extends RecyclerView.Adapter<MountAdapter.ViewHolder> {

    private List<Mount> mounts;
    private OnItemClickListener clickListener;
    private OnItemDeleteListener deleteListener;

    public MountAdapter() {
        this.mounts = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mounts_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setMount(mounts.get(position));
    }

    @Override
    public int getItemCount() {
        return mounts == null ? 0 : mounts.size();
    }

    public void addMount(Mount mount) {
        mounts.add(mount);
        notifyDataSetChanged();
    }

    public void removeMount(Mount mount) {
        mounts.remove(mount);
        notifyDataSetChanged();
    }

    public void setMounts(List<String> mounts) {
        this.mounts.clear();
        for (String mount : mounts) {
            String[] tmp = mount.split(":", 2);
            if (tmp.length > 1) {
                this.mounts.add(new Mount(tmp[0], tmp[1]));
            } else {
                this.mounts.add(new Mount(tmp[0], ""));
            }
        }
        notifyDataSetChanged();
    }

    public List<String> getMounts() {
        List<String> mounts = new ArrayList<>();
        for (Mount mount : this.mounts) {
            if (mount.getTarget().isEmpty()) {
                mounts.add(mount.getSource());
            } else {
                mounts.add(mount.getSource() + ":" + mount.getTarget());
            }
        }
        return mounts;
    }

    public void setOnItemClickListener(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public interface OnItemClickListener {
        void onItemClick(Mount mount);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(Mount mount);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private View view;
        private TextView mountPoint;
        private ImageView delete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            view = itemView;
            mountPoint = itemView.findViewById(R.id.mount_point);
            delete = itemView.findViewById(R.id.delete_mount);
        }

        void setMount(Mount mount) {
            if (mount.getTarget().isEmpty()) {
                mountPoint.setText(mount.getSource());
            } else {
                mountPoint.setText(mount.getSource() + " - " + mount.getTarget());
            }

            view.setOnClickListener(v -> {
                if (clickListener != null)
                    clickListener.onItemClick(mount);
            });

            delete.setOnClickListener(v -> {
                if (deleteListener != null)
                    deleteListener.onItemDelete(mount);
            });
        }
    }
}
