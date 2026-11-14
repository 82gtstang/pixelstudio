package com.pixelstudio.falcon;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private final int BG=0xff0b1020, PANEL=0xff1c2440, TEXT=0xfff7f7ff, ACCENT=0xff7968ff;
    private EditText ip, count, offset; private TextView status, swatch;
    private SeekBar hue, brightness, speed; private Spinner effect;
    private final AtomicBoolean running=new AtomicBoolean(false);
    private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
    private DatagramSocket socket; private InetAddress destination; private int frame=0;
    private int pixels=300, channelOffset=0, level=80, hueValue=0, speedValue=50;
    private volatile String mode="Solid";
    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,22,24,32);scroll.addView(root);setContentView(scroll);
        title(root,"✦  PIXEL STUDIO",28); label(root,"SMART PIXEL LIGHTING   •   FALCON F16V5");
        label(root,"Make every pixel shine.  Local Wi-Fi • No cloud.");
        section(root,"CONTROLLER"); ip=field(root,"Falcon IP address","192.168.1.231",false);
        count=field(root,"Pixel count (1–20000)","300",true);offset=field(root,"DDP start channel (zero-based)","0",true);
        label(root,"Match these settings to your Falcon's DDP input mapping. A wrong offset may illuminate a different prop.");
        Button connect=button(root,"CONNECT / START",ACCENT); status=label(root,"Not connected");
        section(root,"COLOR STUDIO");swatch=label(root,"       LIVE COLOR PREVIEW"); swatch.setTextColor(Color.BLACK);swatch.setPadding(20,30,20,30);
        label(root,"QUICK COLORS");
        LinearLayout palette=new LinearLayout(this);palette.setOrientation(LinearLayout.HORIZONTAL);root.addView(palette);
        int[] colors={0xffff3b59,0xffffa326,0xffffe55c,0xff22d37f,0xff27c5ff,0xff8657ff};
        for(int color:colors){TextView dot=new TextView(this);dot.setText("●");dot.setTextSize(30);dot.setTextColor(color);dot.setGravity(17);palette.addView(dot,new LinearLayout.LayoutParams(0,60,1));dot.setOnClickListener(v->{float[] hsv=new float[3];Color.colorToHSV(color,hsv);hue.setProgress((int)hsv[0]);});}
        hue=slider(root,"Hue",0,360,0,v->{hueValue=v;refreshSwatch();});
        brightness=slider(root,"Brightness",0,100,80,v->level=v);
        section(root,"DIY FINGER SKETCH");
        Button edit=button(root,"✎ OPEN VISUAL PIXEL EDITOR",0xff6955e8);
        edit.setOnClickListener(v->startActivity(new Intent(this,EditorActivity.class)));
        label(root,"Draw colors on a 300-cell strip, save your design, then stream it as a repeating pattern.");
        section(root,"DYNAMIC SCENES");String[] effects={"Solid","Rainbow","Chase","Meteor","Sparkle","Fire","Color Wipe","Twinkle","Breathing","DIY Editor"};
        effect=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,effects);effect.setAdapter(adapter);root.addView(effect);
        label(root,"ONE-TAP SCENES");
        String[] sceneNames={"🌈 Rainbow","🔥 Fire","✨ Sparkle","☄ Meteor","🌊 Chase","💜 Breathe","🎨 Wipe","⭐ Twinkle"};
        int[] sceneIndices={1,5,4,3,2,8,6,7};
        for(int row=0;row<2;row++){LinearLayout line=new LinearLayout(this);root.addView(line);for(int col=0;col<4;col++){int idx=row*4+col;Button tile=new Button(this);tile.setText(sceneNames[idx]);tile.setTextSize(11);tile.setAllCaps(false);tile.setTextColor(TEXT);tile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(idx%2==0?0xff30345d:0xff283b55));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,74,1);lp.setMargins(2,4,2,4);line.addView(tile,lp);tile.setOnClickListener(v->effect.setSelection(sceneIndices[idx]));}}
        effect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){mode=effects[pos];}});
        speed=slider(root,"Effect speed",1,100,50,v->speedValue=v);
        Button stop=button(root,"STOP / BLACKOUT",0xffcf405f);
        connect.setOnClickListener(v->start());stop.setOnClickListener(v->stop());refreshSwatch();
        label(root,"PIXEL STUDIO v2.0  •  DDP / UDP 4048  •  PRIVATE LOCAL CONTROL");
        timer.scheduleAtFixedRate(()->{if(running.get())sendFrame();},0,40,TimeUnit.MILLISECONDS);
    }
    private void section(LinearLayout root,String name){TextView h=label(root,name);h.setTextSize(18);h.setTextColor(0xffa99bff);h.setTypeface(null,Typeface.BOLD);h.setPadding(0,32,0,12);}
    @Override protected void onResume(){super.onResume();
        if(getSharedPreferences("pixel_diy",MODE_PRIVATE).contains("pattern") && effect!=null){effect.setSelection(9);mode="DIY Editor";}
    }
    private void start(){try{
        String host=ip.getText().toString().trim(); if(host.isEmpty())throw new Exception("Enter Falcon IP");
        int n=Integer.parseInt(count.getText().toString().trim()),off=Integer.parseInt(offset.getText().toString().trim());
        if(n<1||n>20000||off<0||off>1000000||off%3!=0)throw new Exception("Count must be 1–20000; channel offset must be a multiple of 3");
        destination=InetAddress.getByName(host);pixels=n;channelOffset=off;
        if(socket==null||socket.isClosed())socket=new DatagramSocket();running.set(true);
        status.setText("Streaming DDP to "+host+":4048 (not a confirmed controller handshake)");
        }catch(Exception ex){status.setText("Setup error: "+ex.getMessage());}}
    private void stop(){running.set(false);if(socket!=null&&destination!=null){byte[] black=new byte[pixels*3];try{sendPixels(black);}catch(Exception ignored){}}status.setText("Stopped; blackout sent once");}
    private void sendFrame(){try{int n=pixels;byte[] rgb=new byte[n*3];int h=hueValue,br=level;String m=mode;int f=frame++;
        int[] diy=null;int diyDir=0;
        if("DIY Editor".equals(m)){SharedPreferences pref=getSharedPreferences("pixel_diy",MODE_PRIVATE);String[] parts=pref.getString("pattern","").split(",");if(parts.length==300){diy=new int[300];for(int z=0;z<300;z++){try{diy[z]=(int)Long.parseLong(parts[z]);}catch(Exception ignored){diy[z]=Color.BLACK;}}diyDir=pref.getInt("direction",0);br=pref.getInt("brightness",br);}}
        final int[] diyColors=diy;final int diyDirection=diyDir;
        for(int i=0;i<n;i++){int r=0,g=0,b=0;float hh=h/360f;float value=br/100f;
            switch(m){case "DIY Editor":{
                if(diyColors!=null){int idx=i%300;int shift=(f*(speedValue/20+1))%300;
                    if(diyDirection==0)idx=(idx+shift)%300;else if(diyDirection==1)idx=(idx-shift+300)%300;
                    int color=diyColors[idx];int j=i*3;float scale=br/100f;
                    rgb[j]=(byte)(Color.red(color)*scale);rgb[j+1]=(byte)(Color.green(color)*scale);rgb[j+2]=(byte)(Color.blue(color)*scale);continue;
                }break;}
            case "Rainbow":hh=((h+i*2+f*3)%360)/360f;break;
            case "Chase":value=((i+f*(speedValue/10+1))%18<6)?value:0;break;
            case "Meteor":{int pos=(f*(speedValue/15+1))%(n+25);int dist=pos-i;value=dist>=0&&dist<25?value*(1-dist/25f):0;break;}
            case "Sparkle":value=(((i*73+f*101)^((i+f)*17))&31)<3?value:0;break;
            case "Fire":hh=(5+(i*7+f*3)%45)/360f;value*=0.45f+(((i*37+f*13)%51)/100f);break;
            case "Color Wipe":value=i<((f*(speedValue/10+1))%(n+1))?value:0;break;
            case "Twinkle":value=(((i*43+f*7)^(i*11))&63)<9?value:0;break;
            case "Breathing":value*=0.15f+0.85f*(float)(0.5+0.5*Math.sin(f*(speedValue+5)/220.0));break;
            }
            int c=Color.HSVToColor(new float[]{hh*360,1f,Math.max(0,Math.min(1,value))});int j=i*3;rgb[j]=(byte)Color.red(c);rgb[j+1]=(byte)Color.green(c);rgb[j+2]=(byte)Color.blue(c);
        }sendPixels(rgb);
    }catch(Exception ex){running.set(false);runOnUiThread(()->status.setText("Send error: "+ex.getMessage()));}}
    private void sendPixels(byte[] rgb)throws Exception{if(socket==null||destination==null)return;
        final int chunk=1440;for(int pos=0;pos<rgb.length;pos+=chunk){int len=Math.min(chunk,rgb.length-pos);byte[] packet=new byte[10+len];
            packet[0]=0x41;packet[1]=0;packet[2]=0;packet[3]=1;
            int off=channelOffset+pos;packet[4]=(byte)(off>>>24);packet[5]=(byte)(off>>>16);packet[6]=(byte)(off>>>8);packet[7]=(byte)off;
            packet[8]=(byte)(len>>>8);packet[9]=(byte)len;System.arraycopy(rgb,pos,packet,10,len);
            socket.send(new DatagramPacket(packet,packet.length,destination,4048));
        }}
    private void refreshSwatch(){if(swatch!=null){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.HSVToColor(new float[]{hueValue,1,1}),0xff283454});g.setCornerRadius(26);swatch.setBackground(g);}}
    private TextView label(LinearLayout root,String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(TEXT);t.setTextSize(14);t.setPadding(0,9,0,12);root.addView(t);return t;}
    private void title(LinearLayout root,String s,int size){TextView t=label(root,s);t.setTextSize(size);t.setTypeface(null,Typeface.BOLD);t.setPadding(0,22,0,12);}
    private EditText field(LinearLayout root,String hint,String initial,boolean numeric){label(root,hint);EditText e=new EditText(this);e.setSingleLine(true);e.setText(initial);e.setTextColor(TEXT);e.setHintTextColor(0xff9999aa);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ACCENT));if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER);root.addView(e);return e;}
    private Button button(LinearLayout root,String text,int color){Button b=new Button(this);b.setText(text);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));root.addView(b);return b;}
    interface Changed{void apply(int v);}
    private SeekBar slider(LinearLayout root,String text,int min,int max,int initial,Changed callback){TextView l=label(root,text+" : "+initial);SeekBar s=new SeekBar(this);s.setMax(max-min);s.setProgress(initial-min);s.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));root.addView(s);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean u){l.setText(text+" : "+(p+min));callback.apply(p+min);}});return s;}
    @Override protected void onDestroy(){running.set(false);timer.shutdownNow();if(socket!=null)socket.close();super.onDestroy();}
}
