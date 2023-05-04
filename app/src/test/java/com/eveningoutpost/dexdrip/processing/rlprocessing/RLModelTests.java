package com.eveningoutpost.dexdrip.processing.rlprocessing;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
import java.util.List;

public class RLModelTests {
    // Tests for inner class RLInput
    @Test
    public void testCreateRLInput() {
        RLModel.RLInput.DataPoint dataPoint = new RLModel.RLInput.DataPoint();
        assertNotNull(dataPoint);
        dataPoint.bgreading = 100;
        dataPoint.timestamp = 1000;
        dataPoint.carbs = 10;
        dataPoint.insulin = 1;

        List<RLModel.RLInput.DataPoint> dataPoints = new ArrayList<>();
        dataPoints.add(dataPoint);

        assertEquals(dataPoints.get(0), dataPoint);

        RLModel.RLInput input = new RLModel.RLInput(dataPoints);
        assertNotNull(input);
        assertEquals(input.dataPoints.get(0), dataPoint);
    }

    @Test
    public void testGetLatestBG() {
        RLModel.RLInput.DataPoint dataPoint = new RLModel.RLInput.DataPoint();
        dataPoint.bgreading = 100;
        dataPoint.timestamp = 1000;
        dataPoint.carbs = 10;
        dataPoint.insulin = 1;

        List<RLModel.RLInput.DataPoint> dataPoints = new ArrayList<>();
        dataPoints.add(dataPoint);

        RLModel.RLInput input = new RLModel.RLInput(dataPoints);
        assertEquals(input.getLatestBG(), 100, 0.01);

        // Test for empty dataPoints
        List<RLModel.RLInput.DataPoint> emptyDataPoints = new ArrayList<>();
        RLModel.RLInput emptyInput = new RLModel.RLInput(emptyDataPoints);
        assertThrows(IndexOutOfBoundsException.class, emptyInput::getLatestBG);
    }

    @Test
    public void testCreateRatios() {
        RLModel.Ratios ratios = new RLModel.Ratios();
        assertNotNull(ratios);

        RLModel.Ratios ratios1 = new RLModel.Ratios();
        assertNull(ratios1.getCarbRatio());

        RLModel.Ratios ratios2 = new RLModel.Ratios();
        ratios2.setCarbRatio(1.0);
        assertEquals(ratios2.getCarbRatio(), 1.0, 0.01);

        RLModel.Ratios ratios3 = new RLModel.Ratios();
        assertNull(ratios3.getInsulinSensitivity());

        RLModel.Ratios ratios4 = new RLModel.Ratios();
        ratios4.setInsulinSensitivity(1.0);
        assertEquals(ratios4.getInsulinSensitivity(), 1.0, 0.01);
    }

    @Test
    public void testConstructor() {
        // Test null interpreter
        new RLModel(null);

        // Test non-null interpreter
        Interpreter interpreter = mock(Interpreter.class);
        new RLModel(interpreter);
    }

    @Test
    public void testInferInsulin() {
        // Test null interpreter
        RLModel model = new RLModel(null);
        assertNotNull(model);

        RLModel modelForLamda = model;
        assertThrows(RLModel.InferErrorException.class, () -> modelForLamda.inferInsulin(null));

        // Test non-null interpreter
        Interpreter mockedInterpreter = mock(Interpreter.class);
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            args[1] = new float[][]{{0}}; // This the modified output of the mocked interpreter
            return null; // to be void
        }).when(mockedInterpreter).run(any(), any());

        model = new RLModel(mockedInterpreter);
        assertNotNull(model);

        // Empty RLInput
        List<RLModel.RLInput.DataPoint> dataPoints = new ArrayList<>();
        RLModel.RLInput input = new RLModel.RLInput(dataPoints);
        RLModel.RLInput finalInput = input;
        RLModel finalModel = model;
        assertThrows(RLModel.InferErrorException.class, () -> finalModel.inferInsulin(finalInput));

        // Non-empty RLInput
        RLModel.RLInput.DataPoint dataPoint = new RLModel.RLInput.DataPoint();
        dataPoint.bgreading = 100;
        dataPoint.timestamp = 1000;
        dataPoint.carbs = 10;
        dataPoint.insulin = 1;

        dataPoints.add(dataPoint);
        input = new RLModel.RLInput(dataPoints);

        try {
            assertEquals(model.inferInsulin(input), 0, 0.01);
        } catch (RLModel.InferErrorException e) {
            fail("IndexOutOfBoundsException thrown");
        }
    }

    @Test
    public void testRatios(){
        // Test null interpreter
        RLModel model = new RLModel(null);
        assertNotNull(model);

        RLModel modelForLamda = model;
        assertThrows(NotImplementedException.class, () -> modelForLamda.inferRatios(null));

        // Test non-null interpreter
        Interpreter mockedInterpreter = mock(Interpreter.class);
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            args[1] = new float[][]{{0}}; // This the modified output of the mocked interpreter
            return null; // to return null
        }).when(mockedInterpreter).run(any(), any());

        model = new RLModel(mockedInterpreter);
        assertNotNull(model);

        // Empty RLInput
        List<RLModel.RLInput.DataPoint> dataPoints = new ArrayList<>();
        RLModel.RLInput input = new RLModel.RLInput(dataPoints);
        RLModel.RLInput finalInput = input;
        RLModel finalModel = model;
        assertThrows(NotImplementedException.class, () -> finalModel.inferRatios(finalInput));

        // Non-empty RLInput
        RLModel.RLInput.DataPoint dataPoint = new RLModel.RLInput.DataPoint();
        dataPoint.bgreading = 100;
        dataPoint.timestamp = 1000;
        dataPoint.carbs = 10;
        dataPoint.insulin = 1;

        dataPoints.add(dataPoint);
        input = new RLModel.RLInput(dataPoints);

        try {
            assertThrows(NotImplementedException.class, () -> modelForLamda.inferRatios(null));
        } catch (Exception e) {
            fail("exception thrown");
        }
    }

    @Test
    public void testInferBasal(){
        // Test null interpreter
        RLModel model = new RLModel(null);
        assertNotNull(model);

        RLModel modelForLamda = model;
        assertThrows(NotImplementedException.class, () -> modelForLamda.inferBasal(null));

        // Test non-null interpreter
        Interpreter mockedInterpreter = mock(Interpreter.class);
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            args[1] = new float[][]{{0}}; // This the modified output of the mocked interpreter
            return null; // to return null
        }).when(mockedInterpreter).run(any(), any());

        model = new RLModel(mockedInterpreter);
        assertNotNull(model);

        // Empty RLInput
        List<RLModel.RLInput.DataPoint> dataPoints = new ArrayList<>();
        RLModel.RLInput input = new RLModel.RLInput(dataPoints);
        RLModel.RLInput finalInput = input;
        RLModel finalModel = model;
        assertThrows(NotImplementedException.class, () -> finalModel.inferBasal(null));

        // Non-empty RLInput
        RLModel.RLInput.DataPoint dataPoint = new RLModel.RLInput.DataPoint();
        dataPoint.bgreading = 100;
        dataPoint.timestamp = 1000;
        dataPoint.carbs = 10;
        dataPoint.insulin = 1;

        dataPoints.add(dataPoint);
        input = new RLModel.RLInput(dataPoints);

        try {
            assertThrows(NotImplementedException.class, () -> modelForLamda.inferBasal(null));
        } catch (Exception e) {
            fail("exception thrown");
        }
    }

}
