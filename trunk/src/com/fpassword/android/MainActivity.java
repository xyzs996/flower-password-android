package com.fpassword.android;

import static com.fpassword.android.Helper.getStringOnCursor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.Toast;

import com.fpassword.android.Database.Keys;
import com.fpassword.core.EncryptionException;
import com.fpassword.core.FlowerPassword;

@SuppressWarnings("deprecation")
public class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getName();

    private final Database database = new Database(this);

    private EditText editPassword;

    private InstantAutoCompleteTextView editKey;

    private EditText editResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database.open();

        setContentView(R.layout.main);
        editPassword = (EditText) findViewById(R.id.edit_password);
        editKey = (InstantAutoCompleteTextView) findViewById(R.id.edit_key);
        editResult = (EditText) findViewById(R.id.edit_result);
        final Button buttonCopy = (Button) findViewById(R.id.button_copy);

        final TextWatcher textWatcher = new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                encryptPasswordWithKey();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

        };
        editPassword.addTextChangedListener(textWatcher);
        editKey.addTextChangedListener(textWatcher);

        buttonCopy.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                copyResultToClipboard();
            }

        });

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.key_list_item, null,
                new String[] { Keys.COLUMN_USED_KEY }, new int[] { R.id.key_list_item }, 0);
        adapter.setCursorToStringConverter(new CursorToStringConverter() {

            @Override
            public CharSequence convertToString(Cursor cursor) {
                return getStringOnCursor(cursor, Keys.COLUMN_USED_KEY);
            }

        });
        adapter.setFilterQueryProvider(new FilterQueryProvider() {

            @Override
            public Cursor runQuery(CharSequence constraint) {
                return database.queryUsedKeys(constraint != null ? constraint.toString() : null);
            }

        });
        editKey.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void encryptPasswordWithKey() {
        String passwordText = editPassword.getText().toString();
        String keyText = editKey.getText().toString();
        String resultText = "";
        try {
            resultText = FlowerPassword.encrypt(passwordText, keyText);
        } catch (EncryptionException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        editResult.setText(resultText);
    }

    private void copyResultToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setText(editResult.getText());
        Toast.makeText(this, R.string.toast_copy_success, Toast.LENGTH_SHORT).show();

        new AsyncTask<String, Void, Void>() {

            @Override
            protected Void doInBackground(String... params) {
                database.insertOrUpdateUsedKey(params[0]);
                return null;
            }

        }.execute(editKey.getText().toString());
    }

    private void resetPasswordAndKey() {
        editPassword.setText("");
        editKey.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_reset:
            resetPasswordAndKey();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}