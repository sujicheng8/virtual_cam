package com.example.vcam;


import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookMain implements IXposedHookLoadPackage {
    public static Surface msurf;
    public static SurfaceTexture msurftext;
    public static MediaPlayer mMedia;
    public static SurfaceTexture virtual_st;
    public static Camera reallycamera;

    public static Camera data_camera;
    public static volatile byte[] data_buffer;
    public static byte[] input;
    public static int mhight;
    public static int mwidth;
    public static boolean is_someone_playing;
    public static boolean is_hooked;
    //public static Thread file_thred;
    //public static Thread prepare_thred;
    public static VideoToFrames hw_decode_obj;
    public static VideoToFrames c2_hw_decode_obj;
    public static VideoToFrames c2_hw_decode_obj_1;
    public static SurfaceTexture c1_fake_texture;
    public static Surface c1_fake_surface;
    public static SurfaceHolder ori_holder;
    public static MediaPlayer mplayer1;
    public static Camera mcamera1;
    public static SurfaceTexture c2_virtual_texture;
    public static boolean is_c2_text_playing;
    public static boolean is_c2_surf1_playing;
    public static boolean is_c2_surf2_playing;

    public static int onemhight;
    public static int onemwidth;
    public static Class camera_callback_calss;

    public static Surface c2_preview_Surfcae;
    public static Surface c2_reader_Surfcae;
    public static Surface c2_reader_Surfcae_1;
    public static MediaPlayer c2_player;
    public static CaptureRequest.Builder c2_builder;
    public static ImageReader c2_image_reader;
    public static Surface c2_virtual_surface;

    public static Class c2_state_callback;
    public Context toast_content;

    public static int repeat_count;


    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Exception {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
        if (file.exists()) {
            Class cameraclass = XposedHelpers.findClass("android.hardware.Camera", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(cameraclass, "setPreviewTexture", SurfaceTexture.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (is_hooked ){
                        is_hooked = false;
                        return;
                    }
                    if (param.args[0] == null) {
                        return;
                    }
                    if (reallycamera != null && reallycamera.equals(param.thisObject)) {
                        param.args[0] = HookMain.virtual_st;
                        XposedBridge.log("发现重复" + reallycamera.toString());
                        return;
                    } else {
                        XposedBridge.log("创建预览");
                    }

                    reallycamera = (Camera) param.thisObject;
                    HookMain.msurftext = (SurfaceTexture) param.args[0];


                    if (HookMain.virtual_st == null) {
                        HookMain.virtual_st = new SurfaceTexture(10);
                    } else {
                        HookMain.virtual_st.release();
                        HookMain.virtual_st = new SurfaceTexture(10);
                    }
                    param.args[0] = HookMain.virtual_st;

                    if (HookMain.msurf == null) {
                        HookMain.msurf = new Surface(HookMain.msurftext);
                    } else {
                        HookMain.msurf.release();
                        HookMain.msurf = new Surface(HookMain.msurftext);
                    }

                    if (HookMain.mMedia == null) {
                        HookMain.mMedia = new MediaPlayer();
                    } else {
                        HookMain.mMedia.release();
                        HookMain.mMedia = new MediaPlayer();
                    }

                    HookMain.mMedia.setSurface(HookMain.msurf);

                    File sfile = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/no-silent.jpg");
                    if (!(sfile.exists() && (!is_someone_playing))){
                        HookMain.mMedia.setVolume(0, 0);
                        is_someone_playing =false;
                    }else {
                        is_someone_playing = true;
                    }
                    HookMain.mMedia.setLooping(true);

                    HookMain.mMedia.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            HookMain.mMedia.start();
                        }
                    });

                    try {
                        HookMain.mMedia.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
                        HookMain.mMedia.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
        } else {
            if (toast_content != null) {
                /*Toast.makeText(toast_content, "不存在替换视频", Toast.LENGTH_SHORT).show();*/
            }
        }

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera", String.class, CameraDevice.StateCallback.class, Handler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                c2_state_callback = param.args[1].getClass();
                File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/disable.jpg");
                if (control_file.exists()) {
                    if (toast_content != null) {
                        /*Toast.makeText(toast_content, "disable.jpg存在，已禁用C2", Toast.LENGTH_LONG).show();*/
                    }
                    return;
                }
                XposedBridge.log("1位参数初始化相机，类：" + c2_state_callback.toString());
                process_camera2_init(c2_state_callback);
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera", String.class, Executor.class, CameraDevice.StateCallback.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/disable.jpg");
                    if (control_file.exists()) {
                        if (toast_content != null) {
                            /*Toast.makeText(toast_content, "disable.jpg存在，已禁用C2", Toast.LENGTH_LONG).show();*/
                        }
                        return;
                    }
                    c2_state_callback = param.args[2].getClass();
                    XposedBridge.log("2位参数初始化相机，类：" + c2_state_callback.toString());
                    process_camera2_init(c2_state_callback);
                }
            });
        }


        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "addTarget", Surface.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] == null){
                    return;
                }
                File file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
                File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/disable.jpg");
                if ((!file.exists()) && control_file.exists() ){
                    if (toast_content != null) {
                        /*Toast.makeText(toast_content, "disable.jpg存在，已禁用C2", Toast.LENGTH_LONG).show();*/
                    }
                    return;
                }
                if ((!param.thisObject.equals(HookMain.c2_builder))){
                    HookMain.c2_builder = (CaptureRequest.Builder) param.thisObject;
                    c2_reader_Surfcae = null;
                    c2_preview_Surfcae = null;
                    c2_reader_Surfcae_1 = null;
                    is_c2_text_playing =false;
                    is_c2_surf1_playing =false;
                    is_c2_surf2_playing = false;
                    String surfaceInfo = param.args[0].toString();
                    if (surfaceInfo.contains("Surface(name=null)")){
                        c2_reader_Surfcae = (Surface) param.args[0];
                    }else {
                        c2_preview_Surfcae = (Surface) param.args[0];
                    }
                    if (HookMain.c2_image_reader == null) {
                        HookMain.c2_image_reader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 5);
                    }
                    /*if (c2_virtual_texture != null){
                        c2_virtual_texture.release();
                        c2_virtual_texture = null;
                    }
                    c2_virtual_texture = new SurfaceTexture(155);
                    if (c2_virtual_surface != null){
                        c2_virtual_surface.release();
                        c2_virtual_surface = null;
                    }
                    c2_virtual_surface = new Surface(c2_virtual_texture);*/
                    c2_virtual_surface=c2_image_reader.getSurface();
                    XposedBridge.log("添加目标："+param.args[0].toString());
                    XposedBridge.log(c2_virtual_surface.toString());
                    param.args[0] = c2_image_reader.getSurface();
                    process_camera2_play();
                }else{
                    String surfaceInfo = param.args[0].toString();
                    if (surfaceInfo.contains("Surface(name=null)")){
                        if (c2_reader_Surfcae == null) {
                            c2_reader_Surfcae = (Surface) param.args[0];
                        }else {
                            if ((!c2_reader_Surfcae.equals(param.args[0])) && c2_reader_Surfcae_1 == null){
                                c2_reader_Surfcae_1 = (Surface) param.args[0];
                            }
                        }
                    }else {
                        if (c2_preview_Surfcae == null) {
                            c2_preview_Surfcae = (Surface) param.args[0];
                        }
                    }
                    XposedBridge.log("多个添加目标："+param.args[0].toString());
                    param.args[0] = c2_image_reader.getSurface();
                    XposedBridge.log(c2_virtual_surface.toString());
                    process_camera2_play();
                    param.setResult(null);
                }

