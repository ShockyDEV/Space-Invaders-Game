package com.example.spaceinvaders_activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;

    // SpaceInvadersActivity es el comienzo de la aplicación.
    // Administra el ciclo de vida del motor del juego y los diálogos de pre-partida y Game Over.
public class SpaceInvadersActivity extends Activity {

    SpaceInvadersEngine spaceInvadersEngine;
    private boolean isGameInitialized = false;
    private GameData gameData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameData = new GameData(this);
        initGame();

        // Pantalla de selección de habilidades antes de empezar a jugar.
        showSkillSelection();
    }

    // Crea el motor del juego (una sola vez) y lo establece como vista de contenido.
    private void initGame() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        if (!isGameInitialized) {
            spaceInvadersEngine = new SpaceInvadersEngine(this, size.x, size.y, "Facil");
            setContentView(spaceInvadersEngine);
            isGameInitialized = true;
        }
    }

    // Diálogo de selección de habilidades. El jugador elige hasta MAX_ACTIVE_SKILLS
    // de entre las desbloqueadas para su nivel actual.
    public void showSkillSelection() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int playerLevel = gameData.getPlayerLevel();
                final List<Skill> available = Skill.getAvailableSkills(playerLevel);

                final CharSequence[] names = new CharSequence[available.size()];
                for (int i = 0; i < available.size(); i++) {
                    Skill s = available.get(i);
                    names[i] = s.name + " — " + s.description;
                }

                final boolean[] checked = new boolean[available.size()];
                final List<Integer> selected = new ArrayList<>();

                AlertDialog.Builder builder = new AlertDialog.Builder(SpaceInvadersActivity.this);
                builder.setTitle("Habilidades (máx " + Skill.MAX_ACTIVE_SKILLS + ") · Nivel " + playerLevel);
                builder.setMultiChoiceItems(names, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        int id = available.get(which).id;
                        if (isChecked) {
                            if (selected.size() >= Skill.MAX_ACTIVE_SKILLS) {
                                // Revertimos la selección por encima del límite permitido.
                                ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                                checked[which] = false;
                            } else {
                                selected.add(id);
                            }
                        } else {
                            selected.remove((Integer) id);
                        }
                    }
                });
                builder.setPositiveButton("Jugar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        spaceInvadersEngine.startGame(1, new ArrayList<>(selected), false);
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        });
    }

    // Pantalla de Game Over con estadísticas persistidas de la partida.
    public void showGameOverScreen(final int score, final int levelReached, final int playerLevel,
                                   final int xpEarned, final int highScore, final boolean newHighScore) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(SpaceInvadersActivity.this);
                builder.setTitle(newHighScore ? "¡Nuevo récord!" : "Game Over");
                String message = "Puntuación: " + score + "\n"
                        + "Nivel alcanzado: " + levelReached + "\n"
                        + "Nivel de jugador: " + playerLevel + "\n"
                        + "XP ganada: " + xpEarned + "\n"
                        + "Récord: " + highScore;
                builder.setMessage(message);
                builder.setPositiveButton("Jugar de nuevo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showSkillSelection();
                    }
                });

                AlertDialog gameOverDialog = builder.create();
                gameOverDialog.setCancelable(false);
                gameOverDialog.show();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (isGameInitialized) {
            spaceInvadersEngine.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isGameInitialized) {
            spaceInvadersEngine.pause();
        }
    }
}
