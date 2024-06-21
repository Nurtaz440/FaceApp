package com.nurtaz.dev.facedetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private TextView textView;
    private ImageView imageView;

    private final static int REQUEST_IMAGE_CAPTURE = 123;
    InputImage firebaseVision;
    FaceDetector visionFaceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.btn_camera);
        textView = findViewById(R.id.tv1);
        imageView = findViewById(R.id.iv);

        FirebaseApp.initializeApp(this);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenFile();
            }
        });

        Toast.makeText(this, "App is started", Toast.LENGTH_SHORT).show();
    }

    private void OpenFile() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle bundle = data.getExtras();
        Bitmap bitmap = (Bitmap) bundle.get("data");
        uploadBitmapToFirebase(bitmap);
        FaceDetectionProcces(bitmap);
        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
    }
    public void uploadBitmapToFirebase(Bitmap bitmap) {
        // Convert bitmap to byte array
        byte[] data = bitmapToByteArray(bitmap);

        // Get a reference to Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Generate a unique filename using a timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";

        // Create a reference to the file you want to upload
        StorageReference imageRef = storageRef.child("images/" + imageFileName);

        // Upload the byte array
        UploadTask uploadTask = imageRef.putBytes(data);

        // Register observers to listen for when the upload is done or if it fails
        uploadTask.addOnFailureListener(exception -> {
            // Handle unsuccessful uploads
            Log.e("Firebase", "Upload failed", exception);
        }).addOnSuccessListener(taskSnapshot -> {
            // Task completed successfully
            Log.i("Firebase", "Upload successful");
        });
    }

    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }
    private void FaceDetectionProcces(Bitmap bitmap) {
        textView.setText("Face Detecter in Proccess");
        final StringBuilder builder = new StringBuilder();
        BitmapDrawable drowable = (BitmapDrawable) imageView.getDrawable();

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions
                .Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking().build();

        FaceDetector detector = FaceDetection.getClient(options);

        Task<List<Face>> result = detector.process(image);
        result.addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {
                //titling and rotaiting probablity
//                if (faces.size() != 0) {
//                    if (faces.size() == 1) {
//                        builder.append(faces.size() + " Face Detected \n\n");
//                    } else if (faces.size() > 1) {
//                        builder.append(faces.size() + " Faces Detected \n\n");
//                    }
//                }
                for (Face face : faces) {
                    int id = face.getTrackingId();
                    float rotY = face.getHeadEulerAngleY();
                    float rotZ = face.getHeadEulerAngleZ();

                    //  builder.append("1. Face Tracking ID ["+id+"]\n");
                    //  builder.append("2. Head Rotation to Right ["+String.format("%.2f",rotY)+" deg. ]\n");
                    //  builder.append("1. Head Titled to Sideways ["+String.format("%.2f",rotZ)+" deg.]\n");

                    //smiling problablity
                    Float smilingProbability = face.getSmilingProbability();
                    Log.v("smile", String.valueOf(smilingProbability));
                 //  String smileDegree =  String.format("%.2f", smilingProbability);
                    if (0.01 < smilingProbability && smilingProbability <= 0.1) {
                        builder.append(" You are crying\uD83D\uDE2D \n");
                    } else if (0.1 < smilingProbability && smilingProbability <= 0.2) {
                        builder.append(" You have tired face \uD83D\uDE2B  \n");
                    } else if (0.2 < smilingProbability && smilingProbability <= 0.3) {
                        builder.append(" You are disappointed \uD83D\uDE1E \n");
                    } else if (0.3 < smilingProbability && smilingProbability <= 0.4) {
                        builder.append(" You are upset \uD83D\uDE15 \n");
                    } else if (0.4 < smilingProbability && smilingProbability <= 0.5) {
                        builder.append(" You are worried \uD83D\uDE1F \n");
                    } else if (0.5 < smilingProbability && smilingProbability <= 0.6 ) {
                        builder.append(" You are angry \uD83D\uDE20 \n");
                    }else if (0.6 < smilingProbability && smilingProbability <= 0.7) {
                        builder.append(" You are normal \uD83D\uDE10 \n");
                    }else if (0.7 < smilingProbability && smilingProbability <= 0.8) {
                        builder.append(" You are grinning \uD83D\uDE00 \n");
                    }else if (0.8 < smilingProbability && smilingProbability <= 0.9) {
                        builder.append(" You are smiling \uD83D\uDE04 \n");
                    }else if (0.9 < smilingProbability && smilingProbability < 1.0) {
                        builder.append(" You are sweat smile \uD83D\uDE05 \n");
                    }else if (smilingProbability == 1.0) {
                        builder.append(" You are cheerful \uD83E\uDD23 \n");
                    }


//                    float SmilingProbablity = face.getSmilingProbability();
//                    builder.append(" Smiling Probablity [" + String.format("%.2f", SmilingProbablity) + "]\n");
                    //left eye open problablity
//                    if (face.getLeftEyeOpenProbability() > 0) {
//                        float leftEyeOpenProbability = face.getLeftEyeOpenProbability();
//                        builder.append("4. Left eye Probablity [" + String.format("%.2f", leftEyeOpenProbability) + "]\n");
//                    }
//
//                    //right eye open problablity
//                    if (face.getRightEyeOpenProbability() > 0) {
//                        float righttEyeOpenProbability = face.getRightEyeOpenProbability();
//                        builder.append("4. Right eye Probablity [" + String.format("%.2f", righttEyeOpenProbability) + "]\n");
//                    }
                    builder.append("\n");
                }

                ShowDetection("Face Detection", builder, true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                StringBuilder builder1 = new StringBuilder();
                builder1.append("Sorry !! there is some error " + e);
                ShowDetection("Face detection", builder, false);
            }
        });
    }

    // show result of Firebase ML kit detection in textView
    public void ShowDetection(final String faceDetection, final StringBuilder builder, boolean b) {
        if (b == true) {
            textView.setText(null);
            textView.setMovementMethod(new ScrollingMovementMethod());
            if (builder.length() != 0) {
                textView.append(builder);
//                if (faceDetection.substring(0, faceDetection.indexOf(' '))
//                        .equalsIgnoreCase("OCR")) {
//                    // hello my firend
//
//                    textView.append("\n (Hold the text to copy it!)");
//
//                } else {
//                    textView.append("(Hold the text to copy it!)");
//                }
                textView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText(faceDetection, builder);
                        clipboardManager.setPrimaryClip(clipData);
                        return true;
                    }
                });
            } else {
                textView.append(faceDetection.substring(0, faceDetection.indexOf(' ')) +
                        "Failed to find Anything!");

            }
        } else if (b == false) {
            textView.setText(null);
            textView.setMovementMethod(new ScrollingMovementMethod());
            textView.append(builder);
        }
    }
}