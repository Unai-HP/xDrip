package com.eveningoutpost.dexdrip.processing.rlprocessing;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;

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
        }
        catch (FileNotFoundException e)           { fail("File not found. Exception: " + e.getMessage()); }
        catch (IOException e)                     { fail("IOException. Exception: " + e.getMessage()); }
        catch (Calculations.ModelLoadException e) { fail("ModelLoadException. Exception: " + e.getMessage()); }

    }

    //TODO use file for testing. For now is not possible to load TFLite library
    @Test
    public void test_2_LoadModel() {
        File appModelFile = new File(modelFile.getParent(), "model.tflite");
        assertTrue(appModelFile.exists());

        try {
            calculations.loadModel();
            assertNotNull(calculations.model);
        }
        catch (Calculations.ModelLoadException e) { fail("ModelLoadException. Exception: " + e.getMessage()); }

        appModelFile.deleteOnExit();
    }

    @Test
    public void test_3_calculateInsulin() {
        RLModel mockedRLModel = mock(RLModel.class);

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
            Mockito.doReturn((float)10).when(mockedRLModel).inferInsulin(any());

            assertEquals((float) 10, calculations.calculateInsulin(), 0.01);

        } catch (RLModel.InferErrorException e) {
            fail("InferErrorException. Exception: " + e.getMessage());
        } catch (Calculations.ModelLoadException e) {
            fail("ModelLoadException. Exception: " + e.getMessage());
        }
    }
}
