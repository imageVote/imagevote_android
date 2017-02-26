package at.imagevote;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

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
        }else{
            this.hideSoftInput();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        Log.i(this.logName, "onEditorAction");
        hideSoftInput();
        this.setVisibility(this.INVISIBLE);

        ParseObject gameScore = new ParseObject("commentsv3");
        gameScore.put("comment", v.getText().toString());
        gameScore.saveInBackground();

        return false;
    }

    //FOCUS CHANGE events:

    public void hideSoftInput() {
        imm.hideSoftInputFromWindow(this.getWindowToken(),0);
    }

    public void showSoftInput() {
        this.setVisibility(this.VISIBLE);
        this.requestFocus();
        //softkeyboard last
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
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
