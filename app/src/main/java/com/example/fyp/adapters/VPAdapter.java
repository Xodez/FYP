package com.example.fyp.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.fyp.InventoryFragment;
import com.example.fyp.RecipeFragment;
import com.example.fyp.ShoppingListFragment;

import java.util.ArrayList;

public class VPAdapter extends FragmentStateAdapter {

    public VPAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public VPAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public VPAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 1:
                return new InventoryFragment();
            case 2:
                return new ShoppingListFragment();
        }
        return new RecipeFragment();
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
