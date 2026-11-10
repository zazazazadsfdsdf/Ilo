package com.ashenlegacy.game

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import kotlin.math.*
import kotlin.random.Random

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        setContentView(GameView())
    }
}

private data class Mob(var x:Float,var y:Float,var hp:Float=2f,var speed:Float=45f)
private data class Orb(var x:Float,var y:Float)
private data class Shot(var x:Float,var y:Float,var vx:Float,var vy:Float)

private class GameView : View() {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val mobs=mutableListOf<Mob>(); private val orbs=mutableListOf<Orb>(); private val shots=mutableListOf<Shot>()
    private var px=500f; private var py=350f; private var hp=100f; private var xp=0; private var level=1; private var kills=0
    private var last=System.nanoTime(); private var spawn=0f; private var fire=0f; private var moveX=0f; private var moveY=0f
    private var dragging=false; private var dead=false; private var paused=false; private var upgrade=false
    private var chosen=0; private var damage=1f; private var speed=1f
    private val names=listOf("ПЕПЕЛЬНЫЙ ВЗРЫВ","КРОВАВЫЙ ШАГ","КЛИНОК ПУСТОТЫ")
    override fun onDraw(c:Canvas) {
        super.onDraw(c); val w=width.toFloat(); val h=height.toFloat()
        c.drawColor(Color.rgb(14,13,20))
        p.color=Color.rgb(35,31,42); p.strokeWidth=1f
        for(x in 0..(w/48).toInt()) c.drawLine(x*48f,0f,x*48f,h,p)
        for(y in 0..(h/48).toInt()) c.drawLine(0f,y*48f,w,y*48f,p)
        val now=System.nanoTime(); val dt=min(.04f,(now-last)/1e9f).toFloat(); last=now
        if(!dead&&!paused&&!upgrade) update(dt,w,h)
        // ember-lit hero
        p.color=Color.rgb(235,170,92); c.drawCircle(px,py,18f,p)
        p.color=Color.rgb(255,225,170); c.drawCircle(px,py,7f,p)
        for(m in mobs){p.color=Color.rgb(130,54,62);c.drawCircle(m.x,m.y,15f,p);p.color=Color.rgb(245,130,90);c.drawCircle(m.x-4,m.y-3,3f,p)}
        for(o in orbs){p.color=Color.rgb(80,210,190);c.drawCircle(o.x,o.y,6f,p)}
        for(s in shots){p.color=Color.rgb(255,210,130);c.drawCircle(s.x,s.y,5f,p)}
        hud(c,w)
        if(upgrade) overlay(c,w,h,"УРОВЕНЬ $level","Выбери дар пепла",names[0],names[1],names[2])
        if(dead) overlay(c,w,h,"ПУТЕШЕСТВИЕ ОКОНЧЕНО","Убийств: $kills  •  Уровень: $level","КОСНИСЬ, ЧТОБЫ НАЧАТЬ ЗАНОВО","","")
        if(paused&&!upgrade&&!dead) overlay(c,w,h,"ПАУЗА","Передышка среди пепла","КОСНИСЬ, ЧТОБЫ ПРОДОЛЖИТЬ","","")
        postInvalidateOnAnimation()
    }
    private fun update(dt:Float,w:Float,h:Float) {
        if(!dragging){moveX=0f;moveY=0f}
        px=(px+moveX*220f*speed*dt).coerceIn(24f,w-24f); py=(py+moveY*220f*speed*dt).coerceIn(24f,h-24f)
        spawn+=dt; if(spawn>max(.28f,1.1f-level*.025f)){spawn=0f;val side=Random.nextInt(4);val x=if(side<2)Random.nextFloat()*w else if(side==2) -20f else w+20f;val y=if(side>=2)Random.nextFloat()*h else if(side==0)-20f else h+20f;mobs.add(Mob(x,y,speed=42f+level*2f))}
        for(m in mobs){val dx=px-m.x;val dy=py-m.y;val d=max(1f,hypot(dx,dy));m.x+=dx/d*m.speed*dt;m.y+=dy/d*m.speed*dt;if(d<27f)hp-=13f*dt}
        fire+=dt; if(fire>.42f&&!mobs.isEmpty()){fire=0f;val m=mobs.minByOrNull{hypot(it.x-px,it.y-py)}!!;val dx=m.x-px;val dy=m.y-py;val d=max(1f,hypot(dx,dy));shots.add(Shot(px,py,dx/d*420f,dy/d*420f))}
        val it=shots.iterator();while(it.hasNext()){val s=it.next();s.x+=s.vx*dt;s.y+=s.vy*dt;var hit=false;val mi=mobs.iterator();while(mi.hasNext()){val m=mi.next();if(hypot(m.x-s.x,m.y-s.y)<18f){m.hp-=damage;hit=true;if(m.hp<=0){orbs.add(Orb(m.x,m.y));mi.remove();kills++};break}};if(hit||s.x<0||s.y<0||s.x>w||s.y>h)it.remove()}
        val oi=orbs.iterator();while(oi.hasNext()){val o=oi.next();val dx=px-o.x;val dy=py-o.y;val d=hypot(dx,dy);if(d<25f){xp+=1;oi.remove();if(xp>=level*7){xp=0;level++;upgrade=true}}else if(d<125f){o.x+=dx/d*160f*dt;o.y+=dy/d*160f*dt}}
        if(hp<=0)dead=true
    }
    private fun hud(c:Canvas,w:Float){p.color=Color.rgb(65,35,43);c.drawRoundRect(RectF(22f,20f,222f,34f),7f,7f,p);p.color=Color.rgb(207,74,83);c.drawRoundRect(RectF(22f,20f,22f+200f*(hp/100f).coerceIn(0f,1f),34f),7f,7f,p);p.color=Color.WHITE;p.textSize=18f;c.drawText("УРОВЕНЬ $level",22f,61f,p);p.textSize=14f;p.color=Color.rgb(130,215,195);c.drawText("Осколки опыта: $xp / ${level*7}",22f,82f,p);p.color=Color.LTGRAY;c.drawText("УБИЙСТВА $kills",w-145f,34f,p);c.drawText("II  ПАУЗА",w-105f,64f,p)}
    private fun overlay(c:Canvas,w:Float,h:Float,title:String,sub:String,a:String,b:String,d:String){p.color=0xDD08080F.toInt();c.drawRect(0f,0f,w,h,p);p.textAlign=Paint.Align.CENTER;p.color=Color.rgb(236,190,125);p.textSize=34f;c.drawText(title,w/2,h/2-70,p);p.color=Color.WHITE;p.textSize=18f;c.drawText(sub,w/2,h/2-35,p);p.color=Color.rgb(220,205,185);p.textSize=19f;if(a.isNotEmpty())c.drawText("1. $a",w/2,h/2+10,p);if(b.isNotEmpty())c.drawText("2. $b",w/2,h/2+42,p);if(d.isNotEmpty())c.drawText("3. $d",w/2,h/2+74,p);p.textAlign=Paint.Align.LEFT}
    override fun onTouchEvent(e:MotionEvent):Boolean {val w=width.toFloat();val h=height.toFloat();when(e.action){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{if(dead){reset();return true};if(upgrade){chosen=((e.y-h/2+5f)/32f).toInt().coerceIn(0,2);when(chosen){0->damage+=.7f;1->speed+=.12f;2->hp=(hp+25).coerceAtMost(100f)};upgrade=false;return true};if(e.x>w-150&&e.y<95){paused=!paused;return true};dragging=true;val dx=e.x-px;val dy=e.y-py;val d=max(1f,hypot(dx,dy));moveX=dx/d;moveY=dy/d};MotionEvent.ACTION_UP->{dragging=false;moveX=0f;moveY=0f}};return true}
    private fun reset(){mobs.clear();orbs.clear();shots.clear();px=width/2f;py=height/2f;hp=100f;xp=0;level=1;kills=0;damage=1f;speed=1f;dead=false;upgrade=false;paused=false}
}
