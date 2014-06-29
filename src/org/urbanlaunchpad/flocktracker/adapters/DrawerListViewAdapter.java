package org.urbanlaunchpad.flocktracker.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.menu.RowItem;

import java.util.List;

public class DrawerListViewAdapter extends ArrayAdapter<RowItem> {

  Context context;

  public DrawerListViewAdapter(Context context, int resourceId,
      List<RowItem> items) {
    super(context, resourceId, items);
    this.context = context;
  }

  public View getView(int position, View convertView, ViewGroup parent) {
    ViewHolder holder = null;
    RowItem rowItem = getItem(position);

    LayoutInflater mInflater = (LayoutInflater) context
        .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    if (convertView == null) {
      convertView = mInflater.inflate(R.layout.chapter_drawer_list_item, null);
      holder = new ViewHolder();
      holder.txtTitle = (TextView) convertView.findViewById(R.id.list_text);
      holder.imageView = (ImageView) convertView.findViewById(R.id.list_image);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    holder.txtTitle.setText(rowItem.getTitle());
    holder.imageView.setImageResource(rowItem.getImageId());

    return convertView;
  }

  private class ViewHolder {

    ImageView imageView;
    TextView txtTitle;
  }
}