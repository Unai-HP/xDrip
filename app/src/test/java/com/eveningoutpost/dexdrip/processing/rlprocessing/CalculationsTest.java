package com.eveningoutpost.dexdrip.processing.rlprocessing;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.xdrip;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.tensorflow.lite.Interpreter;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalculationsTest extends RobolectricTestWithConfig {
    private static final MockedStatic<xdrip> xdripMockedStatic = mockStatic(xdrip.class);
    private static final Context context = mock(Context.class);
    private static final File mockedFile = mock(File.class);
    private static File modelFile;
    private Calculations calculations;

    @Before
    public void setUps(){
        // --- Mock tflite_inputstream_path ---
        modelFile = new File("src/test/java/com/eveningoutpost/dexdrip/processing/rlprocessing", "testModel.tflite");
        assertTrue(modelFile.exists());
        assertTrue(modelFile.length()>0);

        xdripMockedStatic.when(xdrip::getAppContext).thenReturn(context);
        when(context.getFilesDir()).thenReturn(mockedFile);
        when(mockedFile.getAbsolutePath()).thenReturn(modelFile.getParent());

        // --- Mock TensorFlow Lite interpreter ---
        calculations = spy(Calculations.getInstance());
        Interpreter interpreter = mock(Interpreter.class);
        // If interpreter is loaded, there is a unhandledLink error, the TensorFlow Lite library is not loaded
        // To avoid that, we mock the interpreter and the call that creates it,
        // which avoids executing the native code that loads the library
        doReturn(interpreter).when(calculations).createInterpreter(any());
    }

    @Test
    public void test_1_Import(){
        ContentResolver cs = mock(ContentResolver.class);

        Uri uri = Uri.parse(modelFile.getAbsolutePath());
        calculations = Calculations.getInstance();

        try {
            InputStream inputStream = new FileInputStream(modelFile);          // Returned by the ContentResolver

            when(context.getContentResolver()).thenReturn(cs);                 // Mock Context return inside Calculations.importModel()
            when(cs.openInputStream(any())).thenReturn(inputStream);           // Mock ContentResolver return inside Calculations.importModel()

            calculations.importModel(uri);                                     // Test importModel()

            File file = new File(modelFile.getParent(), "model.tflite");  // Resulting file from importModel()
            assertTrue(file.exists());
            assertTrue(file.length()>0);

            file.deleteOnExit();
        }
        catch (FileNotFoundException e)           { fail("File not found. Exception: " + e.getMessage()); }
        catch (IOException e)                     { fail("IOException. Exception: " + e.getMessage()); }

    }

    //TODO use file for testing. For now is not possible to load TFLite library
    @Test
    public void test_2_LoadModel() {
        File testModelFile = new File(modelFile.getParent(), "testModel.tflite");
        File useModelFile = new File(modelFile.getParent(), "model.tflite");
        useModelFile.delete();

        // Test if model file does not exist
        assertThrows(Calculations.ModelLoadException.class, calculations::loadModel);

        // Test if model file exists
        try {
            Files.copy(testModelFile.toPath(), useModelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            assertTrue(useModelFile.exists());
        } catch (IOException e) {
            fail("Error copying file. Exception: " + e);
        }

        try {
            calculations.loadModel();
            assertNotNull(calculations.model);
        }
        catch (Calculations.ModelLoadException e) { fail("ModelLoadException. Exception: " + e.getMessage()); }
    }

    @Test
    public void test_3_getRLInput() {
        MockedStatic<BgReading> bgReadingMockedStatic = mockStatic(BgReading.class);
        bgReadingMockedStatic.when(() -> BgReading.latest(anyInt())).thenReturn(new ArrayList<>());

        // No data
        RLModel.RLInput rlInput = calculations.getRLInput(1);
        assertEquals(0, rlInput.dataPoints.size());
        assertEquals(new ArrayList<>(), rlInput.dataPoints);

        // One data point
        List<BgReading> bgReadings = new ArrayList<>(1);
        BgReading bgReading = new BgReading();
        bgReading.timestamp = 1000;
        bgReading.calculated_value = 100.0;

        bgReadings.add(bgReading);

        bgReadingMockedStatic.when(() -> BgReading.latest(anyInt())).thenReturn(bgReadings);

        rlInput = calculations.getRLInput(1);
        assertEquals(1, rlInput.dataPoints.size());
        assertEquals((float) 100, rlInput.dataPoints.get(0).bgreading);
    }

    @Test
    public void test_4_calculateInsulin() {
        RLModel mockedModel = mock(RLModel.class);
        calculations.model = mockedModel;

        RLModel.RLInput.DataPoint dataPoint = new RLModel.RLInput.DataPoint();
        dataPoint.bgreading = 100;
        dataPoint.carbs = 10;
        dataPoint.insulin = 0;
        dataPoint.timestamp = 1000;
        ArrayList<RLModel.RLInput.DataPoint> dataPoints = new ArrayList<>(1);
        dataPoints.add(dataPoint);

        RLModel.RLInput rlInput = new RLModel.RLInput(dataPoints);

        try {
            Mockito.doReturn(rlInput).when(calculations).getRLInput(anyInt());
            when(mockedModel.inferInsulin(any())).thenReturn((float) 10);

            assertEquals((Double) 10.0, calculations.calculateInsulin(), 0.01);

        } catch (RLModel.InferErrorException e) {
            fail("InferErrorException. Exception: " + e.getMessage());
        } catch (Calculations.ModelLoadException e) {
            fail("ModelLoadException. Exception: " + e.getMessage());
        }
    }

    @Test
    public void test_4_calculateRatios(){
        assertThrows(NotImplementedException.class, calculations::calculateRatios);
    }

    @Test
    public void test_4_calculateBasal(){
        assertThrows(NotImplementedException.class, calculations::calculateBasal);
    }

}
