package com.eveningoutpost.dexdrip.processing.rlprocessing;

import android.net.Uri;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.xdrip;

import org.apache.commons.lang3.NotImplementedException;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class Calculations {
    private static Calculations instance;

    /** Exception for any exception occurred during the loading of the model */
    public static class ModelLoadException extends Exception {
        public ModelLoadException(String message) {
            super(message);
        }
    }

    private static final String TAG = "Calculations"; // Tag string to identify the class in the logs.
    // The assign path to copy of the model file in the app's private storage.
    private final String tflite_inputstream_path = xdrip.getAppContext().getFilesDir().getAbsolutePath() + "/model.tflite";

    RLModel model;

    public Calculations() throws ModelLoadException {
        loadModel();
    }

    public static Calculations getInstance() throws ModelLoadException {
        if (instance == null) {
            instance = new Calculations();
        }
        return instance;
    }

    /**
     * Using historical BG data, calculate the ratios.
     * @return Wrapper class containing insulin/carb ratios.
     */
    public RLModel.Ratios calculateRatios() throws ModelLoadException, RLModel.InferErrorException {
        // TODO
        throw new NotImplementedException("Ratios are not implemented.");
    }

    /**
     * Using historical BG data, calculate the basal.
     * @return Needed basal.
     */
    public Double calculateBasal() throws ModelLoadException, RLModel.InferErrorException {
        // TODO
        throw new NotImplementedException("Basal not implemented.");
    }

    /**
     * Using historical BG data, calculate the insulin needed.
     * @return Needed insulin.
     */
    public float calculateInsulin() throws ModelLoadException, RLModel.InferErrorException {
        RLModel.RLInput input = getRLInput();
        return model.inferInsulin(input);
    }

    /** Gets all the data needed for the RL model from other classes. */
    private static RLModel.RLInput getRLInput() {
        // For now this data is only used for inference, and not retraining/fine tuning to a user. This only requires the latest data.
        List<BgReading> bgReadings = BgReading.latest(1); // TODO: get the number of bg readings using timestamps.

        // Get the latest BG timestamp to get its treatments.
        BgReading bgReading = bgReadings.get(0);
        List<Treatments> treatments = Treatments.listByTimestamp(bgReading.timestamp); // TODO: get the number of treatments using timestamps.

        return new RLModel.RLInput(treatments, bgReadings);
    }

    // ------------------ Model Importing ------------------

    /** Loads the model from the file. */
    private void loadModel() throws ModelLoadException {
        FileInputStream inputStream;
        try { inputStream = new FileInputStream(tflite_inputstream_path); } // Gets model's imported file from the app's private storage. Exception if not found.
        catch (FileNotFoundException e) {
            Log.e(TAG, "Error loading the model:" + e.getMessage());
            throw new ModelLoadException("RL model file not found.");
        }

        try {
            // Conversion from input stream to MappedByteBuffer.
            FileChannel fileChannel = inputStream.getChannel();
            MappedByteBuffer mappedBuffer =  fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

            Interpreter inter = new Interpreter(mappedBuffer);
            model = new RLModel(inter);
            Log.i(TAG, "Model loaded successfully.");
        }
        catch (Exception e) {
            Log.e(TAG, "Error loading the model:" + e.getMessage());
            throw new ModelLoadException("RL interpreter load exception.");
        }
    }

    /**
     * Given a file uri, copies the file to the app's private storage and loads the model.
     * @param uri Uri of the file to import. Uri is obtained from androids file picker.
     * @throws IOException
     * @throws ModelLoadException
     */
    public void importModel(Uri uri) throws IOException, ModelLoadException {
        InputStream stream = xdrip.getAppContext().getContentResolver().openInputStream(uri);

        OutputStream output = new BufferedOutputStream(new FileOutputStream(tflite_inputstream_path));
        copyFile(stream, output); // Copies the file from the uri to the app's private storage.

        loadModel();
    }

    /**
     * Copies a file from an InputStream to an OutputStream.
     * Used to copy the model file from the inputstream to the app's private storage.
     * @param in InputStream to read from.
     * @param out OutputStream to write to.
     * @throws IOException
     */
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }
}
