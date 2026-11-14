package com.pixelstudio.falcon;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

public class EditorActivity extends Activity {
    private static final int CELLS=60;
    private final int[] pattern=new int[CELLS];
    private int background=Color.BLACK, fill=Color.RED, brightness=80, direction=0;
    private boolean eraser=false;
    private Preview preview;
    private TextView info;
    private SharedPreferences prefs;
    private int dp(float value){return (int)(getResources().getDisplayMetrics().density*value+.5f);}
    private TextView label(LinearLayout parent,String s,int size){
        TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(size);
        t.setPadding(dp(4),dp(10),dp(4),dp(8));parent.addView(t);return t;
    }
    private Button button(LinearLayout parent,String s, Runnable action){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff303458));
        parent.addView(b);b.setOnClickListener(v->action.run());return b;
    }
    private void colors(LinearLayout parent,boolean bg){
        LinearLayout row=new LinearLayout(this);parent.addView(row);
        int[] choices={Color.RED,Color.WHITE,0xff2277ff,0xffffd22e,0xff16dc9a,0xffa060ff,0xffff62b3,Color.BLACK};
        for(int c:choices){
            TextView tile=new TextView(this);tile.setText("●");tile.setTextColor(c==Color.BLACK?0xff555566:c);
            tile.setTextSize(29);tile.setGravity(17);row.addView(tile,new LinearLayout.LayoutParams(0,dp(54),1));
            tile.setOnClickListener(v->{if(bg){background=c;for(int i=0;i<CELLS;i++)if(pattern[i]==Color.TRANSPARENT)pattern[i]=background;}
                else fill=c;preview.invalidate();});
        }
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(0xff19163b);getWindow().setNavigationBarColor(0xff0b1020);
        prefs=getSharedPreferences("pixel_diy",MODE_PRIVATE);
        background=prefs.getInt("background",Color.BLACK);fill=prefs.getInt("fill",Color.RED);
        brightness=prefs.getInt("brightness",80);direction=prefs.getInt("direction",0);
        String saved=prefs.getString("pattern","");
        String[] parts=saved.split(",");
        for(int i=0;i<CELLS;i++){try{pattern[i]=Long.valueOf(parts[i]).intValue();}catch(Exception e){pattern[i]=background;}}
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff100e29);setContentView(scroll);
        LinearLayout root=new LinearLayout(this);root.setPadding(dp(16),dp(18),dp(16),dp(30));root.setOrientation(1);scroll.addView(root);
        label(root,"✦ DIY PIXEL EDITOR",25);
        label(root,"Finger sketch • Paint your own lighting pattern",13);
        label(root,"Background color",17);colors(root,true);
        label(root,"Fill color",17);colors(root,false);
        LinearLayout tools=new LinearLayout(this);root.addView(tools);
        Button paint=button(tools,"✎ Paint",()->{eraser=false;info.setText("Paint mode: drag your finger across the strip");});
        Button erase=button(tools,"⌫ Erase",()->{eraser=true;info.setText("Erase mode: drag to restore the background");});
        paint.setLayoutParams(new LinearLayout.LayoutParams(0,dp(52),1));erase.setLayoutParams(new LinearLayout.LayoutParams(0,dp(52),1));
        label(root,"Finger sketch preview — 60 editable cells",17);
        preview=new Preview();root.addView(preview,new LinearLayout.LayoutParams(-1,dp(190)));
        info=label(root,"Drag across the strip to draw your pattern",13);
        label(root,"Brightness",17);
        SeekBar br=new SeekBar(this);br.setMax(100);br.setProgress(brightness);root.addView(br);
        br.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean user){brightness=p;preview.invalidate();}
            public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
        });
        label(root,"Animation direction",17);
        LinearLayout dir=new LinearLayout(this);root.addView(dir);
        String[] names={"↻ Clockwise","↺ Counterclockwise","⟳ Cycle"};
        for(int i=0;i<3;i++){final int d=i;Button b=button(dir,names[i],()->{direction=d;info.setText("Direction: "+names[d]);});b.setTextSize(11);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(60),1));}
        button(root,"Fill entire strip with selected color",()->{for(int i=0;i<CELLS;i++)pattern[i]=fill;preview.invalidate();});
        button(root,"Clear to background",()->{for(int i=0;i<CELLS;i++)pattern[i]=background;preview.invalidate();});
        Button save=button(root,"✓ SAVE & USE IN PIXEL STUDIO",()->save());save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff6955e8));
        label(root,"Your 60-cell design repeats across the pixel count selected on the main screen.",12);
    }
    private void save(){
        StringBuilder s=new StringBuilder();for(int i=0;i<CELLS;i++){if(i>0)s.append(',');s.append(pattern[i]&0xffffffffL);}
        prefs.edit().putString("pattern",s.toString()).putInt("background",background).putInt("fill",fill)
            .putInt("brightness",brightness).putInt("direction",direction).apply();
        Toast.makeText(this,"DIY scene saved",Toast.LENGTH_SHORT).show();finish();
    }
    private class Preview extends View{
        private final Paint paint=new Paint(3);
        Preview(){super(EditorActivity.this);setBackgroundColor(0xff222046);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float pad=dp(8),w=(getWidth()-2*pad)/CELLS;
            float top=dp(28),h=dp(94);
            for(int i=0;i<CELLS;i++){
                int color=pattern[i];float scale=brightness/100f;
                paint.setColor(Color.rgb((int)(Color.red(color)*scale),(int)(Color.green(color)*scale),(int)(Color.blue(color)*scale)));
                c.drawRect(pad+i*w,top,pad+(i+1)*w-1,top+h,paint);
            }
            paint.setColor(Color.WHITE);paint.setTextSize(dp(13));
            c.drawText("PIXEL STRIP • TOUCH TO PAINT",pad,dp(18),paint);
            c.drawText("1",pad,top+h+dp(20),paint);c.drawText("60",getWidth()-dp(28),top+h+dp(20),paint);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){
                float pad=dp(8),w=(getWidth()-2*pad)/CELLS;
                int idx=Math.max(0,Math.min(CELLS-1,(int)((e.getX()-pad)/w)));
                pattern[idx]=eraser?background:fill;invalidate();return true;
            }
            return e.getAction()==MotionEvent.ACTION_UP||super.onTouchEvent(e);
        }
    }
}
