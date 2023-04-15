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

public class RLModel {
    // TODO use TF signatures to make the model more flexible.


    /** Exception for any exception occurred during the inference (calculations) of the model */
    public static class InferErrorException extends Exception {
        public InferErrorException(String message) {
            super(message);
        }
    }

    private final String      TAG = "RLApi"; // Tag string to identify the class in the logs.
    private final Interpreter interpreter;   // Model interpreter. Used to feed data to the model and get the result.

    /**
     * Singleton pattern.
     * @return The only instance of the class.
     */
    public RLModel(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /**
     * Using historical BG and Treatment data, calculate the ratios.
     * @return ModelApi.Ratios - Wrapper class containing insulin/carb ratios.
     */
    public Ratios inferRatios(RLInput input) throws InferErrorException {
        if (interpreter == null) {
            Log.e(TAG, "Model not loaded.");
        }
        throw new NotImplementedException("Ratios are not implemented.");
    }

    /**
     * Using historical BG data, calculate the basal insulin.
     * @return Calculated needed basal insulin.
     */
    public Double inferBasal(RLInput input) throws InferErrorException {
        if (interpreter == null) {
            Log.e(TAG, "Model not loaded.");
        }
        throw new NotImplementedException("Basal not implemented.");
    }


    /**
     * Using historical BG and Treatment data, calculate the bolus insulin.
     * @return Needed insulin.
     */
    public float inferInsulin(RLInput input) throws InferErrorException {
        if (interpreter == null) {
            Log.e(TAG, "Model not loaded.");
            throw new InferErrorException("RL model not loaded.");
        }

        float[][] output_data = new float[1][1];

        try { interpreter.run(input.getInputData(), output_data); }
        catch (Exception e) {
            Log.e(TAG, "Error running the model:" + e.getMessage());
            throw new InferErrorException("RL model running failed.");
        }
        return output_data[0][0];
    }

    /** Wrapper to save the result of any ratio calculated by inferRatios. */
    public static class Ratios {
        private Double carbRatio;
        private Double insulinSensitivity;
        public Double getCarbRatio() {
            return carbRatio;
        }

        public void setCarbRatio(Double carbRatio) {
            this.carbRatio = carbRatio;
        }

        public Double getInsulinSensitivity() {
            return insulinSensitivity;
        }

        public void setInsulinSensitivity(Double insulinSensitivity) {
            this.insulinSensitivity = insulinSensitivity;
        }
    }

    /** This class formats the data obtained from the historical data from other classes and convert them to a easily used wrapper. */
    public static class RLInput {
        float[] bg_data; // All bg values in the last X hours.
        float[] carbs_data; // All carbs values in the last X hours.
        float[] insulin_data; // All insulin values in the last X hours.

        public RLInput(List<Treatments> treatments, List<BgReading> bgreadings) {
            // For now the model only uses the lastest BG value to do the inference.
            bg_data = new float[1];
            bg_data[0] = (float) bgreadings.get(0).calculated_value;
        }

        public float[] getInputData() {
            // input: [bg_in_float]
            return bg_data;
        }
    }
}
