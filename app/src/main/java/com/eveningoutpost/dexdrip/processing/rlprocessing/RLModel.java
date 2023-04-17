package com.eveningoutpost.dexdrip.processing.rlprocessing;

import android.util.Log;

import org.apache.commons.lang3.NotImplementedException;
import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
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
    public float inferBasal(RLInput input) throws InferErrorException {
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

        float[] input_data = new float[]{input.getLatestBG()};
        float[][] output_data = new float[1][1];

        try { interpreter.run(input_data, output_data); }
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
        ArrayList<DataPoint> dataPoints;

        static class DataPoint {
            public float bgreading;
            public float insulin;            public float carbs;
            public long timestamp;
        }

        public RLInput(List<DataPoint> dataPoints) {
            this.dataPoints = new ArrayList<>(dataPoints);
        }

        public float getLatestBG() {
            // input: [bg_in_float]
            return dataPoints.get(0).bgreading;
        }
    }
}
