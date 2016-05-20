package com.example.acer.myapplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends Activity {

    RecordAudio recordTask;
    ClientTask clientTask;
    //ServerTask serverTask;
    Button recordButton;
    TextView statusText;
    File recordFile;
    File outputFile;
    boolean isRecording = false;

    int SAMPLE_RATE = 16000;
    int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_IN_MONO;
    int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView) this.findViewById(R.id.StatusTextView);
        recordButton = (Button) this.findViewById(R.id.RecordButton);
        videoView = (VideoView) this.findViewById(R.id.videoView);

        recordFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "recording.pcm");
        boolean recordFileExists = recordFile.delete();
        if (recordFileExists) {
            recordFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "recording.pcm");
        }
        outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "AudioTest.wav");
        boolean outputFileExists = outputFile.delete();
        if (outputFileExists) {
            outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "AudioTest.wav");
        }

        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        startRecording();
                        return true;
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        stopRecording();
                }
                return false;
            }
        });
    }

    public void startRecording() {

        recordTask = new RecordAudio();
        recordTask.execute();

    }

    public void stopRecording() {
        isRecording = false;
        pcmToWav(recordFile, outputFile);
        System.out.println("Conversion Completed");

        clientTask = new ClientTask("192.168.43.117", 8080);
        clientTask.execute();
    }

    public void playVideo(VideoView videoView, String URL){
        videoView.setVideoPath(URL);
        //videoView.requestFocus();
        videoView.start();
    }

    private class RecordAudio extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            isRecording = true;
            try {
                DataOutputStream outputStream = new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(recordFile)));
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIGURATION,
                        AUDIO_ENCODING);
                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE, CHANNEL_CONFIGURATION, AUDIO_ENCODING, bufferSize);

                short[] buffer = new short[bufferSize];
                audioRecord.startRecording();

                int r = 0;
                while (isRecording) {
                    int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                    for (int i = 0; i < bufferReadResult; i++) {
                        outputStream.writeShort(buffer[i]);
                    }
                    publishProgress(new Integer(r));
                    r++;
                }
                audioRecord.stop();
                audioRecord.release();  // released for next object instantiate
                outputStream.close();
            } catch (Throwable throwable) {
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            statusText.setText(progress[0].toString());
        }
    }

    /*
    ** convert pcm to .wav
     */
    private void pcmToWav(final File inputFile, final File waveFile) {

        byte[] pcmData = new byte[(int) inputFile.length()];
        System.out.println(pcmData.length);
        DataInputStream inputStream = null;
        DataOutputStream dataOutputStream = null;
        try {
            inputStream = new DataInputStream(new FileInputStream(inputFile));
            inputStream.read(pcmData);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            String chunckID = "RIFF";
            String format = "WAVE";
            String subChunk1ID = "fmt ";
            int subChunk1Size = 16;
            short audioFormat = 1; // format_code : 0x0001
            short numChannels = 1;
            int sampleRate = 16000;
            int bitRate = 16;
            int byteRate = sampleRate * bitRate / 8 * numChannels;
            int blockAlign = bitRate / 8 * numChannels;
            String subChunk2ID = "data";
            int dataSize = pcmData.length;
            long subChunk2Size = bitRate / 8 * numChannels * dataSize;
            long chunkSize = 36 + dataSize;

            dataOutputStream = new DataOutputStream(new FileOutputStream(waveFile));
            dataOutputStream.writeBytes(chunckID);
            dataOutputStream.write(intInLittleEndian((int) chunkSize), 0, 4);
            dataOutputStream.writeBytes(format);
            dataOutputStream.writeBytes(subChunk1ID);
            dataOutputStream.write(intInLittleEndian(subChunk1Size), 0, 4);
            dataOutputStream.write(shortInLittleEndian(audioFormat), 0, 2);
            dataOutputStream.write(shortInLittleEndian(numChannels), 0, 2);
            dataOutputStream.write(intInLittleEndian(sampleRate), 0, 4);
            dataOutputStream.write(intInLittleEndian(byteRate), 0, 4);
            dataOutputStream.write(shortInLittleEndian((short) blockAlign), 0, 2);
            dataOutputStream.write(shortInLittleEndian((short) bitRate), 0, 2);
            dataOutputStream.writeBytes(subChunk2ID);
            dataOutputStream.write(intInLittleEndian((int) subChunk2Size), 0, 4);
            // pcm data converted from big endian to little endian
            short[] shorts = new short[pcmData.length / 2];
            ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer byteBuffer = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                byteBuffer.putShort(s);
            }
            dataOutputStream.write(byteBuffer.array());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static byte[] intInLittleEndian(int data) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) (data >> 0);
        byteArray[1] = (byte) (data >> 8);
        byteArray[2] = (byte) (data >> 16);
        byteArray[3] = (byte) (data >> 24);
        return byteArray;
    }

    private static byte[] shortInLittleEndian(short data) {
        byte[] byteArray = new byte[2];
        byteArray[0] = (byte) (data >> 0);
        byteArray[1] = (byte) (data >> 8);
        return byteArray;
    }

    /*
    ** upload wave file to server.
     */
    private class ClientTask extends AsyncTask<Void, Void, Void> {
        String serverIP;
        int serverPort;
        private String filePath;
        private String fileName;
        String videoPath;

        public ClientTask(String address, int port) {
            this.serverIP = address;
            this.serverPort = port;
        }

        @Override
        protected Void doInBackground(Void... params) {

            Socket socket = null;
            String response;
            FileInputStream fileInputStream;
            BufferedInputStream bufferedInputStream;
            DataOutputStream dataOutputStream;
            DataInputStream dataInputStream;
            try {
                socket = new Socket(serverIP, serverPort);
                // upload file to server
                filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                fileName = "AudioTest.wav";
                File file = new File(filePath +"/" + fileName);
                byte[] array = new byte[(int) file.length()];
                fileInputStream = new FileInputStream(file);
                bufferedInputStream = new BufferedInputStream(fileInputStream);
                bufferedInputStream.read(array, 0, array.length);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.write(array, 0, array.length);
                dataOutputStream.flush();
                socket.shutdownOutput(); // shutdownOutput() to close just the output stream. prevent from closing socket.
                // receive response from the server.
                dataInputStream = new DataInputStream(socket.getInputStream());
                response = dataInputStream.readUTF();

                videoPath = getVideoPath(response);
                System.out.println(response);
                dataInputStream.close();

            }catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            playVideo(videoView,videoPath);
        }

        private String getVideoPath(String videoName){
            String name = videoName + ".mp4";
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + name;
            return path;
        }
    }

}