/*
                if (file.exists() && (!control_file.exists())) {
                    if (HookMain.c2_builder != null && HookMain.c2_builder.equals(param.thisObject)) {
                        param.args[0] = HookMain.c2_image_reader.getSurface();
                        XposedBridge.log("发现重复" + HookMain.c2_builder.toString() +"都大的" +param.args[0].toString());
                        return;
                    } else {
                        XposedBridge.log("创建C2预览" +  param.args[0].toString());
                    }
                    HookMain.c2_builder = (CaptureRequest.Builder) param.thisObject;
                    HookMain.c2_ori_Surf = (Surface) param.args[0];

                    if (HookMain.c2_image_reader == null) {
                        HookMain.c2_image_reader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 5);
                    }

                    param.args[0] = HookMain.c2_image_reader.getSurface();

                    if (HookMain.c2_player == null) {
                        HookMain.c2_player = new MediaPlayer();
                    } else {
                        HookMain.c2_player.release();
                        HookMain.c2_player = new MediaPlayer();
                    }

                        if (c2_hw_decode_cb != null){
                            c2_hw_decode_cb =null;
                        }
                        c2_hw_decode_cb = new VideoToFrames.Callback() {
                            @Override
                            public void onFinishDecode() {
                                try {
                                    c2_hw_decode_obj.stopDecode();
                                    c2_hw_decode_obj = null;
                                    c2_hw_decode_obj = new VideoToFrames();
                                    c2_hw_decode_obj.set_surfcae(HookMain.c2_ori_Surf);
                                    c2_hw_decode_obj.setCallback(c2_hw_decode_cb);
                                    c2_hw_decode_obj.setSaveFrames(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera2/",OutputImageFormat.NV21);
                                    c2_hw_decode_obj.decode(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
                                }catch (Exception eee){
                                    XposedBridge.log(eee.toString());
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                }
                            }

                            @Override
                            public void onDecodeFrame(int index) {

                            }
                        };
                        if (c2_hw_decode_obj != null){
                            c2_hw_decode_obj.stopDecode();
                            c2_hw_decode_obj =null;
                        }

                        c2_hw_decode_obj = new VideoToFrames();
                        c2_hw_decode_obj.setCallback(c2_hw_decode_cb);
                        try {
                            c2_hw_decode_obj.setSaveFrames(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera2/", OutputImageFormat.NV21);
                            c2_hw_decode_obj.set_surfcae(HookMain.c2_ori_Surf);
                            c2_hw_decode_obj.decode(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
                        }catch (Throwable throwable){
                            throwable.printStackTrace();
                        }

                    HookMain.c2_player.setSurface(HookMain.c2_ori_Surf);
                    File sfile = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/no-silent.jpg");
                    if (!sfile.exists()){
                        HookMain.c2_player.setVolume(0, 0);
                    }
                    HookMain.c2_player.setLooping(true);


                    HookMain.c2_player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        public void onPrepared(MediaPlayer mp) {
                            HookMain.c2_player.start();
                        }
                    });
                    try {
                        HookMain.c2_player.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
                        HookMain.c2_player.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (toast_content != null) {
                        Toast.makeText(toast_content, "不存在替换视频或已禁用C2", Toast.LENGTH_SHORT).show();
                    }
                }*/
            }
        });


        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewCallbackWithBuffer", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "addCallbackBuffer", byte[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    param.args[0] = new byte[((byte[]) param.args[0]).length];
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewCallback", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setOneShotPreviewCallback", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "takePicture", Camera.ShutterCallback.class, Camera.PictureCallback.class, Camera.PictureCallback.class, Camera.PictureCallback.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                XposedBridge.log("4参数拍照");
                if (param.args[1] == null) {
                    process_a_shot_jpeg(param, 3);
                } else {
                    process_a_shot_YUV(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "takePicture", Camera.ShutterCallback.class, Camera.PictureCallback.class, Camera.PictureCallback.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                XposedBridge.log("3参数拍照");
                if (param.args[1] == null) {
                    process_a_shot_jpeg(param, 2);
                } else {
                    process_a_shot_YUV(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.media.MediaRecorder", lpparam.classLoader, "setCamera", Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XposedBridge.log("在录像，已打断");
                if (toast_content != null) {
                    Toast.makeText(toast_content, "已打断录像", Toast.LENGTH_LONG).show();
                }
                param.args[0] = null;
            }
        });

        XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (param.args[0] instanceof Application) {
                    toast_content = ((Application) param.args[0]).getApplicationContext();
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewDisplay", SurfaceHolder.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.thisObject.equals(mcamera1)) {
                    XposedBridge.log("创建Surfaceview预览");
                    mcamera1 = (Camera) param.thisObject;
                    ori_holder = (SurfaceHolder) param.args[0];
                    if (c1_fake_texture == null) {
                        c1_fake_texture = new SurfaceTexture(11);
                    } else {
                        c1_fake_texture.release();
                        c1_fake_texture = null;
                        c1_fake_texture = new SurfaceTexture(11);
                    }

                    if (c1_fake_surface == null) {
                        c1_fake_surface = new Surface(c1_fake_texture);
                    } else {
                        c1_fake_surface.release();
                        c1_fake_surface = null;
                        c1_fake_surface = new Surface(c1_fake_texture);
                    }

                    if (mplayer1 == null) {
                        mplayer1 = new MediaPlayer();
                    } else {
                        mplayer1.release();
                        mplayer1 = null;
                        mplayer1 = new MediaPlayer();
                    }

                    HookMain.mplayer1.setSurface(HookMain.ori_holder.getSurface());
                    is_hooked =true;
                    mcamera1.setPreviewTexture(c1_fake_texture);
                    File sfile = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/no-silent.jpg");
                    if (!(sfile.exists() && (!is_someone_playing))){
                        HookMain.mplayer1.setVolume(0, 0);
                        is_someone_playing =false;
                    }else {
                        is_someone_playing = true;
                    }
                    HookMain.mplayer1.setLooping(true);

                    HookMain.mplayer1.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            HookMain.mplayer1.start();
                        }
                    });

                    try {
                        HookMain.mplayer1.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
                        HookMain.mplayer1.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    XposedBridge.log("发现重复Surfaceview");
                    is_hooked = true;
                    mcamera1.setPreviewTexture(c1_fake_texture);
                }
                param.setResult(null);
            }
        });
    }

    public void process_camera2_play(){
        if (c2_preview_Surfcae != null && (!is_c2_text_playing)){
            if (HookMain.c2_player == null) {
                HookMain.c2_player = new MediaPlayer();
            } else {
                HookMain.c2_player.release();
                HookMain.c2_player = new MediaPlayer();
            }
            is_c2_text_playing = true;
            HookMain.c2_player.setSurface(HookMain.c2_preview_Surfcae);
            File sfile = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/no-silent.jpg");
            if (!sfile.exists()){
                HookMain.c2_player.setVolume(0, 0);
            }
            HookMain.c2_player.setLooping(true);


            HookMain.c2_player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    HookMain.c2_player.start();
                }
            });
            try {
                HookMain.c2_player.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
                HookMain.c2_player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (c2_reader_Surfcae !=null && (!is_c2_surf1_playing)){
            is_c2_surf1_playing = true;
            if (c2_hw_decode_obj != null){
                c2_hw_decode_obj.stopDecode();
                c2_hw_decode_obj =null;
            }

            c2_hw_decode_obj = new VideoToFrames();
            try {
                c2_hw_decode_obj.setSaveFrames(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera2/", OutputImageFormat.NV21);
                c2_hw_decode_obj.set_surfcae(HookMain.c2_reader_Surfcae);
                c2_hw_decode_obj.decode(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
            }catch (Throwable throwable){
                throwable.printStackTrace();
            }
        }

        if (c2_reader_Surfcae_1 !=null && (!is_c2_surf2_playing)){
            is_c2_surf2_playing = true;
            if (c2_hw_decode_obj_1 != null){
                c2_hw_decode_obj_1.stopDecode();
                c2_hw_decode_obj_1 =null;
            }

            c2_hw_decode_obj_1 = new VideoToFrames();
            try {
                c2_hw_decode_obj_1.setSaveFrames(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera2/", OutputImageFormat.NV21);
                c2_hw_decode_obj_1.set_surfcae(HookMain.c2_reader_Surfcae_1);
                c2_hw_decode_obj_1.decode(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
            }catch (Throwable throwable){
                throwable.printStackTrace();
            }
        }
    }

    public void process_camera2_init(Class hooked_class) {
        File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/disable.jpg");
        if (control_file.exists()) {
            if (toast_content != null) {
                /*Toast.makeText(toast_content, "disable.jpg存在，已禁用C2", Toast.LENGTH_LONG).show();*/
            }
            return;
        }
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
        if (!file.exists()) {
            if (toast_content != null) {
                /*Toast.makeText(toast_content, "不存在替换视频", Toast.LENGTH_SHORT).show();*/
            }
            return;
        }
        XposedHelpers.findAndHookMethod(hooked_class, "onOpened", CameraDevice.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createCaptureSession", List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                        if (HookMain.c2_image_reader == null) {
                            HookMain.c2_image_reader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 5);
                        }
                        XposedBridge.log("来HOOK了"+HookMain.c2_image_reader.getSurface().toString());
                        paramd.args[0] = Arrays.asList(HookMain.c2_image_reader.getSurface());
                                            }
                });
            }

        });
    }

    public void process_a_shot_jpeg(XC_MethodHook.MethodHookParam param, int index) {
        try {
            XposedBridge.log("第二个jpeg:" + param.args[index].toString());
        } catch (Exception eee) {
            XposedBridge.log(eee.toString());

        }
        Class callback = param.args[index].getClass();

        XposedHelpers.findAndHookMethod(callback, "onPictureTaken", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                try {
                    Camera loaclcam = (Camera) paramd.args[1];
                    onemwidth = loaclcam.getParameters().getPreviewSize().width;
                    onemhight = loaclcam.getParameters().getPreviewSize().height;
                    XposedBridge.log("JPEG拍照回调初始化：宽：" + onemwidth + "高：" + onemhight + "对应的类：" + loaclcam.toString());
                    if (toast_content != null) {
                        Toast.makeText(toast_content, "发现拍照\n宽：" + onemwidth + "\n高：" + onemhight + "\n格式：JPEG", Toast.LENGTH_LONG).show();
                    }
                    Bitmap pict = getBMP(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/1000.bmp");
                    ByteArrayOutputStream temp_array = new ByteArrayOutputStream();
                    pict.compress(Bitmap.CompressFormat.JPEG, 100, temp_array);
                    byte[] jpeg_data = temp_array.toByteArray();
                    paramd.args[0] = jpeg_data;
                } catch (Exception ee) {
                    XposedBridge.log(ee.toString());
                }
            }
        });
    }

    public void process_a_shot_YUV(XC_MethodHook.MethodHookParam param) {
        try {
            XposedBridge.log("发现拍照YUV:" + param.args[1].toString());
        } catch (Exception eee) {
            XposedBridge.log(eee.toString());

        }
        Class callback = param.args[1].getClass();

        XposedHelpers.findAndHookMethod(callback, "onPictureTaken", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                try {
                    Camera loaclcam = (Camera) paramd.args[1];
                    onemwidth = loaclcam.getParameters().getPreviewSize().width;
                    onemhight = loaclcam.getParameters().getPreviewSize().height;
                    XposedBridge.log("YUV拍照回调初始化：宽：" + onemwidth + "高：" + onemhight + "对应的类：" + loaclcam.toString());
                    if (toast_content != null) {
                        Toast.makeText(toast_content, "发现拍照\n宽：" + onemwidth + "\n高：" + onemhight + "\n格式：YUV_420_888", Toast.LENGTH_LONG).show();
                    }
                    input = getYUVByBitmap(getBMP(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/1000.bmp"));
                    paramd.args[0] = input;
                } catch (Exception ee) {
                    XposedBridge.log(ee.toString());
                }
            }
        });
    }

    public void process_callback(XC_MethodHook.MethodHookParam param) {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
        if (!file.exists()) {
            return;
        }
        Class nmb = param.args[0].getClass();
        XposedHelpers.findAndHookMethod(nmb, "onPreviewFrame", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                Camera localcam = (android.hardware.Camera) paramd.args[1];
                if (localcam.equals(data_camera)) {
                    while (data_buffer == null) {
                    }
                    System.arraycopy(HookMain.data_buffer, 0, paramd.args[0], 0, Math.min(HookMain.data_buffer.length, ((byte[]) paramd.args[0]).length));
                    //HookMain.data_buffer = null;
                } else {
                    camera_callback_calss = nmb;
                    repeat_count = 1000;
                    HookMain.data_camera = (android.hardware.Camera) paramd.args[1];
                    mwidth = data_camera.getParameters().getPreviewSize().width;
                    mhight = data_camera.getParameters().getPreviewSize().height;
                    int frame_Rate = data_camera.getParameters().getPreviewFrameRate();
                    XposedBridge.log("帧预览回调初始化：宽：" + mwidth + " 高：" + mhight + " 帧率：" + frame_Rate);
                    if (toast_content != null) {
                        Toast.makeText(toast_content, "宽：" + mwidth + "\n高：" + mhight + "\n" + "帧率：" + frame_Rate, Toast.LENGTH_LONG).show();
                    }
                    //input = getYUVByBitmap(getBMP(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/bmp/" + repeat_count + ".bmp"));
                    //System.arraycopy(input, 0, (byte[]) paramd.args[0], 0, Math.min(input.length, ((byte[]) paramd.args[0]).length));
                    if (hw_decode_obj != null) {
                        hw_decode_obj.stopDecode();
                    }

                    hw_decode_obj = new VideoToFrames();
                    hw_decode_obj.setSaveFrames("", OutputImageFormat.NV21);
                    hw_decode_obj.decode(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/virtual.mp4");
                    while (data_buffer == null) {
                    }
                    System.arraycopy(HookMain.data_buffer, 0, paramd.args[0], 0, Math.min(HookMain.data_buffer.length, ((byte[]) paramd.args[0]).length));
                }

            }
        });
    }

    //以下代码来源：https://blog.csdn.net/jacke121/article/details/73888732
    private Bitmap getBMP(String file) throws Throwable {
        return BitmapFactory.decodeFile(file);
    }

    private static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        // yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        byte[] yuv = new byte[len * 3 / 2];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = (pixels[i * width + j]) & 0x00FFFFFF;
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;
                // 套用公式
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                y = y < 16 ? 16 : (Math.min(y, 255));
                u = u < 0 ? 0 : (Math.min(u, 255));
                v = v < 0 ? 0 : (Math.min(v, 255));
                // 赋值
                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1)] = (byte) u;
                yuv[len + +(i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }

    private static byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int[] pixels = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return rgb2YCbCr420(pixels, width, height);
    }
}

