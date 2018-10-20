package com.kirayim;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;
import org.apache.commons.exec.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Hello world!
 * gstreamer1.0-opencv
 * libopencv3.2-java
 * 
 * 
 * Found at: /usr/share/OpenCV/java/opencv-320.jar
 *
 * Video from: http://techslides.com/sample-webm-ogg-and-mp4-video-files-for-html5
 */
public class App 
{
    File testFile;

    static {
//        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    //=============================================

    public class OpenCVPanel extends JPanel {
        public BufferedImage image = null;
        VideoCapture cap;
        Mat mat;
        int type = 0;
        boolean reopen;
        long frameWait;
        String url;
        Thread workerThread = null;


        public OpenCVPanel(String url, boolean reopen, long frameWait) {
            this.url = url;
            this.reopen = reopen;
            this.frameWait = frameWait;

            workerThread = new Thread(this::capture, "Capture thread");
            workerThread.setDaemon(true);
            workerThread.start();

        }



        @Override
        public void paint(Graphics g) {
            if (image == null) {
                super.paint(g);
            } else {
                g.drawImage(image, 0, 0, getWidth(), getHeight(), 0, 0, image.getWidth(), image.getHeight(), this);
            }
        }

        public void open() {
            if (cap != null) {
                cap.release();
            }

            if (url == null) {
                cap  = new VideoCapture(0);
            } else {
                cap = new VideoCapture(url, Videoio.CV_CAP_FFMPEG);
            }
        }

        // ==================================================
        public void capture() {
            mat = new Mat();
            open();

            try {
                while (true) {
                    if (cap.read(mat) == false) {
                        System.out.println("Read failed.");
                        Thread.sleep(50);

                        if (reopen) {
                            open();
                        } else {
                            break;
                        }

                        continue;
                    }

                    int w = mat.cols();
                    int h = mat.rows();

                    if (w == 0 || h == 0) {
                        System.out.println("Width or height zero");

                        if (cap.isOpened() == false) {
                            if (reopen) {
                                open();
                            } else {
                                break;
                            }
                        }
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

                    repaint();

                    if (frameWait > 0) {
                        Thread.sleep(50);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ===============================================


    public App(String url) {
        setup();
    }



    // ==================================================
    public void setup() {

        startFFMPEG();

        JFrame frame = new JFrame("Video");

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 640, 480);

        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new MigLayout("", "[grow][grow]", "[grow][grow]"));

        contentPane.add(new OpenCVPanel(null, false, 10), "grow");
        contentPane.add(new OpenCVPanel("file://" + testFile.getAbsolutePath(), true, 50), "grow,wrap");
        contentPane.add(new OpenCVPanel("udp://:1330", true, 50), "grow");

        frame.setVisible(true);
    }

    public void startFFMPEG() {
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("small.mp4");
            testFile = File.createTempFile("testFile", ".mp4");
            testFile.deleteOnExit();

            Files.copy(in, testFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();

            CommandLine commandLine = CommandLine.parse("ffmpeg -re -y -stream_loop -1 -i file://" + testFile.getAbsolutePath() +  " -f mpegts udp://0.0.0.0:1330");

            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

            ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);
            Executor executor = new DefaultExecutor();
            executor.setExitValue(1);
            executor.setWatchdog(watchdog);
            executor.execute(commandLine, resultHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ==================================================
    public static void main( String[] args ) {

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
