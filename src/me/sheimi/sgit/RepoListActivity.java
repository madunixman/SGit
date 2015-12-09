package me.sheimi.sgit;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.android.utils.Constants;
import me.sheimi.sgit.activities.explorer.ExploreFileActivity;
import me.sheimi.sgit.activities.explorer.ImportRepositoryActivity;
import me.sheimi.sgit.activities.explorer.PrivateKeyManageActivity;
import me.sheimi.sgit.adapters.RepoListAdapter;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.dialogs.CloneDialog;
import me.sheimi.sgit.dialogs.DummyDialogListener;
import me.sheimi.sgit.dialogs.ImportLocalRepoDialog;
import me.sheimi.sgit.dialogs.ProfileDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SearchView;

import java.io.File;

import me.sheimi.sgit.ssh.PrivateKeyUtils;

public class RepoListActivity extends SheimiFragmentActivity {

    private ListView mRepoList;
    private RepoListAdapter mRepoListAdapter;

    private static final int REQUEST_IMPORT_REPO = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrivateKeyUtils.migratePrivateKeys();
        setContentView(R.layout.activity_main);
        mRepoList = (ListView) findViewById(R.id.repoList);
        mRepoListAdapter = new RepoListAdapter(this);
        mRepoList.setAdapter(mRepoListAdapter);
        mRepoListAdapter.queryAllRepo();
        mRepoList.setOnItemClickListener(mRepoListAdapter);
        mRepoList.setOnItemLongClickListener(mRepoListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        configSearchAction(searchItem);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_manage_private_keys:
                intent = new Intent(this, PrivateKeyManageActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_new:
                CloneDialog cloneDialog = new CloneDialog();
                cloneDialog.show(getFragmentManager(), "clone-dialog");
                return true;
            case R.id.action_preferences:
                ProfileDialog profileDialog = new ProfileDialog();
                profileDialog.show(getFragmentManager(), "preferences-dialog");
                return true;
            case R.id.action_import_repo:
                intent = new Intent(this, ImportRepositoryActivity.class);
                startActivityForResult(intent, REQUEST_IMPORT_REPO);
                forwardTransition();
                return true;
            case R.id.action_feedback:
                Intent feedback = new Intent(Intent.ACTION_SEND);
                feedback.setType("text/email");
                feedback.putExtra(Intent.EXTRA_EMAIL,
                        new String[] { Constants.FEEDBACK_EMAIL });
                feedback.putExtra(Intent.EXTRA_SUBJECT,
                        getString(Constants.FEEDBACK_SUBJECT));
                startActivity(Intent.createChooser(feedback,
                        getString(R.string.label_send_feedback)));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void configSearchAction(MenuItem searchItem) {
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView == null)
            return;
        SearchListener searchListener = new SearchListener();
        searchItem.setOnActionExpandListener(searchListener);
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(searchListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        switch (requestCode) {
            case REQUEST_IMPORT_REPO:
                final String path = data.getExtras().getString(
                        ExploreFileActivity.RESULT_PATH);
                File file = new File(path);
                File dotGit = new File(file, Repo.DOT_GIT_DIR);
                if (!dotGit.exists()) {
                    showToastMessage(getString(R.string.error_no_repository));
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        this);
                builder.setTitle(R.string.dialog_comfirm_import_repo_title);
                builder.setMessage(R.string.dialog_comfirm_import_repo_msg);
                builder.setNegativeButton(R.string.label_cancel,
                        new DummyDialogListener());
                builder.setPositiveButton(R.string.label_import,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialogInterface, int i) {
                                Bundle args = new Bundle();
                                args.putString(ImportLocalRepoDialog.FROM_PATH, path);
                                ImportLocalRepoDialog rld = new ImportLocalRepoDialog();
                                rld.setArguments(args);
                                rld.show(getFragmentManager(), "import-local-dialog");
                            }
                        });
                builder.show();
                break;
        }
    }

    public class SearchListener implements SearchView.OnQueryTextListener,
            MenuItem.OnActionExpandListener {

        @Override
        public boolean onQueryTextSubmit(String s) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            mRepoListAdapter.searchRepo(s);
            return false;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem menuItem) {
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem menuItem) {
            mRepoListAdapter.queryAllRepo();
            return true;
        }

    }

    public void finish() {
        rawfinish();
    }

}
