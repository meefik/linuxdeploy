package ru.meefik.linuxdeploy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.meefik.linuxdeploy.R;
import ru.meefik.linuxdeploy.model.RepositoryProfile;

public class RepositoryProfileAdapter extends RecyclerView.Adapter<RepositoryProfileAdapter.ViewHolder> {

    private List<RepositoryProfile> repositoryProfiles;
    private OnItemClickListener listener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.repository_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setRepository(repositoryProfiles.get(position));
    }

    @Override
    public int getItemCount() {
        return repositoryProfiles != null ? repositoryProfiles.size() : 0;
    }

    public void setRepositoryProfiles(List<RepositoryProfile> repositoryProfiles) {
        this.repositoryProfiles = repositoryProfiles;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private View view;
        private TextView title;
        private TextView subTitle;
        private ImageView icon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            view = itemView;
            title = itemView.findViewById(R.id.repo_entry_title);
            subTitle = itemView.findViewById(R.id.repo_entry_subtitle);
            icon = itemView.findViewById(R.id.repo_entry_icon);
        }

        public void setRepository(RepositoryProfile repositoryProfile) {
            int iconRes = R.raw.linux;
            if (repositoryProfile.getType() != null) {
                switch (repositoryProfile.getType()) {
                    case "alpine":
                        iconRes = R.raw.alpine;
                        break;
                    case "archlinux":
                        iconRes = R.raw.archlinux;
                        break;
                    case "centos":
                        iconRes = R.raw.centos;
                        break;
                    case "debian":
                        iconRes = R.raw.debian;
                        break;
                    case "fedora":
                        iconRes = R.raw.fedora;
                        break;
                    case "kali":
                        iconRes = R.raw.kali;
                        break;
                    case "slackware":
                        iconRes = R.raw.slackware;
                        break;
                    case "ubuntu":
                        iconRes = R.raw.ubuntu;
                        break;
                }
            }

            icon.setImageResource(iconRes);
            title.setText(repositoryProfile.getProfile());
            if (repositoryProfile.getDescription() != null && !repositoryProfile.getDescription().isEmpty())
                subTitle.setText(repositoryProfile.getDescription());
            else
                subTitle.setText(view.getContext().getString(R.string.repository_default_description));

            view.setOnClickListener(v -> {
                if (listener != null)
                    listener.onClick(repositoryProfile);
            });
        }
    }

    public interface OnItemClickListener {
        void onClick(RepositoryProfile repositoryProfile);
    }
}
