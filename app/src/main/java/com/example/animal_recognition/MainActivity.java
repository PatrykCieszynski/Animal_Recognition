package com.example.animal_recognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.animal_recognition.ml.Model;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ImageView pictureView;
    Button take;
    Button load;
    PieChart pieChart;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        load = findViewById(R.id.take);
        take = findViewById(R.id.load);
        pictureView = findViewById(R.id.pictureView);
        //result = findViewById(R.id.result);
        pieChart = findViewById(R.id.result);

        // Permission for camera
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.CAMERA
            }, 100);
        }
    }

    public void Take_Picture(View v) {
        Intent open_camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(open_camera, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Camera
            if (requestCode == 1) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                Generate_Output(photo);
            }
            // Gallery
            else if (requestCode == 2) {
                Uri selectedImage = data.getData();
                try {
                    Bitmap photo = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    Generate_Output(photo);
                } catch (IOException e) {
                    Log.i("TAG", "Some exception " + e);
                }
            }
        }
    }

    public void Load_Picture(View v) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 2);
    }

    public void setupPieChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelTextSize(12);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setCenterText("Result");
        pieChart.setCenterTextSize(24);
        pieChart.getDescription().setEnabled(false);
        pieChart.setVisibility(View.VISIBLE);

    }

    public void loadPieChartData(List<Category> probability) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        float other = 1f;
        for (Category category: probability) {
            if(category.getScore() > 0.04) {
                other -= category.getScore();
                if(category.getLabel().startsWith("n0")) {
                    String[] splittedLabel = category.getLabel().split("-");
                    entries.add(new PieEntry(category.getScore(), splittedLabel[1]));
                }
                else entries.add(new PieEntry(category.getScore(), category.getLabel()));
            }
            else break;
        }
        entries.add(new PieEntry(other, "other"));
        ArrayList<Integer> colors = new ArrayList<>();
        for (int color: ColorTemplate.MATERIAL_COLORS) {
            colors.add(color);
        }
        for (int color: ColorTemplate.VORDIPLOM_COLORS) {
            colors.add(color);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setDrawValues(true);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(20f);
        data.setValueTextColor(Color.BLACK);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        pieChart.setData(data);
        pieChart.setCenterTextSize(18f);
        pieChart.setEntryLabelTextSize(16f);
        pieChart.setExtraTopOffset(12f);
        pieChart.setExtraRightOffset(18f);
        pieChart.setExtraLeftOffset(18f);
        pieChart.getLegend().setEnabled(false);
        pieChart.invalidate();

        pieChart.animateY(1400, Easing.EaseInOutQuad);
    }

    public void Generate_Output(Bitmap photo) {
        try {
            pictureView.setImageBitmap(photo);

            Model animalsModel = Model.newInstance(this);
            Bitmap bitmap = photo.copy(Bitmap.Config.ARGB_8888, true);
            TensorImage TFImage = TensorImage.fromBitmap(bitmap);

            Model.Outputs outputs = animalsModel.process(TFImage);
            List<Category> probability = outputs.getProbabilityAsCategoryList();
            animalsModel.close();

            probability.sort((obj1, obj2) -> {
                // ## Ascending order
                //return Float.compare(obj1.getScore(), obj2.getScore());

                // ## Descending order
                return Float.compare(obj2.getScore(), obj1.getScore());
            });
            setupPieChart();
            loadPieChartData(probability);
        } catch (IOException e) {
            //catch error
        }
    }

}