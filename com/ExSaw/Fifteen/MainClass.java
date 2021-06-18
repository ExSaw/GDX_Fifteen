package com.ExSaw.Fifteen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.Random;


public class MainClass extends ApplicationAdapter implements GestureDetector.GestureListener {

    OrthographicCamera camera2D;
    SpriteBatch spriteBatch2D;
    Pixmap symbolsPix,blockPix,mixedPix;
    Texture texAtlas;
    TextureRegion[] blocksRegTex;
    Sprite[] blocksSpr;
    int[][] symbolsIndexesArray;
    int symbolsIndexesMEM=-1,curI=3,curJ=3,nMixes=0;
    int symbolWidth=0,symbolHeight=0,atlasWidth=0,atlasHeight=0;
    float cameraZOOM=1.0f,screenW=0.0f,screenH=0.0f,screenW_Half=0.0f,screenH_Half=0.0f,dx=0.0f,dy=0.0f;

    //Utils
    String pathPrefix="";
    Random rand;
    public static boolean REBUILD=true;
    boolean error=false;

    //gestures
    float HALF_TAP_SQUARE_SIZE = 20.0f;
    float TAP_COUNT_INTERVAL = 0.4f;
    float LONG_PRESS_DURATION = 1.1f;
    float MAX_FLING_DELAY = 0.15f;
    GestureDetector gestureDetector;
    Vector3 touchVecXYZ;
    int touchX=0,touchY=0;

    Sound touchSFX_OK;

    @Override
    public void create (){

        Gdx.app.log("TAG", '\n' + "<<<Loading New Game>>>");

        Preferences preferences = Gdx.app.getPreferences("Fifteen.pref");
        if(preferences==null){
            try {
                preferences.putInteger("TotalScore",0);
            }catch (NullPointerException e){System.err.println("catch Preferences RuntimeException");error=true;}}

        int tscore=preferences.getInteger("TotalScore");
        tscore+=1;preferences.putInteger("TotalScore",tscore);
        preferences.flush();
        Gdx.app.log("TAG",""+tscore);

        gestureDetector = new GestureDetector(HALF_TAP_SQUARE_SIZE, TAP_COUNT_INTERVAL, LONG_PRESS_DURATION, MAX_FLING_DELAY, this);
        Gdx.input.setInputProcessor(gestureDetector);
        touchVecXYZ = new Vector3(0,0,0);

        rand = new Random();
        camera2D=new OrthographicCamera();
        spriteBatch2D=new SpriteBatch();

        if (Gdx.app.getType() == Application.ApplicationType.Desktop){pathPrefix="android/assets/";}//remove this path, when compiling final desktop version
        if (Gdx.app.getType() == Application.ApplicationType.Android){pathPrefix="";}

        //Создаем MIX из символов и шаблона блока
        blockPix = new Pixmap(Gdx.files.internal(pathPrefix+"Button_2.png"));
        symbolsPix = new Pixmap(Gdx.files.internal(pathPrefix+"Atlas.png"));
        touchSFX_OK = Gdx.audio.newSound(Gdx.files.internal(pathPrefix+"sfx_touch2.mp3"));
        symbolsIndexesArray=new int[4][4];

        for(int i=0,n=0;i<4;i++){
            for(int j=0;j<4;j++,n++){
                symbolsIndexesArray[j][i]=n;}}

        mixedPix=new Pixmap(symbolsPix.getWidth(),symbolsPix.getHeight(), Pixmap.Format.RGBA8888);
        symbolWidth=(int)(symbolsPix.getWidth()*0.25f);
        symbolHeight=(int)(symbolsPix.getHeight()*0.25f);
        for(int i=1,n=0;i<mixedPix.getWidth();i+=symbolWidth){
            for(int j=1;j<mixedPix.getHeight();j+=symbolHeight,n++){
                mixedPix.drawPixmap(blockPix,i,j);
                mixedPix.drawPixmap(symbolsPix,i,j,i,j,symbolWidth,symbolHeight);}
        }
        texAtlas=new Texture(mixedPix);mixedPix.dispose();symbolsPix.dispose();blockPix.dispose();
        texAtlas.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
        atlasWidth=texAtlas.getWidth();atlasHeight=texAtlas.getHeight();
        texAtlas.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        blocksRegTex=new TextureRegion[16];
        blocksSpr=new Sprite[16];
        for(int i=0,n=0;i<texAtlas.getWidth();i+=symbolWidth) {
            for (int j=0; j < texAtlas.getHeight(); j += symbolHeight, n++) {
                blocksRegTex[n] = new TextureRegion(texAtlas, j, i, symbolWidth, symbolHeight);
                blocksSpr[n] = new Sprite(blocksRegTex[n], 0, 0, symbolWidth, symbolHeight);
                blocksSpr[n].setOrigin(0, 0);//очень важно
            }
        }
        resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
    }

