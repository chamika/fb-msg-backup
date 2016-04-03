package com.chamika.fbmsgbackup.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.model.FBFriend;
import com.chamika.fbmsgbackup.model.FBThread;
import com.chamika.fbmsgbackup.utils.DataStorage;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;

public class ThreadsListAdapter extends EndlessThreadAdapter<FBThread> {

	static int resouceId = R.layout.list_view_thread_row;

	private Context context;
	private LayoutInflater inflater;

	private List<FBThread> list;

	public ThreadsListAdapter(Context context, List<FBThread> objects) {
		super(context, resouceId, objects);
		this.context = context;
		list = objects;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View view = null;

		if (position == list.size()) {
			return super.getView(position, convertView, parent);
		}

		if (convertView != null) {
			view = convertView;
		} else {
			view = inflater.inflate(resouceId, null);
			ViewHolder holder = new ViewHolder();
			holder.textReceipients = (TextView) view.findViewById(R.id.textViewRecipients);
			holder.textCount = (TextView) view.findViewById(R.id.textviewCount);
			holder.textSnippet = (TextView) view.findViewById(R.id.textViewSnippet);

			ViewGroup imageParent = (ViewGroup) view.findViewById(R.id.images_parent);

			holder.firstRow = (ViewGroup) imageParent.findViewById(R.id.images_first_row);
			holder.secondRow = (ViewGroup) imageParent.findViewById(R.id.images_second_row);

			holder.pic1 = (ImageView) holder.firstRow.findViewById(R.id.profile_pic_1);
			holder.pic2 = (ImageView) holder.firstRow.findViewById(R.id.profile_pic_2);
			holder.pic3 = (ImageView) holder.secondRow.findViewById(R.id.profile_pic_3);
			holder.pic4 = (TextView) holder.secondRow.findViewById(R.id.profile_4);

			view.setTag(holder);
		}

		FBThread thread = list.get(position);

		ViewHolder holder = (ViewHolder) view.getTag();

		List<FBFriend> friends = thread.getFriends(DataStorage.getLoggedUser());

		if (friends != null) {
			int size = friends.size();

			if (size == 0) {
				hide(holder.firstRow);
				hide(holder.secondRow);
			} else if (size == 1) {
				show(holder.firstRow);
				hide(holder.secondRow);

				show(holder.pic1);
				hide(holder.pic2);

				loadImage(holder.pic1, friends.get(0).getThumbUrl());

			} else if (size == 2) {
				show(holder.firstRow);
				hide(holder.secondRow);
				show(holder.pic1);
				show(holder.pic2);

				loadImage(holder.pic1, friends.get(0).getThumbUrl());
				loadImage(holder.pic2, friends.get(1).getThumbUrl());

			} else if (size == 3) {
				show(holder.firstRow);
				show(holder.secondRow);
				show(holder.pic1);
				show(holder.pic2);
				show(holder.pic3);
				hide(holder.pic4);

				loadImage(holder.pic1, friends.get(0).getThumbUrl());
				loadImage(holder.pic2, friends.get(1).getThumbUrl());
				loadImage(holder.pic3, friends.get(2).getThumbUrl());

			} else {
				show(holder.firstRow);
				show(holder.secondRow);

				show(holder.pic1);
				show(holder.pic2);
				show(holder.pic3);
				show(holder.pic4);

				loadImage(holder.pic1, friends.get(0).getThumbUrl());
				loadImage(holder.pic2, friends.get(1).getThumbUrl());
				loadImage(holder.pic3, friends.get(2).getThumbUrl());

				holder.pic4.setText(String.valueOf(size - 3));
			}

			StringBuilder sb = new StringBuilder();

			if (size > 0) {

				for (int i = 0; i < size; i++) {
					if (i != 0) {
						sb.append(", ");
					}

					if (size == 1) {
						sb.append(friends.get(i).getName());
					} else {
						sb.append(friends.get(i).getFirstName());
					}
				}
			} else {
				sb.append(context.getResources().getString(R.string.unknown));
			}

			holder.textReceipients.setText(sb.toString());
			String snippet = thread.getSnippetWithFriend();
			if (snippet != null) {
				holder.textSnippet.setText(snippet);
				holder.textSnippet.setVisibility(View.VISIBLE);
			} else {
				holder.textSnippet.setVisibility(View.GONE);
			}
		}

		holder.textCount.setText(String.valueOf(thread.getMessageCount()));

		return view;
	}

	private void hide(View view) {
		view.setVisibility(View.GONE);
	}

	private void show(View view) {
		view.setVisibility(View.VISIBLE);
	}

	private void loadImage(ImageView view, String url) {
		DisplayImageOptions options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.ic_launcher)
				.showImageForEmptyUri(R.drawable.ic_launcher).showImageOnFail(R.drawable.ic_launcher)
				.cacheInMemory(true).cacheOnDisc(true).imageScaleType(ImageScaleType.EXACTLY)
				.bitmapConfig(Bitmap.Config.RGB_565).displayer(new SimpleBitmapDisplayer()).build();

		ImageLoader.getInstance().displayImage(url, view, options);
	}

	static class ViewHolder {
		TextView textReceipients;
		TextView textCount;
		TextView textSnippet;
		ImageView pic1;
		ImageView pic2;
		ImageView pic3;
		TextView pic4;
		ViewGroup firstRow;
		ViewGroup secondRow;
	}
}
