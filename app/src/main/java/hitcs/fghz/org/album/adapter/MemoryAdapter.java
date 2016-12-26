package hitcs.fghz.org.album.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.List;

import hitcs.fghz.org.album.R;
import hitcs.fghz.org.album.entity.MemoryItem;

/**
 * 回忆栏目的适配器
 * Created by me on 16-12-21.
 */

public class MemoryAdapter extends ArrayAdapter<MemoryItem> {
    private int resourceId;
    public MemoryAdapter(Context context, int textViewResourceId,
                         List<MemoryItem> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MemoryItem photo= getItem(position);
        View view = LayoutInflater.from(getContext()).inflate(resourceId, null);
        ImageView memoryImage = (ImageView) view.findViewById(R.id.memory_photo);
        memoryImage.setImageResource(photo.getImageId());
        return view;
    }
}