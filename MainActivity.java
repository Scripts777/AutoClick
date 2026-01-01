package com.example.pixelwatcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.Display;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.DisplayManager;

public class MainActivity extends Activity {

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private AutoClickService service;

    private final int[][] clickSequence = {
            {2159,580},{1238,787},{2159,580},{1238,787},
            {2159,580},{1238,787},{2159,580},{1238,787},
            {2272,112},{985,455}
    };
    private final int TARGET_COLOR = 0x000042AB; // заменить на свой цвет

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        service = (AutoClickService) getSystemService(AutoClickService.class);

        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 100 && resultCode == RESULT_OK){
            MediaProjectionManager projectionManager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);

            imageReader = ImageReader.newInstance(1,1, PixelFormat.RGBA_8888,2);
            Display display = getWindowManager().getDefaultDisplay();
            mediaProjection.createVirtualDisplay(
                    "PixelWatcher",
                    1, 1, getResources().getDisplayMetrics().densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null, null
            );

            startPixelLoop();
        }
    }

    private void startPixelLoop(){
        new Thread(() -> {
            while(true){
                Image image = imageReader.acquireLatestImage();
                if(image != null){
                    Image.Plane plane = image.getPlanes()[0];
                    java.nio.ByteBuffer buffer = plane.getBuffer();
                    int pixelColor = buffer.getInt();
                    image.close();

                    if(pixelColor == TARGET_COLOR){
                        for(int[] point : clickSequence){
                            service.tap(point[0], point[1]);
                        }
                    }
                }
                try { Thread.sleep(5); } catch(Exception e){}
            }
        }).start();
    }
}
