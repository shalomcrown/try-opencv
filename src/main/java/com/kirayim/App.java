package com.kirayim;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Hello world!
 *
 */
public class App 
{
    public BufferedImage image = null;
    VideoCapture cap;
    JPanel panel;

    static {
        nu.pattern.OpenCV.loadShared();        
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public App(String url) {
        setup();
    }

    // ==================================================
    public void setup() {
        
        JFrame frame = new JFrame("Video");

        panel = new JPanel()  {
            public void paint(Graphics g) {
                if (image == null) {
                    super.paint(g);
                } else {
                    g.drawImage(image, 0, 0, this);
                }

            }            
        };

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 640, 480);
        frame.setContentPane(panel);
        frame.setVisible(true);

        cap = new VideoCapture("file:///home/shalom/workspace/trials/try-opencv/small.mp4", Videoio.CV_CAP_FFMPEG);

        Thread workerThread = new Thread(this::capture, "CApture thread");
        workerThread.setDaemon(true);
        workerThread.start();
    }
    // ==================================================
    public void capture() {
        Mat mat = new Mat();
        int type = 0;

        try {
            while (true) {
                if (cap.read(mat) == false) {
                    System.out.println("Read failed.");
                    Thread.sleep(50);
                    continue;
                }

                int w = mat.cols();
                int h = mat.rows();

                if (w == 0 || h == 0) {
                    System.out.println("Width or height zero");
                    Thread.sleep(50);
                    continue;
                }

                if (mat.channels() == 1) {
                    type = BufferedImage.TYPE_BYTE_GRAY;
                } else if (mat.channels() == 3) {
                    type = BufferedImage.TYPE_3BYTE_BGR;
                }

                if (image == null || image.getWidth() != w || image.getHeight() != h || image.getType() != type) {
                    image = new BufferedImage(w, h, type);
                }
                
                WritableRaster raster = image.getRaster();
                DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
                byte[] data = dataBuffer.getData();
                mat.get(0, 0, data);

                panel.repaint();

            }            
        } catch (Exception e) {
            //TODO: handle exception
        }

    }

    // ==================================================
    public static void main( String[] args )
    {
        App app = new App("");

        try {
            synchronized (app) {
                app.wait();
            }
        } catch (Exception e) {
            // Nothing
        }

    }
}