//以下代码修改自 https://github.com/zhantong/Android-VideoToImages
class VideoToFrames implements Runnable {
    private static final String TAG = "VideoToFrames";
    private static final boolean VERBOSE = false;
    private static final long DEFAULT_TIMEOUT_US = 10000;

    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;


    private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;

    private LinkedBlockingQueue<byte[]> mQueue;
    private OutputImageFormat outputImageFormat;
    private boolean stopDecode = false;

    private String videoFilePath;
    private Throwable throwable;
    private Thread childThread;
    private Surface play_surf;

    private Callback callback;

    public interface Callback {
        void onFinishDecode();

        void onDecodeFrame(int index);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setEnqueue(LinkedBlockingQueue<byte[]> queue) {
        mQueue = queue;
    }

    //设置输出位置，没啥用
    public void setSaveFrames(String dir, OutputImageFormat imageFormat) throws IOException {
        outputImageFormat = imageFormat;

    }

    public void set_surfcae(Surface player_surface) {
        if (player_surface != null) {
            play_surf = player_surface;
        }
    }

    public void stopDecode() {
        stopDecode = true;
    }

    public void decode(String videoFilePath) throws Throwable {
        this.videoFilePath = videoFilePath;
        if (childThread == null) {
            childThread = new Thread(this, "decode");
            childThread.start();
            if (throwable != null) {
                throw throwable;
            }
        }
    }

