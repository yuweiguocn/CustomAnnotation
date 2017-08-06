package io.github.yuweiguocn.customannotation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

import io.github.yuweiguocn.MyCustomAnnotation;
import io.github.yuweiguocn.annotation.CustomAnnotation;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StringBuilder sb = new StringBuilder();
        List<String> annotations = MyCustomAnnotation.getAnnotations();
        for (int i = 0; i < annotations.size(); i++) {
            sb.append(annotations.get(i));
            sb.append("\n");
        }

        ((TextView)findViewById(R.id.tv_annotation)).setText(sb.toString());
    }

    @CustomAnnotation
    public void testAnnotation() {
        Log.d("test", "test annotation");
    }
}
