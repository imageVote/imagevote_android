package at.imagevote;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import at.wouldyourather.R;

import com.parse.Parse;
import com.parse.ParseObject;

public class MessageLayout extends EditText implements View.OnFocusChangeListener, TextView.OnEditorActionListener, TextWatcher {

    public MessageLayout(Context context) {
        super(context);
        start(context);
    }

    public MessageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        start(context);
    }

    public MessageLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        start(context);
    }

    private Context ctx;
    private String logName;
    InputMethodManager imm;

    private void start(Context context) {
        this.ctx = context;
        this.logName = this.getClass().getName();
        imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);

        setOnEditorActionListener(this);
        //this.addTextChangedListener(this);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            this.showSoftInput();
        } else {
            this.hideSoftInput();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        Log.i(this.logName, "onEditorAction");
        hideSoftInput();
        this.setVisibility(INVISIBLE);

        //PARSE SERVER:
        String parseId = ctx.getResources().getString(R.string.parse_id);
        Parse.initialize(new Parse.Configuration.Builder(ctx)
                .applicationId(parseId)
                .server("https://api.parse.buddy.com/parse").build());

        ParseObject gameScore = new ParseObject("commentsv3");
        String message = v.getText().toString();
        Log.i(logName, "onEditorAction message: " + message);
        gameScore.put("comment", message);
        gameScore.saveInBackground();

        return false;
    }

    //FOCUS CHANGE events:
    public void hideSoftInput() {
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    public void showSoftInput() {
        this.setVisibility(VISIBLE);
        this.requestFocus();
        //softkeyboard last
        //http://stackoverflow.com/questions/5520085/android-show-softkeyboard-with-showsoftinput-is-not-working
        imm.toggleSoftInput(0, 0);
    }

    //http://stackoverflow.com/questions/5014219/multiline-edittext-with-done-softinput-action-label-on-2-3/12570003#12570003
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);
        int imeActions = outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION;
        if ((imeActions & EditorInfo.IME_ACTION_DONE) != 0) {
            // clear the existing action
            outAttrs.imeOptions ^= imeActions;
            // set the DONE action
            outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
        }
        if ((outAttrs.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }
        return connection;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //
    }

    @Override
    public void afterTextChanged(Editable s) {
        //
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //
    }
}