    public void run() {
        try {
            videoDecode(videoFilePath);
        } catch (Throwable t) {
            throwable = t;
        }
    }

    @SuppressLint("WrongConstant")
    public void videoDecode(String videoFilePath) throws IOException {
        XposedBridge.log("开始解码");
        MediaExtractor extractor = null;
        MediaCodec decoder = null;
        try {
            File videoFile = new File(videoFilePath);
            extractor = new MediaExtractor();
            extractor.setDataSource(videoFile.toString());
            int trackIndex = selectTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + videoFilePath);
            }
            extractor.selectTrack(trackIndex);
            MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            showSupportedColorFormat(decoder.getCodecInfo().getCapabilitiesForType(mime));
            if (isColorFormatSupported(decodeColorFormat, decoder.getCodecInfo().getCapabilitiesForType(mime))) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
                Log.i(TAG, "set decode color format to type " + decodeColorFormat);
            } else {
                Log.i(TAG, "unable to set decode color format, color format type " + decodeColorFormat + " not supported");
            }
            decodeFramesToImage(decoder, extractor, mediaFormat);
            decoder.stop();
            while (!stopDecode) {
                extractor.seekTo(0, 0);
                decodeFramesToImage(decoder, extractor, mediaFormat);
                decoder.stop();
            }
        } finally {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
        }
    }

    private void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        System.out.print("supported color format: ");
        for (int c : caps.colorFormats) {
            System.out.print(c + "\t");
        }
        System.out.println();
    }

    private boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.CodecCapabilities caps) {
        for (int c : caps.colorFormats) {
            if (c == colorFormat) {
                return true;
            }
        }
        return false;
    }

    private void decodeFramesToImage(MediaCodec decoder, MediaExtractor extractor, MediaFormat mediaFormat) {
        boolean is_first = false;
        long startWhen = 0;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        decoder.configure(mediaFormat, play_surf, null, 0);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        decoder.start();
        final int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        final int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        int outputFrameCount = 0;
        while (!sawOutputEOS && !stopDecode) {
            if (!sawInputEOS) {
                int inputBufferId = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }
            int outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US);
            if (outputBufferId >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
                boolean doRender = (info.size != 0);
                if (doRender) {
                    outputFrameCount++;
                    if (callback != null) {
                        callback.onDecodeFrame(outputFrameCount);
                    }
                    if (!is_first) {
                        startWhen = System.currentTimeMillis();
                        is_first = true;
                    }
                    if (play_surf == null) {
                        Image image = decoder.getOutputImage(outputBufferId);
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] arr = new byte[buffer.remaining()];
                        buffer.get(arr);
                        if (mQueue != null) {
                            try {
                                mQueue.put(arr);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (outputImageFormat != null) {
                            HookMain.data_buffer = getDataFromImage(image, COLOR_FormatNV21);
                        }
                        image.close();
                    }
                    long sleepTime = info.presentationTimeUs / 1000 - (System.currentTimeMillis() - startWhen);
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            XposedBridge.log("线程延迟出错");
                        }
                    }
                    decoder.releaseOutputBuffer(outputBufferId, true);
                }
            }
        }
        if (callback != null) {
            callback.onFinishDecode();
        }
    }

    private static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }
        return -1;
    }

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    private static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        if (VERBOSE) Log.v(TAG, "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            if (VERBOSE) {
                Log.v(TAG, "pixelStride " + pixelStride);
                Log.v(TAG, "rowStride " + rowStride);
                Log.v(TAG, "width " + width);
                Log.v(TAG, "height " + height);
                Log.v(TAG, "buffer size " + buffer.remaining());
            }
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }


}

enum OutputImageFormat {
    I420("I420"),
    NV21("NV21"),
    JPEG("JPEG");
    private final String friendlyName;

    OutputImageFormat(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String toString() {
        return friendlyName;
    }
}


