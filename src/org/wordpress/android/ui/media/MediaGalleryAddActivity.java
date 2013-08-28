package org.wordpress.android.ui.media;

import java.util.ArrayList;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.MultiSelectGridView;
import org.wordpress.android.ui.MultiSelectGridView.MultiSelectListener;

public class MediaGalleryAddActivity extends SherlockActivity implements MultiSelectListener, Callback {

    private MultiSelectGridView mGridView;
    private MediaGridAdapter mAdapter;
    private ActionMode mActionMode;

    private ArrayList<String> mFilteredItems;
    
    private static final String STATE_FILTERED_ITEMS = "STATE_FILTERED_ITEMS";
    private static final String STATE_SELECTED_ITEMS = "STATE_SELECTED_ITEMS";
    
    public static final int REQUEST_CODE = 1000;
    public static final String PARAM_FILTERED_IDS = "PARAM_FILTERED_IDS";
    public static final String RESULT_IDS = "RESULT_IDS";
    public static final String TAG = MediaGalleryAddActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> checkedItems = new ArrayList<String>();
        mFilteredItems = getIntent().getStringArrayListExtra(PARAM_FILTERED_IDS);
        if (savedInstanceState != null) {
            checkedItems.addAll(savedInstanceState.getStringArrayList(STATE_SELECTED_ITEMS));
            mFilteredItems = savedInstanceState.getStringArrayList(STATE_FILTERED_ITEMS);
        }
        
        setContentView(R.layout.media_gallery_add_layout);
        mGridView = (MultiSelectGridView) findViewById(R.id.media_gallery_add_gridview);
        mGridView.setMultiSelectModeEnabled(true);
        mGridView.setMultiSelectListener(this);
        mAdapter = new MediaGridAdapter(this, null, 0, checkedItems);
        mGridView.setAdapter(mAdapter);
        
        mActionMode = getSherlock().startActionMode(this);
        mActionMode.setTitle(checkedItems.size() + " selected");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        refereshViews();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_SELECTED_ITEMS, mAdapter.getCheckedItems());
        outState.putStringArrayList(STATE_FILTERED_ITEMS, mFilteredItems);
    }

    private void refereshViews() {
        if (WordPress.getCurrentBlog() == null)
            return;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        Cursor cursor = WordPress.wpDB.getMediaImagesForBlog(blogId, mFilteredItems);
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onMultiSelectChange(int count) {
        mActionMode.setTitle(count + " selected");
        
        // stay always in multi-select mode, even when count reaches 0
        if (count == 0)
            mGridView.setMultiSelectModeEnabled(true);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_IDS, mAdapter.getCheckedItems());
        setResult(RESULT_OK, intent);
        finish();
    }
    
}
