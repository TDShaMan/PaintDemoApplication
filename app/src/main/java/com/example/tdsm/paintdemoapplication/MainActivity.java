package com.example.tdsm.paintdemoapplication;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;
import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {

    private ImageView image;
    private PhotoView bimage;
    private ImageView curtain;
    // 画笔
    private Paint paint;
    // 画布
    private Canvas canvas;
    // 缩放后的图片
    private Bitmap bitmap;
    // 缩放后的图片副本
    private Bitmap copyBitmap;

    private Button choose;
    private Button draw;
    private Button save;
    private Button processMenuOpen;
    private Button processMenuClose;
    private RelativeLayout processMenu;
    private Button maskOk;
    private Button maskCancel;
    private final static int RESULT = 0;

    private static float downx;
    private static float downy ;
    private float x = 0;
    private float y = 0;
    private int windowWidth;
    private int windowHeight;
    private boolean cut;

    private  int a=0,f = 0;

    private Activity mAc = this;

    File mPrototype;
    File mMask;
    File mTarget;
    int mXLocate;
    int mYLocate;
    int pWidth;
    int pHeight;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 获取id
        image = (ImageView) findViewById(R.id.image);
        curtain = (ImageView) findViewById(R.id.curtain);
        curtain.setBackgroundColor(Color.BLACK);
        curtain.setAlpha(0.5f);
        image.setAlpha(0.5f);
        bimage = (PhotoView ) findViewById(R.id.background);
        choose = (Button) findViewById(R.id.chooseButton);
        draw = (Button) findViewById(R.id.drawButton);
        save = (Button) findViewById(R.id.saveButton);
        processMenu = (RelativeLayout)findViewById(R.id.process_menu);
        processMenuClose = (Button) findViewById(R.id.process_menu_close);
        processMenuOpen = (Button) findViewById(R.id.process_menu_open);
        maskCancel = (Button)findViewById(R.id.mask_cancel);
        maskOk = (Button)findViewById(R.id.mask_ok);

        maskOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                processMenuOpen.setClickable(true);
                maskCancel.setVisibility(View.GONE);
                maskOk.setVisibility(View.GONE);
                f=1;
            }
        });

        maskCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processMenuOpen.setClickable(true);
                maskCancel.setVisibility(View.GONE);
                maskOk.setVisibility(View.GONE);
            }
        });

        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                f = 0;
                curtain.setVisibility(View.GONE);
                image.setVisibility(View.GONE);
                choose();
            }
        });

        draw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curtain.setVisibility(View.VISIBLE);
                image.setVisibility(View.VISIBLE);
                setProcessMenuClose();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                f = 2;
                choose();
            }
        });

        processMenuOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setProcessMenuOpen();
            }
        });

        processMenuClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setProcessMenuClose();
            }
        });
    }

    private void setProcessMenuOpen(){
        TranslateAnimation mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        mShowAction.setDuration(500);
        processMenu.startAnimation(mShowAction);
        processMenu.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                processMenuOpen.setVisibility(View.GONE);
            }
        }, 170);
    }


    private void setProcessMenuClose(){
        TranslateAnimation mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        mShowAction.setDuration(500);
        processMenu.startAnimation(mShowAction);
        processMenu.setVisibility(View.GONE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                processMenuOpen.setVisibility(View.VISIBLE);
            }
        }, 330);
    }

    private Bitmap getMask(Bitmap mask,int newWidth, int newHeight){
        Bitmap cutMask;
        if(!cut) {
            int cutHeight = bimage.getWidth() * newHeight / newWidth;
            Log.d("test_cutmask_width",newWidth+"");
            Log.d("test_cutmask_newHeight",newHeight+"");
            Log.d("test_cutmask_cutHeight",cutHeight+"");
            cutMask = Bitmap.createBitmap(mask, 0,
                    (bimage.getHeight() - cutHeight) / 2,
                    bimage.getWidth(), cutHeight, null, false);
        }else{
            int cutWidth = bimage.getHeight() * newWidth / newHeight;
            cutMask = Bitmap.createBitmap(mask, (bimage.getWidth() - cutWidth) / 2, 0,
                    cutWidth,newHeight , null, false);
        }
//        if(!mask.isRecycled()){
//            mask.recycle();
//            mask = null;
//        }
        return  zoomImg(cutMask, pWidth, pHeight);
    }

    // 选择图片
    public void choose() {
            // 进入图库
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            //初始化数据
//            if (bitmap != null&& !bitmap.isRecycled()) {
//                bitmap.recycle();
//                bitmap = null;
//            }
//            if (copyBitmap != null && f == 2&& !copyBitmap.isRecycled()) {
//                copyBitmap.recycle();
//                copyBitmap = null;
//            }
            startActivityForResult(intent, RESULT);
            setProcessMenuClose();
    }

    // 保存图片
    public void save() {
        if (copyBitmap != null) {
            Bitmap nBitmap = Bitmap.createBitmap(copyBitmap,0,0,
                    copyBitmap.getWidth(),copyBitmap.getHeight(),new Matrix(),false);
            Bitmap zoomedMaskBitmap = zoomImg(nBitmap,(int)(nBitmap.getWidth()/bimage.getScale()),
                    (int)(nBitmap.getHeight()/bimage.getScale()));
            canvas.drawColor(Color.BLACK);
            canvas.drawBitmap(zoomedMaskBitmap,-bimage.getDisplayRect().left/bimage.getScale(),
                    -bimage.getDisplayRect().top/bimage.getScale()+(canvas.getHeight()
                            -pHeight*canvas.getWidth()/pWidth)/2,new Paint());
//            if(nBitmap != null&&!nBitmap.isRecycled()){
//                nBitmap.recycle();
//                nBitmap = null;
//            }
            image.setImageBitmap(copyBitmap);
            curtain.setVisibility(View.INVISIBLE    );
            bimage.setScale(1.0f);
            image.invalidate();
            Bitmap maskBitmap;
            if(pWidth>bimage.getWidth() || pHeight>bimage.getHeight()){
                maskBitmap = getMask(copyBitmap, pWidth, pHeight);
            }else {
                Log.d("test_","1");
                maskBitmap = getMask(copyBitmap, bimage.getWidth(),bimage.getHeight());
            }


            mMask = new File("/mnt/sdcard/01.jpeg");
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mMask));
                maskBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
