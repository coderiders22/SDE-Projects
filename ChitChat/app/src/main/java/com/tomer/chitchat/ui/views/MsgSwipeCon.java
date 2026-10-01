package com.tomer.chitchat.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.tomer.chitchat.R;
import com.tomer.chitchat.modals.states.MsgStatus;
import com.tomer.chitchat.modals.states.UiMsgModal;

import java.util.LinkedList;

public class MsgSwipeCon extends ItemTouchHelper.Callback {

    private final Context con;
    private final SwipeCA swipeCA;
    long lastReplyButtonAnimationTime = 0;
    float replyButtonProgress = 0f;
    private Drawable imageDrawable;
    private Drawable shareRound;
    private RecyclerView.ViewHolder currentItem = null;
    private View mView;
    private float dX = 0f;
    private boolean swipeBack = false;
    private boolean isVibrate = false;
    private boolean startTracking = false;
    private final LinkedList<UiMsgModal> chatItems;

    public MsgSwipeCon(Context context, SwipeCA swipeCA, LinkedList<UiMsgModal> chatItems) {
        this.con = context;
        this.swipeCA = swipeCA;
        this.chatItems = chatItems;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        mView = viewHolder.itemView;
        imageDrawable = ContextCompat.getDrawable(con, R.drawable.round_reply_24);
        shareRound = ContextCompat.getDrawable(con, R.drawable.re_bg);
        return ItemTouchHelper.Callback.makeMovementFlags(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.RIGHT);
    }


    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        if (swipeBack) {
            swipeBack = false;
            return 0;
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (chatItems.get(viewHolder.getBindingAdapterPosition()).getStatus()== MsgStatus.SENDING) return;
            setTouchLis(recyclerView, viewHolder);
        }
        if (mView.getTranslationX() < convertTodp(140) || dX < this.dX) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            this.dX = dX;
            startTracking = true;
        }
        currentItem = viewHolder;
        drawRepButton(c);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchLis(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        recyclerView.setOnTouchListener((view, motionEvent) -> {
                    swipeBack = motionEvent.getAction() == MotionEvent.ACTION_CANCEL || motionEvent.getAction() == MotionEvent.ACTION_UP;

                    if (swipeBack) {
                        if (Math.abs(mView.getTranslationX()) >= convertTodp(100)) {
                            swipeCA.showRep(viewHolder.getBindingAdapterPosition());
                        }
                    }
                    return false;
                }
        );
    }

    private void drawRepButton(Canvas canvas) {
        if (currentItem == null) return;

        float translationX = mView.getTranslationX();
        long newTime = System.currentTimeMillis();
        long dt = Math.min(17, newTime - lastReplyButtonAnimationTime);
        lastReplyButtonAnimationTime = newTime;
        boolean showing = translationX >= convertTodp(30);
        if (showing) {
            if (replyButtonProgress < 1.0f) {
                replyButtonProgress += dt / 180.0f;
                if (replyButtonProgress > 1.0f) {
                    replyButtonProgress = 1.0f;
                } else {
                    mView.invalidate();
                }
            }
        } else if (translationX <= 0.0f) {
            replyButtonProgress = 0f;
            startTracking = false;
            isVibrate = false;
        } else {
            if (replyButtonProgress > 0.0f) {
                replyButtonProgress -= dt / 180.0f;
                if (replyButtonProgress < 0.1f) {
                    replyButtonProgress = 0f;
                } else {
                    mView.invalidate();
                }
            }
        }

        int alpha;
        float scale;
        if (showing) {
            if (replyButtonProgress <= 0.8f) {
                scale = 1.2f * (replyButtonProgress / 0.8f);
            } else {
                scale = 1.2f - 0.2f * ((replyButtonProgress - 0.8f) / 0.2f);
            }
            alpha = (int) Math.min(255f, 255 * (replyButtonProgress / 0.8f));
        } else {
            scale = replyButtonProgress;
            alpha = (int) Math.min(255f, 255 * replyButtonProgress);
        }
        shareRound.setAlpha(alpha);
        imageDrawable.setAlpha(alpha);
        if (startTracking) {
            if (!isVibrate && mView.getTranslationX() >= convertTodp(100)) {
                mView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                isVibrate = true;
            }
        }
        int x;
        float y;
        if (mView.getTranslationX() > convertTodp(130)) {
            x = convertTodp(130) / 2;
        } else {
            x = (int) (mView.getTranslationX() / 2);
        }

        y = (mView.getTop() + (float) mView.getMeasuredHeight() / 2);
        shareRound.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(con, R.color.trans), PorterDuff.Mode.ADD));
        shareRound.setBounds((int) (x - convertTodp(18) * scale), (int) (y - convertTodp(18) * scale), (int) (x + convertTodp(18) * scale), (int) (y + convertTodp(18) * scale));
        shareRound.draw(canvas);
        imageDrawable.setBounds((int) (x - convertTodp(12) * scale), (int) (y - convertTodp(11) * scale), (int) (x + convertTodp(12) * scale), (int) (y + convertTodp(10) * scale));
        imageDrawable.draw(canvas);
        shareRound.setAlpha(255);
        imageDrawable.setAlpha(255);
    }

    private int convertTodp(int val) {
        float density = 1f;
        try {
            density = con.getResources().getDisplayMetrics().density;
        } catch (Exception ignored) {
        }
        if (val == 0f) {
            return 0;
        } else {
            return (int) Math.ceil(density * val);
        }
    }

    public interface SwipeCA {
        void showRep(int position);
    }
}
