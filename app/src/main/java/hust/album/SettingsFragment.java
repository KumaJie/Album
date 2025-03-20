package hust.album;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.bumptech.glide.Glide;

import java.io.File;

public class SettingsFragment extends PreferenceFragmentCompat {

    private OnBackInvokedCallback backInvokedCallback;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_setting, rootKey);

        Preference resetCacheButton = findPreference("force_reset_cache");
        if (resetCacheButton != null) {
            resetCacheButton.setOnPreferenceClickListener(preference -> {
                File cacheDir = requireContext().getExternalCacheDir();
                if (deleteDirectory(cacheDir)) {
                    Toast.makeText(requireContext(), "缓存已被清理", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "缓存清理失败", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference resetCompressedButton = findPreference("force_reset_compressed");
        if (resetCompressedButton != null) {
            resetCompressedButton.setOnPreferenceClickListener(preference -> {
                File compressedDir = requireContext().getExternalFilesDir(null);
                if (deleteDirectory(compressedDir)) {
                    Toast.makeText(requireContext(), "压缩文件已被清理", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "压缩文件清理失败", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference resetGlideButton = findPreference("force_reset_glide");
        if (resetGlideButton != null) {
            resetGlideButton.setOnPreferenceClickListener(preference -> {
                new Thread(() -> {
                    Glide.get(requireContext()).clearDiskCache();
                    Glide.get(requireContext()).clearMemory();
                }).start();
                Toast.makeText(requireContext(), "Glide缓存已被清理", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        backInvokedCallback = () -> ((MainActivity)requireActivity()).back();
        requireActivity().getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                backInvokedCallback);
    }


    public static boolean deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }

        if (!directory.isDirectory()) {
            return false;
        }

        // 获取目录中的所有文件和子目录
        File[] files = directory.listFiles();
        if (files != null) { // 如果目录非空
            for (File file : files) {
                if (file.isFile()) {
                    // 如果是文件，直接删除
                    file.delete();
                } else if (file.isDirectory()) {
                    // 如果是子目录，递归删除
                    deleteDirectory(file);
                }
            }
        }

        // 删除空目录
        return directory.delete();
    }

}
