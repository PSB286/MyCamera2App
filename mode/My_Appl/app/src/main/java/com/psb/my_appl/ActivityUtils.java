package com.psb.my_appl;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ActivityUtils {

    /**
     * Adds the given {@code fragment} to the container view with id {@code frameId}.
     * The operation is performed by the {@code fragmentManager}.
     *
     * @param fragmentManager The FragmentManager used to manage the fragment transactions.
     * @param fragment        The Fragment to be added.
     * @param frameId         The ID of the container view where the fragment will be added.
     */
    @SuppressLint("RestrictedApi")
    public static void addFragmentToActivity(@NonNull FragmentManager fragmentManager,
                                             @NonNull Fragment fragment, int frameId) {
        checkNotNull(fragmentManager, "FragmentManager cannot be null");
        checkNotNull(fragment, "Fragment cannot be null");

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(frameId, fragment);
        transaction.commitAllowingStateLoss(); // Avoid IllegalStateException
    }

    /**
     * Checks if the given object is not null.
     * If it is null, throws a NullPointerException.
     *
     * @param obj The object to check.
     * @param message The error message to throw if the object is null.
     */
    private static void checkNotNull(@Nullable Object obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
    }
}
