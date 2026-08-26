# Cómo probar este plugin

Este plugin revisa una lista de "atajos" (llamados "scripts") que
suele tener un proyecto de JavaScript, y avisa cuáles de esos atajos
nadie usa — para poder borrarlos.

## Qué hacer

1. En el panel de la izquierda, abrí el archivo **`package.json`**
   (dentro de la carpeta `demo`).
2. Buscá la parte que dice `"scripts"` — ahí adentro hay una lista de
   6 atajos con nombre (`build`, `prebuild`, `test`, `pretest`,
   `start`, `deploy-legacy`).
3. Mirá justo al lado izquierdo del número de línea de cada uno de
   esos 6 — ahí va a aparecer un pequeño ícono.

## Qué deberías ver

- Los primeros 5 atajos (`build`, `prebuild`, `test`, `pretest`,
  `start`) deberían tener un ícono que indica "esto se usa" (por
  ejemplo un tilde/check verde, o algo similar).
- El último, **`deploy-legacy`**, debería tener un ícono distinto que
  indica "nadie lo usa" (una advertencia) — porque es el único atajo
  que no aparece referenciado en ningún otro lugar del proyecto.

Podés pasar el mouse por encima de cada ícono para ver el motivo
exacto que da el plugin.

## Si algo no se ve así

Sacá la captura igual, y avisame qué atajo no coincide con lo de
arriba.
