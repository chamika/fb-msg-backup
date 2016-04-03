package com.chamika.fbmsgbackup.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.model.FBFriend;
import com.chamika.fbmsgbackup.model.FBMessage;
import com.chamika.fbmsgbackup.utils.DataStorage;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;

public class MessagesListAdapter extends ArrayAdapter<FBMessage> {

	static int resouceId = R.layout.list_view_msg_row;

	private LayoutInflater inflater;

	private List<FBMessage> list;

	public MessagesListAdapter(Context context, List<FBMessage> objects) {
		super(context, resouceId, objects);
		list = objects;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View view = null;

		if (convertView != null) {
			view = convertView;
		} else {
			view = inflater.inflate(resouceId, null);
			ViewHolder holder = new ViewHolder();
			holder.textBody = (TextView) view.findViewById(R.id.text_body);
			holder.textTime = (TextView) view.findViewById(R.id.text_time);
			holder.pic = (ImageView) view.findViewById(R.id.profile_pic);

			view.setTag(holder);
		}

		FBMessage msg = list.get(position);

		ViewHolder holder = (ViewHolder) view.getTag();

		holder.textBody.setText(msg.getBody());
		holder.textTime.setText(msg.getFormattedTime());

		try {
			FBFriend author = DataStorage.getAllFriends().get(Long.parseLong(msg.getAuthor()));

			if (author != null) {
				loadImage(holder.pic, author.getThumbUrl());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return view;
	}

	private void loadImage(ImageView view, String url) {
		DisplayImageOptions options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.ic_launcher)
				.showImageForEmptyUri(R.drawable.ic_launcher).showImageOnFail(R.drawable.ic_launcher)
				.cacheInMemory(true).cacheOnDisc(true).imageScaleType(ImageScaleType.EXACTLY)
				.bitmapConfig(Bitmap.Config.RGB_565).displayer(new SimpleBitmapDisplayer()).build();

		ImageLoader.getInstance().displayImage(url, view, options);
	}

	static class ViewHolder {
		TextView textBody;
		TextView textTime;
		ImageView pic;
	}
}