//                if(maskBitmap != null&&!maskBitmap.isRecycled()){
//                    maskBitmap.recycle();
//                    maskBitmap = null;
//                }
                bos.flush();
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    @Override
    // 从图库中选取图片后返回
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (f == 2) {
                curtain.setVisibility(View.GONE);
            }
            // TODO Auto-generated method stub
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                // 获取选中的图片的Uri
                Uri imageFileUri = data.getData();
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
                windowWidth = metrics.widthPixels;
                windowHeight = metrics.heightPixels;
                String imagePath = getRealPath(this.getApplicationContext(), imageFileUri);
                if (f == 0) {
                    mPrototype = new File(imagePath);
                } else if (f == 2) {
                    mTarget = new File(imagePath);
                }
                try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        // 不为像素申请内存，只获取图片宽高
                        options.inJustDecodeBounds = true;
                        bitmap = BitmapFactory.decodeStream(getContentResolver()
                                .openInputStream(imageFileUri), null, options);
                        pHeight = options.outHeight;
                        pWidth = options.outWidth;
                        cut = ((float) pHeight / (float) pWidth)
                                > ((float) windowHeight / (float) windowWidth);
                        Glide.with(this)
                                .load(imageFileUri)
                                .into(bimage);
                        copyBitmap = Bitmap.createBitmap(BitmapFactory.decodeResource(this.getResources(),
                                R.drawable.mask), 0, 0, image.getWidth(), image.getHeight());
                        // 创建画布
                        canvas = new Canvas(copyBitmap);
                        // 创建画笔
                        paint = new Paint();
                        // 设置画笔颜色
                        paint.setColor(Color.WHITE);
                        // 设置画笔宽度
                        paint.setStrokeWidth(50);
                        // 开始作画，把原图的内容绘制在白纸上
                        canvas.drawBitmap(copyBitmap, new Matrix(), paint);
                        // 将处理后的图片放入imageview中
                        image.setImageBitmap(copyBitmap);
                        // 设置imageview监听
                        image.setOnTouchListener(new DrawMaskTouchListener());

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

    }


    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("respath");
            Glide.with(mAc)
                    .load(val)
                    .into(image);
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            f = 1;
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("respath", getConnect());
            msg.setData(data);
            handler.sendMessage(msg);
        }

        private String getConnect() {
            String uid = generateGUID();
            String result = "";
            OkHttpClient okHttpClient = new OkHttpClient();
            MultipartBody.Builder iSrc = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("src", uid,
                            RequestBody.create(MediaType.parse("image/jpg"), mPrototype));

            MultipartBody.Builder iMask = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("mask",uid,
                            RequestBody.create(MediaType.parse("image/jpg"),mMask));

            MultipartBody.Builder iDst = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("dst",uid,
                            RequestBody.create(MediaType.parse("image/jpg"),mTarget));



            RequestBody requestBody1 = iSrc.build();
            RequestBody requestBody2 = iMask.build();
            RequestBody requestBody3 = iDst.build();
            FormBody mLocate = new FormBody.Builder()
                    .add("xLocation", mXLocate+"")
                    .add("yLocation", mYLocate+"")
                    .add("GUID",uid)
                    .build();




            Request request1 = new Request
                    .Builder()
                    .url("http://192.168.191.1:8080/PictureMix2/Picture/uploadImage")
                    .post(requestBody1)
                    .build();

            Request request2 = new Request
                    .Builder()
                    .url("http://192.168.191.1:8080/PictureMix2/Picture/uploadImage")
                    .post(requestBody2)
                    .build();

            Request request3 = new Request
                    .Builder()
                    .url("http://192.168.191.1:8080/PictureMix2/Picture/uploadImage")
                    .post(requestBody3)
                    .build();

            Request request4 = new Request
                    .Builder()
                    .url("http://192.168.191.1:8080/PictureMix2/Picture/pictureMix")
                    .post(mLocate)
                    .build();

            try {
                Response response1 = okHttpClient.newCall(request1).execute();
                Response response2 = okHttpClient.newCall(request2).execute();
                Response response3 = okHttpClient.newCall(request3).execute();
                Response response4 = okHttpClient.newCall(request4).execute();
                result = response4.body().string();

                response1.body().close();
                response2.body().close();
                response3.body().close();
                response4.body().close();
                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

    };


    private static String getRealPath(Context context, Uri uri) {
        String filePath = null;
        String[] projection = { MediaStore.Images.Media.DATA };

        CursorLoader loader = new CursorLoader(context, uri, projection, null,
                null, null);
        Cursor cursor = loader.loadInBackground();

        if (cursor != null) {
            cursor.moveToFirst();
            filePath = cursor.getString(cursor.getColumnIndex(projection[0]));
            cursor.close();
        }
        return filePath;
    }

    public static String generateGUID() {
        return new BigInteger(100, new Random()).toString(30).toUpperCase();
    }


    public static Bitmap zoomImg(Bitmap bm, int newWidth ,int newHeight){
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    public class DrawMaskTouchListener implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                // 按下
                case MotionEvent.ACTION_DOWN:
                    if(-15 <= downx-event.getX()&&downx-event.getX() <= 15
                            &&-15 <= downy-event.getY()&&downy-event.getX() <= 15||a == 0){
                        downx = event.getX();
                        downy = event.getY();
                    }
                    break;
                // 移动
                case MotionEvent.ACTION_MOVE:
                    if(f == 0) {
                        // 路径画板
                        x = event.getX();
                        y = event.getY();
                        int m1 = (int) downx,
                                n1 = (int) downy,
                                m2 = (int) x,
                                n2 = (int) y;
                        if (m1 > m2 || n1 > n2) {
                            int ex = m1;
                            m1 = m2;
                            m2 = ex;
                            ex = n1;
                            n1 = n2;
                            n2 = ex;
                        }
                        if (m1 == m2 && n1 != n2) {
                            for (int y1 = n1; y1 <= n2; y1++) {
                                int x1 = (m1 - m2) * (y1 - n1) / (n1 - n2) + m1;
                                canvas.drawCircle(x1, y1, 50.0f, paint);
                            }
                        }
                        if (n1 == n2 && m1 != m2) {
                            for (int x1 = m1; x1 <= m2; x1++) {
                                int y1 = (n1 - n2) * x1 / (m1 - m2) + n1 - m1 * (n1 - n2) / (m1 - m2);
                                canvas.drawCircle(x1, y1,  50.0f, paint);
                            }
                        }
                        if (n1 != n2 && m1 != m2) {
                            for (int x1 = m1; x1 <= m2; x1++) {
                                int y1 = (n1 - n2) * x1 / (m1 - m2) + n1 - m1 * (n1 - n2) / (m1 - m2);
                                canvas.drawCircle(x1, y1,  50.0f, paint);
                            }
                            for (int y1 = n1; y1 <= n2; y1++) {
                                int x1 = (m1 - m2) * (y1 - n1) / (n1 - n2) + m1;
                                canvas.drawCircle(x1, y1, 50.0f, paint);
                            }
                        }
                        // 刷新image
                        image.invalidate();
                        downx = x;
                        downy = y;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if(f == 0){
                        processMenuOpen.setClickable(false);
                        maskCancel.setVisibility(View.VISIBLE);
                        maskOk.setVisibility(View.VISIBLE);
                    }
                    if(f == 2){
                        mXLocate = ((int)event.getX()-(image.getWidth()-bimage.getWidth())/2)
                                *pWidth/bimage.getWidth();
                        mYLocate = ((int)event.getY()-(image.getHeight()-bimage.getHeight())/2)
                                *pHeight/bimage.getHeight();
                        new Thread(runnable).start();
                    }
                    break;

                default:
                    break;
            }
            return true;
        }
    }
}