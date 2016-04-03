package com.chamika.fbmsgbackup.adapters;

import java.util.ArrayList;
import java.util.List;

import com.chamika.fbmsgbackup.fragments.MessageOptionFragment;
import com.chamika.fbmsgbackup.fragments.StorageOptionFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class OptionsFragmentAdapter extends FragmentPagerAdapter {

	List<Fragment> fragments;

	public OptionsFragmentAdapter(FragmentManager fm) {
		super(fm);
		fragments = new ArrayList<Fragment>();
		fragments.add(new MessageOptionFragment());
		fragments.add(new StorageOptionFragment());

	}

	@Override
	public Fragment getItem(int position) {
		if (position < fragments.size()) {
			return fragments.get(position);
		}

		return null;
	}

	@Override
	public int getCount() {
		return fragments.size();
	}

}
