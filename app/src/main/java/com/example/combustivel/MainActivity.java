package com.example.combustivel;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private EditText dateInput, odometerInput, litersInput, priceInput, ethanolPriceInput, gasolinePriceInput;
    private RadioGroup fuelGroup;
    private TextView averageResult, costResult, dailyResult, historyResult, recommendationResult;
    private SharedPreferences preferences;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("controle_combustivel", MODE_PRIVATE);
        dateInput = findViewById(R.id.input_date);
        odometerInput = findViewById(R.id.input_odometer);
        litersInput = findViewById(R.id.input_liters);
        priceInput = findViewById(R.id.input_price);
        ethanolPriceInput = findViewById(R.id.input_ethanol_price);
        gasolinePriceInput = findViewById(R.id.input_gasoline_price);
        fuelGroup = findViewById(R.id.fuel_group);
        averageResult = findViewById(R.id.result_average);
        costResult = findViewById(R.id.result_cost);
        dailyResult = findViewById(R.id.result_daily);
        historyResult = findViewById(R.id.history_result);
        recommendationResult = findViewById(R.id.recommendation_result);
        dateInput.setText(preferences.getString("date", dateFormat.format(new Date())));
        historyResult.setText(preferences.getString("history", "Nenhum abastecimento registrado ainda."));
        dateInput.setInputType(InputType.TYPE_CLASS_DATETIME);
        dateInput.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.save_button).setOnClickListener(v -> saveFueling());
        findViewById(R.id.compare_button).setOnClickListener(v -> updateFuelRecommendation());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        try { Date selected = dateFormat.parse(dateInput.getText().toString()); if (selected != null) calendar.setTime(selected); }
        catch (ParseException ignored) { }
        new DatePickerDialog(this, (view, year, month, day) -> { Calendar chosen = Calendar.getInstance(); chosen.set(year, month, day); dateInput.setText(dateFormat.format(chosen.getTime())); }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveFueling() {
        dateInput.setError(null); odometerInput.setError(null); litersInput.setError(null); priceInput.setError(null);
        String dateText = dateInput.getText().toString().trim();
        Double odometer = number(odometerInput), liters = number(litersInput), price = number(priceInput);
        if (dateText.isEmpty()) { dateInput.setError("Informe a data"); return; }
        if (odometer == null || odometer < 0) { odometerInput.setError("Informe um hodômetro válido"); return; }
        if (liters == null || liters <= 0) { litersInput.setError("Informe os litros abastecidos"); return; }
        if (price == null || price <= 0) { priceInput.setError("Informe o valor por litro"); return; }
        if (fuelGroup.getCheckedRadioButtonId() == -1) { Toast.makeText(this, "Selecione o tipo de combustível", Toast.LENGTH_SHORT).show(); return; }
        try { dateFormat.setLenient(false); dateFormat.parse(dateText); } catch (Exception e) { dateInput.setError("Use o formato dd/MM/yyyy"); return; }
        boolean hasPrevious = preferences.contains("odometer");
        double previousOdometer = preferences.getFloat("odometer", 0);
        if (hasPrevious && odometer <= previousOdometer) { odometerInput.setError("Deve ser maior que o hodômetro anterior"); return; }
        String previousDate = preferences.getString("date", dateText);
        String fuel = fuelGroup.getCheckedRadioButtonId() == R.id.radio_ethanol ? "Etanol" : "Gasolina";
        String history = dateText + " • " + format(odometer) + " km • " + format(liters) + " L • " + fuel + "\n" + preferences.getString("history", "");
        preferences.edit().putFloat("odometer", odometer.floatValue()).putString("date", dateText).putString("history", history).apply();
        if (hasPrevious) {
            double distance = odometer - previousOdometer;
            averageResult.setText(format(distance / liters) + " km/L");
            costResult.setText("R$ " + format((liters * price) / distance) + " por km");
            dailyResult.setText(format(distance / Math.max(1, daysBetween(previousDate, dateText))) + " km/dia");
        } else { averageResult.setText("Registre o próximo abastecimento"); costResult.setText("Registre o próximo abastecimento"); dailyResult.setText("Registre o próximo abastecimento"); }
        historyResult.setText(history);
        Toast.makeText(this, "Abastecimento registrado!", Toast.LENGTH_SHORT).show();
        odometerInput.setText(""); litersInput.setText(""); priceInput.setText("");
    }

    private void updateFuelRecommendation() {
        Double ethanol = number(ethanolPriceInput), gasoline = number(gasolinePriceInput);
        if (ethanol == null || gasoline == null || ethanol <= 0 || gasoline <= 0) { recommendationResult.setText("Informe os dois preços para comparar."); return; }
        double percentage = (ethanol / gasoline) * 100;
        if (percentage <= 70) recommendationResult.setText("Etanol é mais vantajoso (" + format(percentage) + "% do preço da gasolina).");
        else recommendationResult.setText("Gasolina é mais vantajosa (etanol a " + format(percentage) + "% da gasolina).");
    }

    private Double number(EditText input) { try { return Double.parseDouble(input.getText().toString().trim().replace(',', '.')); } catch (Exception e) { return null; } }
    private long daysBetween(String first, String last) { try { return Math.abs((dateFormat.parse(last).getTime() - dateFormat.parse(first).getTime()) / 86400000L); } catch (Exception e) { return 1; } }
    private String format(double value) { return String.format(Locale.getDefault(), "%.2f", value); }
}
