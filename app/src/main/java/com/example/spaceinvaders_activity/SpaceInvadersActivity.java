package com.example.spaceinvaders_activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Point;
import android.view.Display;

    // SpaceInvadersActivity es el comienzo de la aplicación
    // Será quien se encarga de administrar todas las llamadas al resto de las actividades y métodos
public class SpaceInvadersActivity extends Activity {

    SpaceInvadersEngine spaceInvadersEngine;
    private boolean isGameInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializa el juego con una dificultad predeterminada.
        initGame("Facil"); // Inicia con la dificultad fácil como ejemplo.
    }

    // Este método inicia o reinicia el juego con la dificultad seleccionada.
    private void initGame(String difficulty) {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // Si el juego aún no ha sido inicializado, crea una nueva instancia.
        if (!isGameInitialized) {
            spaceInvadersEngine = new SpaceInvadersEngine(this, size.x, size.y, difficulty);
            setContentView(spaceInvadersEngine);
            isGameInitialized = true;
        } else {
            // Si el juego ya está en ejecución, simplemente actualiza la dificultad.
            spaceInvadersEngine.setDifficulty(difficulty);
        }
    }

    public void showDifficultySelection() {
        // Este método se llama desde el motor del juego para mostrar el menú de selección de dificultad.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Mostrar el diálogo de selección de dificultad.
                showDifficultyMenu();
            }
        });
    }

    // Método para mostrar el menú de selección de dificultad.
    private void showDifficultyMenu() {
        // Ejecutar en el hilo de la interfaz de usuario para asegurar que se manejen los elementos de UI correctamente.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(SpaceInvadersActivity.this);
                builder.setTitle("Elige la dificultad");
                builder.setItems(new CharSequence[]{"Facil", "Dificil"}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedDifficulty = (which == 0) ? "Facil" : "Dificil";
                        initGame(selectedDifficulty);
                    }
                });
                AlertDialog difficultyDialog = builder.create();
                difficultyDialog.setCancelable(false); // Evita que el usuario cancele el diálogo.
                difficultyDialog.show();
            }
        });
    }

    public void showGameOverScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(SpaceInvadersActivity.this);
                builder.setTitle("Game Over");
                builder.setMessage("¡El juego ha terminado!");
                builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showDifficultySelection();
                    }
                });

                AlertDialog gameOverDialog = builder.create();
                gameOverDialog.show();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Mostrar el menú de selección de dificultad al reanudar si el juego ya está inicializado.
        if (isGameInitialized) {
            spaceInvadersEngine.resume();
            showDifficultyMenu();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausar el motor del juego si está en ejecución.
        if (isGameInitialized) {
            spaceInvadersEngine.pause();
        }
    }
}