    @Override
    public void resize(int width,int height){
        screenW=width;screenH=height;screenW_Half=screenW*0.5f;screenH_Half=screenH*0.5f;
        if(screenW>=screenH){cameraZOOM=atlasHeight/screenH;dx=(screenW*cameraZOOM-atlasWidth)*0.5f;dy=0.0f;}
        else{cameraZOOM=atlasWidth/screenW;dy=(screenH*cameraZOOM-atlasHeight)*0.5f;dx=0.0f;}
        camera2D.zoom = cameraZOOM;
        camera2D.setToOrtho(false, width, height);
        camera2D.update();
        spriteBatch2D.setProjectionMatrix(camera2D.combined);
    }

    @Override
    public void render () {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if(REBUILD&&nMixes<400){if(nMixes==0){for(int i=0;i<4;i++){
            for(int j=0;j<4;j++){if(symbolsIndexesArray[i][j]==15){curI=i;curJ=j;break;}}}}mixBlocks();nMixes++;if(nMixes>=400){REBUILD=false;nMixes=0;}}
        spriteBatch2D.begin();
        findTappedBlock();
        for(int i=0;i<4;i++){for(int j=0;j<4;j++){if(symbolsIndexesArray[i][j]!=15){blocksSpr[symbolsIndexesArray[i][j]].draw(spriteBatch2D);}}}
        spriteBatch2D.end();
    }
    //<<<FUNCTION>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    public void findTappedBlock(){
        for(int i=0;i<4;i++){
            for(int j=0;j<4;j++){
                if(symbolsIndexesArray[i][j]!=15){//15==0(empty cell)
                    if(touchX>blocksSpr[symbolsIndexesArray[i][j]].getX()
                            &&touchX<blocksSpr[symbolsIndexesArray[i][j]].getX()+symbolWidth
                            &&touchY>blocksSpr[symbolsIndexesArray[i][j]].getY()
                            &&touchY<blocksSpr[symbolsIndexesArray[i][j]].getY()+symbolHeight){symbolsIndexesMEM=-1;

                        if(i+1<4&&symbolsIndexesArray[i+1][j]==15){
                            symbolsIndexesMEM=symbolsIndexesArray[i][j];symbolsIndexesArray[i][j]=15;symbolsIndexesArray[i+1][j]=symbolsIndexesMEM;touchSFX_OK.play();
                        }
                        if(i-1>=0&&symbolsIndexesArray[i-1][j]==15&&symbolsIndexesMEM==-1){
                            symbolsIndexesMEM=symbolsIndexesArray[i][j];symbolsIndexesArray[i][j]=15;symbolsIndexesArray[i-1][j]=symbolsIndexesMEM;touchSFX_OK.play();
                        }
                        if(j+1<4&&symbolsIndexesArray[i][j+1]==15&&symbolsIndexesMEM==-1){
                            symbolsIndexesMEM=symbolsIndexesArray[i][j];symbolsIndexesArray[i][j]=15;symbolsIndexesArray[i][j+1]=symbolsIndexesMEM;touchSFX_OK.play();
                        }
                        if(j-1>=0&&symbolsIndexesArray[i][j-1]==15&&symbolsIndexesMEM==-1){
                            symbolsIndexesMEM=symbolsIndexesArray[i][j];symbolsIndexesArray[i][j]=15;symbolsIndexesArray[i][j-1]=symbolsIndexesMEM;touchSFX_OK.play();
                        }
                        touchX=-1;touchY=-1;
                    }
                    blocksSpr[symbolsIndexesArray[i][j]].setPosition(i * symbolWidth + dx, (3 - j) * symbolHeight + dy);}
            }
        }
    }//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    //<<<FUNCTION>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    public void mixBlocks(){
        if(curI+1<4&&rand.nextInt(5)==0){
            symbolsIndexesMEM=symbolsIndexesArray[curI+1][curJ];symbolsIndexesArray[curI+1][curJ]=15;symbolsIndexesArray[curI][curJ]=symbolsIndexesMEM;curI++;}
        if(curI-1>=0&&rand.nextInt(4)==0){
            symbolsIndexesMEM=symbolsIndexesArray[curI-1][curJ];symbolsIndexesArray[curI-1][curJ]=15;symbolsIndexesArray[curI][curJ]=symbolsIndexesMEM;curI--;}
        if(curJ+1<4&&rand.nextInt(3)==0){
            symbolsIndexesMEM=symbolsIndexesArray[curI][curJ+1];symbolsIndexesArray[curI][curJ+1]=15;symbolsIndexesArray[curI][curJ]=symbolsIndexesMEM;curJ++;}
        if(curJ-1>=0&&rand.nextInt(2)==0){
            symbolsIndexesMEM=symbolsIndexesArray[curI][curJ-1];symbolsIndexesArray[curI][curJ-1]=15;symbolsIndexesArray[curI][curJ]=symbolsIndexesMEM;curJ--;}
    }//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        spriteBatch2D.dispose();
        texAtlas.dispose();
        touchSFX_OK.dispose();
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        touchVecXYZ.set(x, y, 0);
        camera2D.unproject(touchVecXYZ);
        touchX=(int)touchVecXYZ.x;touchY=(int)touchVecXYZ.y;
        //Gdx.app.log("TAG", '\n' + "TouchDown_x="+touchX+" TouchDown_y="+touchY);
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {

        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {//бросок
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }
}
